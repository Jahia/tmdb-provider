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
package org.jahia.modules.provider.tmdb.data;

import info.movito.themoviedbapi.model.movies.Cast;
import info.movito.themoviedbapi.model.movies.Credits;
import info.movito.themoviedbapi.model.movies.Crew;
import info.movito.themoviedbapi.tools.TmdbException;
import net.sf.ehcache.Element;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.provider.tmdb.TMDBCache;
import org.jahia.modules.provider.tmdb.TMDBClient;
import org.jahia.modules.provider.tmdb.helper.Naming;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Jerome Blanchard
 */
@Component(service = { CreditsCollection.class, ProviderDataCollection.class}, scope = ServiceScope.SINGLETON, immediate = true)
public class CreditsCollection implements ProviderDataCollection {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreditsCollection.class);
    private static final String LIST_CACHE_KEY = "credits-list-";
    public static final String CAST = "cast_";
    public static final String CREW = "crew_";
    public static final String ID_PREFIX = "credits-";

    @Reference
    private TMDBClient client;
    @Reference
    private TMDBCache cache;

    @Override
    public ProviderData getData(String identifier) {
        Element element = cache.get(identifier);
        if (element != null) {
            return (ProviderData) element.getObjectValue();
        } else {
            try {
                ProviderData data = null;
                String cleanId = identifier.substring(ID_PREFIX.length());
                String creditsId = StringUtils.substringAfter(cleanId, "-");
                String movieId = StringUtils.substringBefore(cleanId, "-");
                Credits credits = client.getMovies().getCredits(Integer.parseInt(movieId), "en");
                if (creditsId.startsWith(CREW)) {
                    String pid = creditsId.substring(CREW.length());
                    Crew crew = credits.getCrew().stream().filter(c -> c.getId() == Integer.parseInt(pid)).findFirst().orElseThrow();
                    data = map(movieId, crew);
                }
                if (creditsId.startsWith(CAST)) {
                    String pid = creditsId.substring(CAST.length());
                    Cast cast = credits.getCast().stream().filter(c -> c.getId() == Integer.parseInt(pid)).findFirst().orElseThrow();
                    data = map(movieId, cast);
                }
                cache.put(new Element(identifier, data));
                return data;
            } catch (Exception e) {
                LOGGER.warn("Error while getting movie " + identifier, e);
                return null;
            }
        }
    }

    public List<ProviderData> list(String movieId) {
        String cacheKey = LIST_CACHE_KEY.concat(movieId);
        Element element = cache.get(cacheKey);
        if (element != null) {
            List<String> ids = (List<String>) element.getObjectValue();
            return ids.stream().map(this::getData).collect(Collectors.toList());
        } else {
            try {
                List<String> ids = new ArrayList<>();
                Credits credits = client.getMovies().getCredits(Integer.parseInt(movieId), "en");
                ids.addAll(credits.getCast().stream()
                        .map(c -> ID_PREFIX.concat(movieId).concat("-").concat(CAST).concat(Integer.toString(c.getId())))
                        .collect(Collectors.toList()));
                ids.addAll(credits.getCrew().stream()
                        .map(c -> ID_PREFIX.concat(movieId).concat("-").concat(CREW).concat(Integer.toString(c.getId())))
                        .collect(Collectors.toList()));
                credits.getCast().stream().map(c-> this.map(movieId, c)).filter(Objects::nonNull).forEach(this::cache);
                credits.getCrew().stream().map(c-> this.map(movieId, c)).filter(Objects::nonNull).forEach(this::cache);
                cache.put(new Element(cacheKey, ids));
                return ids.stream().map(this::getData).filter(Objects::nonNull).collect(Collectors.toList());
            } catch (Exception e) {
                LOGGER.warn("Error while getting movies ", e);
                return Collections.emptyList();
            }
        }
    }

    /*
    public List<String> search(String nodeType, ExternalQuery query) throws RepositoryException {
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

     */

    protected ProviderData map(String movieId, Crew crew) {
        try {
            String baseUrl = client.getConfiguration().getImageConfig().getBaseUrl();
            Map<String, String[]> properties = new HashMap<>();
            if (StringUtils.isNotEmpty(crew.getDepartment())) {
                properties.put("department", new String[] { crew.getDepartment() });
            }
            if (StringUtils.isNotEmpty(crew.getJob())) {
                properties.put("job", new String[] { crew.getJob() });
            }
            properties.put("person", new String[] { "person-" + crew.getId() });
            if (StringUtils.isNotEmpty(crew.getName())) {
                properties.put("name", new String[] { crew.getName() });
            }
            if (StringUtils.isNotEmpty(crew.getProfilePath())) {
                properties.put("profile", new String[] {
                        baseUrl + client.getConfiguration().getImageConfig().getProfileSizes().get(1) + crew.getProfilePath() });
            }
            String id = ID_PREFIX.concat(movieId).concat("-").concat(CREW).concat(Integer.toString(crew.getId()));
            return new ProviderData(id, Naming.NodeType.CREW, Integer.toString(crew.getId()), properties);
        } catch (TmdbException e) {
            return null;
        }
    }

    private ProviderData map(String movieId, Cast cast) {
        try {
            String baseUrl = client.getConfiguration().getImageConfig().getBaseUrl();
            Map<String, String[]> properties = new HashMap<>();
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
                        baseUrl + client.getConfiguration().getImageConfig().getProfileSizes().get(1) + cast.getProfilePath() });
            }
            String id = ID_PREFIX.concat(movieId).concat("-").concat(CAST).concat(Integer.toString(cast.getId()));
            return new ProviderData(id, Naming.NodeType.CAST, Integer.toString(cast.getId()), properties);
        } catch (TmdbException e) {
            return null;
        }
    }

    private void cache(ProviderData data) {
        cache.put(new Element(data.getId(), data));
    }

}
