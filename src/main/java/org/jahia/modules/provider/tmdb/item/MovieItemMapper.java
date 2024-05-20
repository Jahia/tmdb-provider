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
package org.jahia.modules.provider.tmdb.item;

import info.movito.themoviedbapi.model.core.MovieResultsPage;
import info.movito.themoviedbapi.model.movies.Credits;
import info.movito.themoviedbapi.model.movies.MovieDb;
import info.movito.themoviedbapi.tools.TmdbException;
import info.movito.themoviedbapi.tools.appendtoresponse.MovieAppendToResponse;
import info.movito.themoviedbapi.tools.builders.discover.DiscoverMovieParamBuilder;
import info.movito.themoviedbapi.tools.sortby.DiscoverMovieSortBy;
import net.sf.ehcache.Element;
import org.apache.commons.lang.StringUtils;
import org.apache.groovy.util.Maps;
import org.jahia.api.Constants;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.external.ExternalQuery;
import org.jahia.modules.external.events.EventService;
import org.jahia.modules.external.query.QueryHelper;
import org.jahia.modules.provider.tmdb.helper.Naming;
import org.jahia.modules.provider.tmdb.helper.PathBuilder;
import org.jahia.modules.provider.tmdb.helper.PathHelper;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRStoreProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Short description of the class
 *
 * @author Jerome Blanchard
 */
