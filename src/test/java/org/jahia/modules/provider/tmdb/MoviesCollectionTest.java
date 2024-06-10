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
package org.jahia.modules.provider.tmdb;

import net.sf.ehcache.CacheManager;
import org.jahia.modules.provider.tmdb.data.MoviesCollection;
import org.jahia.modules.provider.tmdb.data.ProviderData;
import org.jahia.services.cache.CacheProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jerome Blanchard
 */
public class MoviesCollectionTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MoviesCollectionTest.class);

    private CacheProvider cacheProvider;
    private MoviesCollection collection;

    @BeforeEach
    public void setUp() {
        CacheManager cacheManager = CacheManager.newInstance();
        cacheManager.clearAll();
        cacheManager.addCacheIfAbsent("tmdb");
        cacheProvider = Mockito.mock(CacheProvider.class);
        Mockito.when(cacheProvider.getCacheManager()).thenReturn(cacheManager);
        TMDBCache cache = new TMDBCache();
        cache.setCacheProvider(cacheProvider);
        cache.start();
        TMDBClient client = new TMDBClient();
        client.setCache(cache);
        client.start(new TestConfig());
        collection = new MoviesCollection();
        collection.setCache(cache);
        collection.setClient(client);
    }

    @Test
    public void testListAndGetMovies() {
        long start = System.currentTimeMillis();
        List<ProviderData> data = collection.list("2024", "04", "fr");
        long stop = System.currentTimeMillis();
        long time1 = (stop-start);
        LOGGER.info("List {} Movies in {} ms", data.size(), time1);
        assertFalse(data.isEmpty());
        assertTrue(data.stream().anyMatch(d -> d.getId().startsWith("movie-1001023")));

        start = System.currentTimeMillis();
        data = collection.list("2024", "04", "fr");
        stop = System.currentTimeMillis();
        long time2 = (stop-start);
        LOGGER.info("List {} Movies in {} ms", data.size(), time2);
        assertTrue(time2 < time1);

        ProviderData movie = data.stream().filter(d -> d.getId().startsWith("movie-1001023")).findFirst().orElse(null);
        assertNotNull(movie);
        assertTrue(movie.hasProperty("popularity"));
        assertFalse(movie.hasProperty("budget"));
        assertTrue(movie.hasLanguage("en"));
        assertFalse(movie.hasLanguage("fr"));

        movie = collection.getData("movie-1084863", "fr", true);
        assertTrue(movie.hasProperty("popularity"));
        assertTrue(movie.hasProperty("budget"));
        assertEquals(1, movie.getProperties().keySet().stream().filter(k -> k.equals("budget")).count());
        assertTrue(movie.hasLanguage("en"));
        assertTrue(movie.hasLanguage("fr"));
        assertTrue(movie.hasProperty("fr", "tagline"));
        assertFalse(movie.hasProperty("en", "tagline"));
        movie = collection.getData("movie-1084863", "en", true);
        assertTrue(movie.hasProperty("fr", "tagline"));
        assertTrue(movie.hasProperty("en", "tagline"));

    }

    @Test
    public void testGetMovie() {
        long start = System.currentTimeMillis();
        ProviderData data = collection.getData("movie-1084863");
        long stop = System.currentTimeMillis();
        long time1 = (stop-start);
        LOGGER.info("Get Movie in {} ms", time1);
        assertTrue(data.getId().contains("1084863"));

        start = System.currentTimeMillis();
        data = collection.getData("movie-1084863");
        stop = System.currentTimeMillis();
        long time2 = (stop-start);
        LOGGER.info("Get Movie in {} ms", time2);
        assertTrue(time2 < time1);
    }

    @Test
    public void testGetFullMovieAndMerge() {
        long start = System.currentTimeMillis();
        ProviderData data = collection.getData("movie-1084863", "en", true);
        long stop = System.currentTimeMillis();
        long time1 = (stop-start);
        LOGGER.info("Get Movie in {} ms", time1);
        assertTrue(data.getId().contains("1084863"));
        assertTrue(data.hasProperty("budget"));
        assertTrue(data.hasLanguage("en"));
        assertFalse(data.hasLanguage("fr"));

        start = System.currentTimeMillis();
        data = collection.getData("movie-1084863", "fr", true);
        stop = System.currentTimeMillis();
        long time2 = (stop-start);
        LOGGER.info("Get Movie in {} ms", time2);
        assertTrue(data.getId().contains("1084863"));
        assertTrue(data.hasProperty("budget"));
        assertTrue(data.hasLanguage("en"));
        assertTrue(data.hasLanguage("fr"));
    }

}
