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

import info.movito.themoviedbapi.model.core.Movie;
import info.movito.themoviedbapi.model.core.MovieResultsPage;
import info.movito.themoviedbapi.model.movies.MovieDb;
import info.movito.themoviedbapi.tools.TmdbException;
import info.movito.themoviedbapi.tools.appendtoresponse.MovieAppendToResponse;
import info.movito.themoviedbapi.tools.builders.discover.DiscoverMovieParamBuilder;
import info.movito.themoviedbapi.tools.sortby.DiscoverMovieSortBy;
import net.sf.ehcache.Element;
import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
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
@Component(service = { MoviesCollection.class, ProviderDataCollection.class}, scope = ServiceScope.SINGLETON, immediate = true)
public class MoviesCollection implements ProviderDataCollection {

    private static final Logger LOGGER = LoggerFactory.getLogger(MoviesCollection.class);
    private static final String LIST_ID_CACHE_KEY = "movie-list-";
    public static final Set<String> LAZY_PROPERTIES = Set.of("original_title", "homepage", "status", "runtime", "imdb_id", "budget",
            "revenue");
    public static final Set<String> LAZY_I18N_PROPERTIES = Set.of(Constants.JCR_TITLE, "overview", "tagline", "poster_path");
    public static final String ID_PREFIX = "movie-";
    private TMDBClient client;
    private TMDBCache cache;

    @Reference
    public void setClient(TMDBClient client) {
        this.client = client;
    }

    @Reference
    public void setCache(TMDBCache cache) {
        this.cache = cache;
    }

    @Override
    public ProviderData getData(String identifier) {
        return getData(identifier, "en", false);
    }

    public ProviderData getData(String identifier, String language, boolean withLazyProperties) {
        Element element = cache.get(identifier);
        ProviderData cachedData = null;
        if (element != null) {
            cachedData = (ProviderData) element.getObjectValue();
            if (!withLazyProperties && cachedData.hasLanguage(language)) {
                return cachedData;
            }
            if (withLazyProperties && cachedData.hasProperty("runtime")) {
                if (cachedData.hasLanguage(language) && cachedData.hasProperty(language, "tagline")) {
                    return cachedData;
                }
            }
        }
        try {
            int mid = Integer.parseInt(identifier.substring(ID_PREFIX.length()));
            MovieDb movie = client.getMovies().getDetails(mid, language, MovieAppendToResponse.KEYWORDS);
            ProviderData data = map(movie, language, cachedData);
            cache.put(new Element(identifier, data));
            return data;
        } catch (Exception e) {
            LOGGER.warn("Error while getting movie " + identifier, e);
            return null;
        }
    }

