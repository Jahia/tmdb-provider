/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2024 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.provider.tmdb.item.mapper;

import info.movito.themoviedbapi.model.movies.Cast;
import info.movito.themoviedbapi.model.movies.Credits;
import info.movito.themoviedbapi.model.movies.Crew;
import info.movito.themoviedbapi.model.movies.MovieDb;
import info.movito.themoviedbapi.model.people.credits.MovieCredits;
import info.movito.themoviedbapi.tools.TmdbException;
import net.sf.ehcache.Element;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.external.ExternalQuery;
import org.jahia.modules.external.query.QueryHelper;
import org.jahia.modules.provider.tmdb.helper.Naming;
import org.jahia.modules.provider.tmdb.helper.PathBuilder;
import org.jahia.modules.provider.tmdb.helper.PathHelper;
import org.jahia.modules.provider.tmdb.item.ItemMapper;
import org.jahia.modules.provider.tmdb.item.ItemMapperDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.*;

/**
 * @author Jerome Blanchard
 */
@ItemMapperDescriptor(pathPattern = "^/movies/\\d{4}/\\d{2}/\\d+/(cast_|crew_)\\d+$", idPattern = "^mcredits-\\d+-(cast_|crew_)\\d+$",
        supportedNodeType = {Naming.NodeType.CREW, Naming.NodeType.CAST}, hasLazyProperties = false)
