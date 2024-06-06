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
    public static final String CAST = "cast_";
    public static final String CREW = "crew_";
    public static final Set<String> LAZY_PROPERTIES = Set.of("original_title", "homepage", "status", "runtime", "imdb_id", "budget",
            "revenue", "genres", "keywords");
    public static final Set<String> LAZY_I18N_PROPERTIES = Set.of(Constants.JCR_TITLE, "overview", "tagline", "poster_path");
    public static final String ID_PREFIX = "movie-";

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
                Integer mid = Integer.parseInt(identifier.substring(ID_PREFIX.length()));
                MovieDb movie = client.getMovies().getDetails(mid, "en", MovieAppendToResponse.KEYWORDS);
                ProviderData data = map(movie, "en");
                cache.put(new Element(identifier, data));
                return data;
            } catch (Exception e) {
                LOGGER.warn("Error while getting movie " + identifier, e);
                return null;
            }
        }
    }

    public List<ProviderData> list(String year, String month, String originLang) {
        DiscoverMovieParamBuilder builder = getBuilder(year, month, originLang);
        Element element = cache.get(LIST_ID_CACHE_KEY);
        if (element != null) {
            List<String> ids = (List<String>) element.getObjectValue();
            return ids.stream().map(this::getData).collect(Collectors.toList());
        } else {
            try {
                List<ProviderData> results = new ArrayList<>();
                List<String> ids = new ArrayList<>();
                MovieResultsPage page;
                do {
                    page = client.getDiscover().getMovie(builder);
                    ids.addAll(page.getResults().stream().map(m -> ID_PREFIX + m.getId()).collect(Collectors.toList()));
                    page.getResults().stream().map(this::map).filter(Objects::nonNull).forEach(this::cache);
                    builder.page(page.getPage() + 1);
                } while (page.getPage() < page.getTotalPages());
                cache.put(new Element(LIST_ID_CACHE_KEY, ids));
                return ids.stream().map(this::getData).filter(Objects::nonNull).collect(Collectors.toList());
            } catch (Exception e) {
                LOGGER.warn("Error while getting movies ", e);
                return Collections.emptyList();
            }
        }
    }

    protected ProviderData map(MovieDb movie, String language) {
        //TODO add all lazy properties in cache (not the i18n ones)
        return this.map(movie.getId(), movie.getAdult(), movie.getVoteAverage(), movie.getVoteCount(), movie.getPopularity(),
                movie.getBackdropPath(), movie.getReleaseDate());
    }

    protected ProviderData map(Movie movie) {
        return this.map(movie.getId(), movie.getAdult(), movie.getVoteAverage(), movie.getVoteCount(), movie.getPopularity(),
                movie.getBackdropPath(), movie.getReleaseDate());
    }

    private ProviderData map(Integer id, boolean adult, double voteAverage, int voteCount, double popularity, String backdropPath, String date) {
        try {
            String baseUrl = client.getConfiguration().getImageConfig().getBaseUrl();
            Map<String, String[]> properties = new HashMap<>();
            if (StringUtils.isNotEmpty(backdropPath)) {
                String backdropSize = client.getConfiguration().getImageConfig().getBackdropSizes().get(1);
                properties.put("backdrop_path", new String[] { baseUrl.concat(backdropSize).concat(backdropPath) });
            }
            if (StringUtils.isNotEmpty(date)) {
                properties.put("release_date", new String[] { date + "T00:00:00.000+00:00" });
            }
            properties.put("adult", new String[] { Boolean.toString(adult) });
            properties.put("vote_average", new String[] { Double.toString(voteAverage) });
            properties.put("vote_count", new String[] { Integer.toString(voteCount) });
            properties.put("popularity", new String[] { Double.toString(popularity) });
            //TODO add genres as keywords
            return new ProviderData(ID_PREFIX + id, Naming.NodeType.CONTENT_FOLDER, Integer.toString(id), properties);
        } catch (TmdbException e) {
            return null;
        }
    }

    private void cache(ProviderData data) {
        cache.put(new Element(data.getId(), data));
    }

    private DiscoverMovieParamBuilder getBuilder(String year, String month, String language) {
        Calendar instance = Calendar.getInstance();
        instance.set(Calendar.YEAR, Integer.parseInt(year));
        instance.set(Calendar.MONTH, Integer.parseInt(month));
        instance.set(Calendar.DAY_OF_MONTH, 1);
        instance.roll(Calendar.DAY_OF_MONTH, false);
        String lastDay = Integer.toString(instance.get(Calendar.DAY_OF_MONTH));
        return new DiscoverMovieParamBuilder()
                .withOriginalLanguage(language)
                .primaryReleaseDateGte(year.concat("-").concat(month).concat("-01"))
                .primaryReleaseDateLte(year.concat("-").concat(month).concat("-").concat(lastDay))
                .page(1);
    }

}