public class MovieItemMapper extends ItemMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(MovieItemMapper.class);
    public static final String CAST = "cast_";
    public static final String CREW = "crew_";
    private static final Set<String> LAZY_PROPERTIES = Set.of("original_title", "homepage", "status", "runtime", "imdb_id", "budget", "revenue");
    private static final Set<String> LAZY_I18N_PROPERTIES = Set.of(Constants.JCR_TITLE, "overview", "tagline", "poster_path");

    public MovieItemMapper() {
    }

    @Override public List<String> listChildren(String path) {
        String node = PathHelper.getLeaf(path);
        if (getCache().get(Naming.Cache.MOVIE_CREDITS_LIST_CACHE_PREFIX + node) != null) {
            return (List<String>) getCache().get(Naming.Cache.MOVIE_CREDITS_LIST_CACHE_PREFIX + node).getObjectValue();
        } else {
            List<String> children = new ArrayList<>();
            try {
                Credits credits = getApiClient().getMovies().getCredits(Integer.parseInt(node), "en-US");
                children.addAll(credits.getCast().stream()
                        .map(c -> CAST.concat(Integer.toString(c.getId())))
                        .collect(Collectors.toList()));
                children.addAll(credits.getCrew().stream()
                        .map(c -> CREW.concat(Integer.toString(c.getId())))
                        .collect(Collectors.toList()));
                getCache().put(new Element(Naming.Cache.MOVIE_CREDITS_LIST_CACHE_PREFIX + node, children));
            } catch (Exception e) {
                LOGGER.warn("Error while getting movie credits", e);
            }
            return children;
        }
    }

    @Override public ExternalData getData(String identifier) {
        String mid = identifier.substring(ItemMapperDescriptor.MOVIE_ID.getIdPrefix().length());
        if (getCache().get(Naming.Cache.MOVIE_CACHE_PREFIX + mid) != null) {
            return (ExternalData) getCache().get(Naming.Cache.MOVIE_CACHE_PREFIX + mid).getObjectValue();
        } else {
            try {
                MovieDb movie = getApiClient().getMovies().getDetails(Integer.parseInt(mid), "en-US", MovieAppendToResponse.KEYWORDS);
                String baseUrl = getConfiguration().getImageConfig().getBaseUrl();
                Map<String, String[]> properties = new HashMap<>();
                if (StringUtils.isNotEmpty(movie.getBackdropPath())) {
                    properties.put("backdrop_path",
                            new String[] { baseUrl.concat(getConfiguration().getImageConfig().getBackdropSizes().get(1)).concat(movie.getBackdropPath()) });
                }
                if (StringUtils.isNotEmpty(movie.getReleaseDate())) {
                    properties.put("release_date", new String[] { movie.getReleaseDate() + "T00:00:00.000+00:00" });
                }
                properties.put("adult", new String[] { Boolean.toString(movie.getAdult()) });
                properties.put("vote_average", new String[] { Double.toString(movie.getVoteAverage()) });
                properties.put("vote_count", new String[] { Integer.toString(movie.getVoteCount()) });
                properties.put("popularity", new String[] { Double.toString(movie.getPopularity()) });
                if (!movie.getGenres().isEmpty()) {
                    properties.put("j:tagList", movie.getGenres().stream().map(g -> g.getName()).collect(Collectors.toList()).toArray(new String[0]));
                }
                if (!movie.getKeywords().getKeywords().isEmpty()) {
                    properties.put("j:tagList", movie.getKeywords().getKeywords().stream().map(g -> g.getName()).collect(Collectors.toList()).toArray(new String[0]));
                }
                String path = buildMoviePath(mid, movie.getReleaseDate());
                ExternalData data = new ExternalData(identifier, path, Naming.NodeType.MOVIE, properties);
                data.setLazyProperties(new HashSet<>(LAZY_PROPERTIES));
                data.setLazyI18nProperties(Maps.of("en", new HashSet<>(LAZY_I18N_PROPERTIES), "fr", new HashSet<>(LAZY_I18N_PROPERTIES)));
                this.notify(mid, data);
                getCache().put(new Element(Naming.Cache.MOVIE_CACHE_PREFIX + mid, data));
                return data;
            } catch (TmdbException e) {
                LOGGER.warn("Error while getting movie data", e);
                return null;
            }
        }
    }

    @Override public String getIdFromPath(String path) {
        return ItemMapperDescriptor.MOVIE_ID.getIdPrefix().concat(PathHelper.getLeaf(path));
    }

    @Override public List<String> search(String nodeType, ExternalQuery query) throws RepositoryException {
        Map<String, Value> m = QueryHelper.getSimpleOrConstraints(query.getConstraint());
        String lang = QueryHelper.getLanguage(query.getConstraint());
        List<String> results = new ArrayList<>();
        try {
            MovieResultsPage page;
            if (m.containsKey(Constants.JCR_TITLE)) {
                do {
                    page = getApiClient().getSearch()
                            .searchMovie(m.get(Constants.JCR_TITLE).getString(), false, lang, null, null, null, null);
                    results.addAll(page.getResults().stream().map(movie -> buildMoviePath(Integer.toString(movie.getId()), movie.getReleaseDate())).collect(Collectors.toList()));
                } while (page.getPage() < page.getTotalPages() || results.size() >= query.getLimit());
            } else {
                do {
                    DiscoverMovieParamBuilder builder = new DiscoverMovieParamBuilder();
                    builder.sortBy(DiscoverMovieSortBy.POPULARITY_DESC);
                    builder.page((int) query.getOffset() / 20);
                    page = getApiClient().getDiscover().getMovie(builder);
                    results.addAll(page.getResults().stream().map(movie -> buildMoviePath(Integer.toString(movie.getId()), movie.getReleaseDate())).collect(Collectors.toList()));
                } while (page.getPage() < page.getTotalPages() || results.size() >= query.getLimit());
            }
        } catch (TmdbException e) {
            throw new RepositoryException("Error while searching movie", e);
        }
        return results;
    }

    @Override public String[] getProperty(String mid, String lang, String propertyName) {
        MovieDb movie;
        try {
            if (getCache().get(Naming.Cache.MOVIE_API_CACHE_PREFIX + lang + "-" + mid) != null) {
                movie = (MovieDb) getCache().get(Naming.Cache.MOVIE_API_CACHE_PREFIX + lang + "-" + mid).getObjectValue();
            } else {
                movie = getApiClient().getMovies().getDetails(Integer.parseInt(mid), lang, MovieAppendToResponse.KEYWORDS);
                getCache().put(new Element(Naming.Cache.MOVIE_API_CACHE_PREFIX + lang + "-" + mid, movie.toString()));
            }
            if (propertyName.equals(Constants.JCR_TITLE) && StringUtils.isNotEmpty(movie.getTitle()) && !movie.getTitle().equals("null")) {
                return new String[] { movie.getTitle() };
            } else if (propertyName.equals(Naming.Property.POSTER_PATH) && StringUtils.isNotEmpty(movie.getPosterPath())
                    && !movie.getPosterPath().equals("null")) {
                String baseUrl = getConfiguration().getImageConfig().getBaseUrl();
                return new String[] {
                        baseUrl + getConfiguration().getImageConfig().getPosterSizes().get(1) + movie.getPosterPath() };
            } else if (propertyName.equals("runtime")) {
                return new String[] { "0" };
            } else {
                try {
                    String methodName = "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
                    Method method = MovieDb.class.getMethod(methodName);
                    String value = (String) method.invoke(movie);
                    if (StringUtils.isNotEmpty(value) && !value.equals("null")) {
                        return new String[] { value };
                    }
                } catch (Exception e) {
                    LOGGER.debug("Unable to access movie property: " + propertyName, e);
                }
            }
        } catch (TmdbException e) {
            LOGGER.warn("Error while getting movie property: " + propertyName, e);
        }
        return new String[] { "" };
    }

    private void notify(String mid, ExternalData data) {
        if (getCache().get(Naming.Cache.INDEXED_FULL_MOVIE_CACHE_PREFIX + mid) == null) {
            EventService eventService = BundleUtils.getOsgiService(EventService.class, null);
            JCRStoreProvider jcrStoreProvider = JCRSessionFactory.getInstance().getProviders().get("TMDBProvider");
            CompletableFuture.supplyAsync(() -> {
                try {
                    eventService.sendAddedNodes(Arrays.asList(data), jcrStoreProvider);
                    getCache().put(new Element(Naming.Cache.INDEXED_FULL_MOVIE_CACHE_PREFIX + mid, "indexed"));
                } catch (RepositoryException e) {
                    e.printStackTrace();
                }
                return "eventSent";
            });
        }
    }

    private String buildMoviePath(String mid, String releaseDate) {
        String year = StringUtils.substringBefore(releaseDate, "-");
        String date = StringUtils.substringBeforeLast(releaseDate, "-");
        return new PathBuilder(ItemMapperDescriptor.MOVIES).append(year).append(date).append(mid).build();
    }
}