public class MovieCreditsItemMapper extends ItemMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(MovieCreditsItemMapper.class);
    public static final String PATH_LABEL = "credits";
    public static final String ID_PREFIX = "mcredits-";
    public static final String CAST = "cast_";
    public static final String CREW = "crew_";

    public MovieCreditsItemMapper() {
    }

    @Override public List<String> listChildren(String path) {
        return Collections.emptyList();
    }

    @Override public ExternalData getData(String identifier) {
        String cleanId = identifier.substring(ID_PREFIX.length());
        String creditsId = StringUtils.substringAfter(cleanId, "-");
        String movieId = StringUtils.substringBefore(cleanId, "-");

        if (getCache().get(Naming.Cache.MOVIE_CREDITS_CACHE_PREFIX + movieId) != null) {
            return (ExternalData) getCache().get(Naming.Cache.MOVIE_CREDITS_CACHE_PREFIX + movieId).getObjectValue();
        } else {
            try {
                MovieDb movie = getApiClient().getMovies().getDetails(Integer.parseInt(movieId), "en-US");
                Credits credits = getApiClient().getMovies().getCredits(Integer.parseInt(movieId), "en-US");
                String year = StringUtils.substringBefore(movie.getReleaseDate(), "-");
                String date = StringUtils.substringBeforeLast(movie.getReleaseDate(), "-");
                String path = new PathBuilder(MoviesItemMapper.PATH_LABEL).append(year)
                        .append(date).append(movieId).append(creditsId).build();
                String baseUrl = getConfiguration().getImageConfig().getBaseUrl();
                if (creditsId.startsWith(CREW)) {
                    String pid = creditsId.substring(CREW.length());
                    Crew crew = credits.getCrew().stream().filter(c -> c.getId() == Integer.parseInt(pid)).findFirst().orElseThrow();
                    Map<String, String[]> properties = new HashMap<>();
                    ExternalData data = new ExternalData(identifier, path, Naming.NodeType.CREW, properties);
                    if (StringUtils.isNotEmpty(crew.getDepartment())) {
                        properties.put("department", new String[] { crew.getDepartment() });
                    }
                    if (StringUtils.isNotEmpty(crew.getJob())) {
                        properties.put("job", new String[] { crew.getJob() });
                    }
                    properties.put("person", new String[] { PersonsItemMapper.ID_PREFIX + crew.getId() });
                    if (StringUtils.isNotEmpty(crew.getName())) {
                        properties.put("name", new String[] { crew.getName() });
                    }
                    if (StringUtils.isNotEmpty(crew.getProfilePath())) {
                        properties.put("profile", new String[] {
                                baseUrl + getConfiguration().getImageConfig().getProfileSizes().get(1) + crew.getProfilePath() });
                    }
                    getCache().put(new Element(Naming.Cache.MOVIE_CREDITS_CACHE_PREFIX + movieId, data));
                    return data;
                }
                if (creditsId.startsWith(CAST)) {
                    String pid = creditsId.substring(CAST.length());
                    Cast cast = credits.getCast().stream().filter(c -> c.getId() == Integer.parseInt(pid)).findFirst().orElseThrow();
                    Map<String, String[]> properties = new HashMap<>();
                    ExternalData data = new ExternalData(identifier, path, Naming.NodeType.CAST, properties);
                    if (StringUtils.isNotEmpty(cast.getCharacter())) {
                        properties.put("character", new String[] { cast.getCharacter() });
                    }
                    properties.put("order", new String[] { Integer.toString(cast.getOrder()) });
                    properties.put("cast_id", new String[] { Integer.toString(cast.getCastId()) });
                    properties.put("id", new String[] { Integer.toString(cast.getId()) });
                    if (StringUtils.isNotEmpty(cast.getName())) {
                        properties.put("name", new String[] { cast.getName() });
                    }
                    if (StringUtils.isNotEmpty(cast.getProfilePath())) {
                        properties.put("profile", new String[] {
                                baseUrl + getConfiguration().getImageConfig().getProfileSizes().get(1) + cast.getProfilePath() });
                    }
                    getCache().put(new Element(Naming.Cache.MOVIE_CREDITS_CACHE_PREFIX + movieId, data));
                    return data;
                }
                return null;
            } catch (TmdbException e) {
                LOGGER.warn("Error while getting movie credits for identifier: " + identifier, e);
                return null;
            }
        }
    }

    @Override public List<String> search(String nodeType, ExternalQuery query) throws RepositoryException {
        Map<String, Value> m = QueryHelper.getSimpleOrConstraints(query.getConstraint());
        String lang = QueryHelper.getLanguage(query.getConstraint());
        List<String> results = new ArrayList<>();
        if ( m.containsKey("id")) {
            String id = m.get("id").getString();
            try {
            MovieCredits credits;
                if (getCache().get(Naming.Cache.MOVIE_CREDITS_QUERY_CACHE_KEY_PREFIX + id) != null) {
                    credits = (MovieCredits) getCache().get(Naming.Cache.MOVIE_CREDITS_QUERY_CACHE_KEY_PREFIX + id).getObjectValue();
                } else {
                    credits = getApiClient().getPeople().getMovieCredits(Integer.parseInt(id), lang);
                    getCache().put(new Element(Naming.Cache.MOVIE_CREDITS_QUERY_CACHE_KEY_PREFIX + id, credits));
                }
                if (Naming.NodeType.CAST.equals(nodeType)) {
                    credits.getCrew().stream()
                            .filter(c -> StringUtils.isNotEmpty(c.getReleaseDate()))
                            .map(c -> buildPath(Integer.toString(c.getId()), CAST.concat(id), c.getReleaseDate()))
                            .forEach(results::add);
                }
                if (Naming.NodeType.CREW.equals(nodeType) && m.containsKey("id")) {
                    credits.getCrew().stream()
                            .filter(c -> StringUtils.isNotEmpty(c.getReleaseDate()))
                            .map(c -> buildPath(Integer.toString(c.getId()), CREW.concat(id), c.getReleaseDate()))
                            .forEach(results::add);
                }
            } catch (TmdbException e) {
                throw new RepositoryException("Error while searching credits", e);
            }
        }
        return results;
    }

    @Override public String getIdFromPath(String path) {
        return ID_PREFIX.concat(PathHelper.getParent(path)).concat("-").concat(PathHelper.getLeaf(path));
    }

    @Override public String getPathLabel() {
        return PATH_LABEL;
    }

    private String buildPath(String mid, String cid, String releaseDate) {
        String year = StringUtils.substringBefore(releaseDate, "-");
        String date = StringUtils.substringBeforeLast(releaseDate, "-");
        return new PathBuilder(MoviesItemMapper.PATH_LABEL).append(year).append(date).append(mid).append(cid).build();
    }
}
