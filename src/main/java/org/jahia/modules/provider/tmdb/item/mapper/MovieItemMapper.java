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

import info.movito.themoviedbapi.model.core.MovieResultsPage;
import info.movito.themoviedbapi.model.movies.Credits;
import info.movito.themoviedbapi.model.movies.MovieDb;
import info.movito.themoviedbapi.tools.TmdbException;
import info.movito.themoviedbapi.tools.appendtoresponse.MovieAppendToResponse;
import info.movito.themoviedbapi.tools.builders.discover.DiscoverMovieParamBuilder;
import info.movito.themoviedbapi.tools.sortby.DiscoverMovieSortBy;
import net.sf.ehcache.Element;
import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.external.ExternalQuery;
import org.jahia.modules.external.events.EventService;
import org.jahia.modules.external.query.QueryHelper;
import org.jahia.modules.provider.tmdb.helper.Naming;
import org.jahia.modules.provider.tmdb.helper.PathBuilder;
import org.jahia.modules.provider.tmdb.helper.PathHelper;
import org.jahia.modules.provider.tmdb.item.ItemMapper;
import org.jahia.modules.provider.tmdb.item.ItemMapperDescriptor;
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
@ItemMapperDescriptor(pathPattern = "^/movies/\\d{4}/\\d{2}/\\d+(?:/j:translation_[a-z]{2})?$", idPattern = "^movie-\\d+", supportedNodeType =
        {Naming.NodeType.MOVIE}, hasLazyProperties = true)