    public List<ProviderData> list(String year, String month, String originLang) {
        String cacheKey = LIST_ID_CACHE_KEY.concat(year).concat("-").concat(month).concat("-").concat(originLang);
        Element element = cache.get(cacheKey);
        List<String> ids = new ArrayList<>();
        if (element != null) {
            ids = (List<String>) element.getObjectValue();
        } else {
            try {
                DiscoverMovieParamBuilder builder = getBuilder(year, month, originLang);
                MovieResultsPage page;
                do {
                    page = client.getDiscover().getMovie(builder);
                    if (page.getPage() == 1 && page.getTotalResults() > 500) {
                        LOGGER.warn("Too many movies for year {} month {}: {}", year, month, page.getTotalResults());
                    }
                    ids.addAll(page.getResults().stream().map(m -> ID_PREFIX + m.getId()).collect(Collectors.toList()));
                    page.getResults().stream().map(m -> this.map(m, "en") ).filter(Objects::nonNull).forEach(this::cache);
                    builder.page(page.getPage() + 1);
                } while (page.getPage() < page.getTotalPages() || page.getPage() >= 500);
                cache.put(new Element(cacheKey, ids));
            } catch (Exception e) {
                LOGGER.warn("Error while getting movies ", e);
                return Collections.emptyList();
            }
        }
        return ids.stream().map(this::getData).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public List<ProviderData> searchByTitle(String title, String lang, long limit) {
        List<String> ids = new ArrayList<>();
        try {
            MovieResultsPage page;
            int pageNb = 1;
            do {
                page = client.getSearch().searchMovie(title, false, lang,null, pageNb, null, null);
                ids.addAll(page.getResults().stream().map(m -> ID_PREFIX + m.getId()).collect(Collectors.toList()));
                page.getResults().stream().filter(m -> !this.isCached(ID_PREFIX + m.getId()))
                        .map(m -> this.map(m, lang) ).filter(Objects::nonNull).forEach(this::cache);
                pageNb++;
            } while (page.getPage() < page.getTotalPages() || ids.size() < limit);
        } catch (TmdbException e) {
            LOGGER.warn("Error while searching movie by title", e);
        }
        return ids.stream().map(this::getData).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public List<ProviderData> discover(long offset, long limit) {
        List<String> ids = new ArrayList<>();
        try {
            MovieResultsPage page;
            DiscoverMovieParamBuilder builder = new DiscoverMovieParamBuilder();
            builder.sortBy(DiscoverMovieSortBy.POPULARITY_DESC);
            builder.page((int) Math.max(1, offset / limit));
            do {
                page = client.getDiscover().getMovie(builder);
                ids.addAll(page.getResults().stream().map(m -> ID_PREFIX + m.getId()).collect(Collectors.toList()));
                page.getResults().stream().filter(m -> !this.isCached(ID_PREFIX + m.getId()))
                        .map(m -> this.map(m, "en") ).filter(Objects::nonNull).forEach(this::cache);
                builder.page(page.getPage() + 1);
            } while (page.getPage() < page.getTotalPages() || ids.size() < limit || ids.size() < 200);
        } catch (TmdbException e) {
            LOGGER.warn("Error while discovering movie", e);
        }
        return ids.stream().map(this::getData).filter(Objects::nonNull).collect(Collectors.toList());
    }

    protected ProviderData map(Movie movie, String language) {
        return this.map(language, movie.getId(), movie.getOriginalTitle(), movie.getAdult(), movie.getVoteAverage(), movie.getVoteCount(),
                movie.getPopularity(), movie.getBackdropPath(), movie.getReleaseDate(), movie.getOverview(), movie.getPosterPath(),
                movie.getTitle());
    }

    protected ProviderData map(MovieDb movie, String language, ProviderData existingData) {
        ProviderData data = this.map(language, movie.getId(), movie.getOriginalTitle(), movie.getAdult(), movie.getVoteAverage(),
                movie.getVoteCount(), movie.getPopularity(), movie.getBackdropPath(), movie.getReleaseDate(), movie.getOverview(),
                movie.getPosterPath(), movie.getTitle());
        if (data != null) {
            data.withProperty("runtime", new String[] { Long.toString(movie.getRuntime()) });
            if (StringUtils.isNotEmpty(movie.getHomepage())) {
                data.withProperty("homepage", new String[] { movie.getHomepage() });
            } else {
                data.withProperty("homepage", new String[] { "" });
            }
            if (StringUtils.isNotEmpty(movie.getStatus())) {
                data.withProperty("status", new String[] { movie.getStatus() });
            }
            if (StringUtils.isNotEmpty(movie.getImdbID())) {
                data.withProperty("imdb_id", new String[] { movie.getImdbID() });
            }
            data.withProperty("budget", new String[] { Long.toString(movie.getBudget()) });
            data.withProperty("revenue", new String[] { Double.toString(movie.getRevenue()) });
            data.withProperty("spoken_languages",
                    movie.getSpokenLanguages().stream().map(l -> l.getName()).collect(Collectors.toList()).toArray(new String[0]));
            data.withProperty("production_companies",
                    movie.getProductionCompanies().stream().map(c -> c.getName()).collect(Collectors.toList()).toArray(new String[0]));
            if (!movie.getGenres().isEmpty()) {
                data.withProperty("j:tagList",
                        movie.getGenres().stream().map(g -> g.getName()).collect(Collectors.toList()).toArray(new String[0]));
            }
            if (!movie.getKeywords().getKeywords().isEmpty()) {
                data.withProperty("j:keywords",
                        movie.getKeywords().getKeywords().stream().map(g -> g.getName()).collect(Collectors.toList()).toArray(new String[0]));
            }
            data.withProperty(language, "tagline", new String[] { movie.getTagline() });
            if (existingData != null) {
                existingData.getProperties().keySet().stream().filter(k -> !data.hasProperty(k)).forEach(k -> {
                    data.getProperties().put(k, existingData.getProperties().get(k));
                });
                data.getI18nProperties().putAll(existingData.getI18nProperties());
            }
        }
        return data;
    }

    private ProviderData map(String language, Integer id, String originalTitle, boolean adult, double voteAverage, int voteCount,
            double popularity, String backdropPath, String date, String overview, String posterPath, String title) {
        try {
            String baseUrl = client.getConfiguration().getImageConfig().getBaseUrl();
            ProviderData data = new ProviderData().withId(ID_PREFIX + id).withType(Naming.NodeType.MOVIE).withName(originalTitle);
            if (StringUtils.isNotEmpty(backdropPath)) {
                String backdropSize = client.getConfiguration().getImageConfig().getBackdropSizes().get(1);
                data.withProperty("backdrop_path", new String[] { baseUrl.concat(backdropSize).concat(backdropPath) });
            }
            if (StringUtils.isNotEmpty(date)) {
                data.withProperty("release_date", new String[] { date + "T00:00:00.000+00:00" });
            }
            data.withProperty("adult", new String[] { Boolean.toString(adult) });
            data.withProperty("vote_average", new String[] { Double.toString(voteAverage) });
            data.withProperty("vote_count", new String[] { Integer.toString(voteCount) });
            data.withProperty("popularity", new String[] { Double.toString(popularity) });
            if (StringUtils.isNotEmpty(originalTitle) && !originalTitle.equals("null")) {
                data.withProperty("original_title", new String[] { overview });
            }
            if (StringUtils.isNotEmpty(overview) && !overview.equals("null")) {
                data.withProperty(language, "overview", new String[] { overview });
            }
            if (StringUtils.isNotEmpty(posterPath) && !posterPath.equals("null")) {
                String posterSize = client.getConfiguration().getImageConfig().getPosterSizes().get(1);
                data.withProperty(language, "poster_path", new String[] { baseUrl.concat(posterSize).concat(posterPath) });
            }
            if (StringUtils.isNotEmpty(title) && !title.equals("null")) {
                data.withProperty(language, Constants.JCR_TITLE, new String[] {title});
            }
            return data;
        } catch (TmdbException e) {
            return null;
        }
    }

    private boolean isCached(String id) {
        return cache.get(id) != null;
    }

    private void cache(ProviderData data) {
        if (cache.get(data.getId()) == null) {
            cache.put(new Element(data.getId(), data));
        }
    }

    /*
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
     */


    private DiscoverMovieParamBuilder getBuilder(String year, String month, String language) {
        Calendar instance = Calendar.getInstance();
        instance.set(Calendar.YEAR, Integer.parseInt(year));
        instance.set(Calendar.MONTH, Integer.parseInt(month)-1);
        instance.set(Calendar.DAY_OF_MONTH, 1);
        String lastDay = Integer.toString(instance.getActualMaximum(Calendar.DAY_OF_MONTH));
        return new DiscoverMovieParamBuilder()
                .withOriginalLanguage(language)
                .includeVideo(false)
                .includeAdult(false)
                .primaryReleaseDateGte(year.concat("-").concat(month).concat("-01"))
                .primaryReleaseDateLte(year.concat("-").concat(month).concat("-").concat(lastDay))
                .page(1);
    }

}