public class MovieItemMapper extends ItemMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(MovieItemMapper.class);
    public static final String PATH_LABEL = "movie";
    public static final String ID_PREFIX = "movie-";
    public static final String CAST = "cast_";
    public static final String CREW = "crew_";
    private static final int MAX_CHILD = 1000000;
    private static final Set<String> LAZY_PROPERTIES = Set.of("original_title", "homepage", "status", "runtime", "imdb_id", "budget", "revenue");
    private static final Set<String> LAZY_I18N_PROPERTIES = Set.of(Constants.JCR_TITLE, "overview", "tagline", "poster_path");

    public MovieItemMapper() {
    }

    @Override public List<String> listChildren(String path) {
        String movieId = PathHelper.getLeaf(path);
        if (movieId.startsWith("j:translation_")) {
            return Collections.emptyList();
        }
        String cacheKey = Naming.Cache.MOVIE_CREDITS_LIST_CACHE_PREFIX + movieId;
        if (getCache().get(cacheKey) != null) {
            return (List<String>) getCache().get(cacheKey).getObjectValue();
        } else {
            List<String> children = new ArrayList<>();
            try {
                Credits credits = getApiClient().getMovies().getCredits(Integer.parseInt(movieId), "en");
                children.addAll(credits.getCast().stream()
                        .map(c -> CAST.concat(Integer.toString(c.getId())))
                        .collect(Collectors.toList()));
                children.addAll(credits.getCrew().stream()
                        .map(c -> CREW.concat(Integer.toString(c.getId())))
                        .collect(Collectors.toList()));
                getCache().put(new Element(cacheKey, children));
            } catch (Exception e) {
                LOGGER.warn("Error while getting movie credits", e);
            }
            return children;
        }
    }

    @Override public ExternalData getData(String identifier) {
        String mid = identifier.substring(ID_PREFIX.length());
        String cacheKey = Naming.Cache.MOVIE_CACHE_PREFIX + mid;
        if (getCache().get(cacheKey) != null) {
            return (ExternalData) getCache().get(cacheKey).getObjectValue();
        } else {
            try {
                MovieDb movie = getApiClient().getMovies().getDetails(Integer.parseInt(mid), "en", MovieAppendToResponse.KEYWORDS);
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
                data.setLazyI18nProperties(Map.of("en", new HashSet<>(LAZY_I18N_PROPERTIES), "fr", new HashSet<>(LAZY_I18N_PROPERTIES)));
                this.notify(mid, data);
                getCache().put(new Element(cacheKey, data));
                return data;
            } catch (TmdbException e) {
                LOGGER.warn("Error while getting movie data", e);
                return null;
            }
        }
    }

    @Override public String getIdFromPath(String path) {
        return ID_PREFIX.concat(PathHelper.getLeaf(path));
    }

    @Override public String getPathLabel() {
        return PATH_LABEL;
    }

    @Override public List<String> search(String nodeType, ExternalQuery query) throws RepositoryException {
        Map<String, Value> m = QueryHelper.getSimpleOrConstraints(query.getConstraint());
        String lang = QueryHelper.getLanguage(query.getConstraint());
        List<String> results = new ArrayList<>();
        try {
            MovieResultsPage page;
            if (m.containsKey(Constants.JCR_TITLE)) {
                int pageNb = 1;
                do {
                    page = getApiClient().getSearch()
                            .searchMovie(m.get(Constants.JCR_TITLE).getString(), false, lang, null, pageNb, null, null);
                    results.addAll(page.getResults().stream().filter(movie -> StringUtils.isNotEmpty(movie.getReleaseDate()))
                            .map(movie -> buildMoviePath(Integer.toString(movie.getId()),movie.getReleaseDate())).collect(Collectors.toList()));
                    pageNb++;
                } while (page.getPage() < page.getTotalPages() && results.size() < query.getLimit());
            } else {
                DiscoverMovieParamBuilder builder = new DiscoverMovieParamBuilder();
                builder.sortBy(DiscoverMovieSortBy.POPULARITY_DESC);
                builder.page((int) Math.max(1, query.getOffset() / query.getLimit()));
                do {
                    page = getApiClient().getDiscover().getMovie(builder);
                    results.addAll(page.getResults().stream().filter(movie -> StringUtils.isNotEmpty(movie.getReleaseDate()))
                            .map(movie -> buildMoviePath(Integer.toString(movie.getId()), movie.getReleaseDate())).collect(Collectors.toList()));
                    builder.page(page.getPage() + 1);
                } while (page.getPage() < page.getTotalPages() && results.size() < query.getLimit() && results.size() < MAX_CHILD);
            }
        } catch (TmdbException e) {
            throw new RepositoryException("Error while searching movie", e);
        }
        return results;
    }

    @Override public String[] getProperty(String identifier, String lang, String propertyName) {
        MovieDb movie;
        String mid = identifier.substring(ID_PREFIX.length());
        String cacheKey = Naming.Cache.MOVIE_FULL_CACHE_PREFIX + lang + "-" + mid;
        Map<String, String[]> properties = new HashMap<>();
        try {
            if (getCache().get(cacheKey) != null) {
                properties = (Map<String, String[]>) getCache().get(cacheKey).getObjectValue();
            }
            if (properties.containsKey(propertyName)) {
                return properties.get(propertyName);
            } else {
                movie = getApiClient().getMovies().getDetails(Integer.parseInt(mid), lang, MovieAppendToResponse.KEYWORDS);
                String[] result = null;
                if (propertyName.equals(Constants.JCR_TITLE) && StringUtils.isNotEmpty(movie.getTitle()) && !movie.getTitle()
                        .equals("null")) {
                    result = new String[] { movie.getTitle() };
                } else if (propertyName.equals(Naming.Property.POSTER_PATH) && StringUtils.isNotEmpty(movie.getPosterPath())
                        && !movie.getPosterPath().equals("null")) {
                    String baseUrl = getConfiguration().getImageConfig().getBaseUrl();
                    result = new String[] { baseUrl + getConfiguration().getImageConfig().getPosterSizes().get(1) + movie.getPosterPath() };
                } else if (propertyName.equals("runtime")) {
                    result = new String[] { "0" };
                } else {
                    try {
                        String methodName = "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
                        Method method = MovieDb.class.getMethod(methodName);
                        Object value = method.invoke(movie);
                        if (value != null && !String.valueOf(value).equals("null")) {
                            result = new String[] { String.valueOf(value) };
                        }
                    } catch (Exception e) {
                        LOGGER.debug("Unable to access movie property: " + propertyName, e);
                    }
                }
                if (result != null) {
                    properties.put(propertyName, result);
                    getCache().put(new Element(cacheKey, properties));
                    return result;
                }
            }
        } catch (TmdbException e) {
            LOGGER.warn("Error while getting movie property: " + propertyName, e);
        }
        return new String[] { "" };
    }

    private void notify(String mid, ExternalData data) {
        String cacheKey = Naming.Cache.INDEXED_FULL_MOVIE_CACHE_PREFIX + mid;
        if (getCache().get(cacheKey) == null) {
            EventService eventService = BundleUtils.getOsgiService(EventService.class, null);
            JCRStoreProvider jcrStoreProvider = JCRSessionFactory.getInstance().getProviders().get("TMDBProvider");
            CompletableFuture.supplyAsync(() -> {
                try {
                    eventService.sendAddedNodes(Arrays.asList(data), jcrStoreProvider);
                    getCache().put(new Element(cacheKey, "indexed"));
                } catch (RepositoryException e) {
                    e.printStackTrace();
                }
                return "eventSent";
            });
        }
    }

    private String buildMoviePath(String mid, String releaseDate) {
        String year = releaseDate.split( "-")[0];
        String month = releaseDate.split( "-")[1];
        return new PathBuilder(MoviesItemMapper.PATH_LABEL).append(year).append(month).append(mid).build();
    }
}