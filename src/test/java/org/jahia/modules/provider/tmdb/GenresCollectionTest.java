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
import org.jahia.api.Constants;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.provider.tmdb.data.CategoriesCollection;
import org.jahia.modules.provider.tmdb.data.GenresCollection;
import org.jahia.modules.provider.tmdb.data.MoviesCollection;
import org.jahia.modules.provider.tmdb.data.ProviderData;
import org.jahia.modules.provider.tmdb.helper.Naming;
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
public class GenresCollectionTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenresCollectionTest.class);

    private CacheProvider cacheProvider;
    private GenresCollection collection;

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
        collection = new GenresCollection();
        collection.setCache(cache);
        collection.setClient(client);
    }

    @Test
    public void testListAndGetMovies() {
        long start = System.currentTimeMillis();
        List<ProviderData> data = collection.list();
        long stop = System.currentTimeMillis();
        long time1 = (stop-start);
        LOGGER.info("List {} Genres in {} ms", data.size(), time1);
        assertTrue(data.size() > 0);
        assertTrue(data.stream().anyMatch(d -> d.getId().startsWith("genre-28")));

        start = System.currentTimeMillis();
        data = collection.list();
        stop = System.currentTimeMillis();
        long time2 = (stop-start);
        LOGGER.info("List {} Genres in {} ms", data.size(), time2);
        assertTrue(time2 < time1);

        ProviderData genre = data.stream().filter(d -> d.getId().startsWith("genre-28")).findFirst().orElse(null);
        assertTrue(genre != null);
        assertTrue(genre.hasProperty("name"));

    }

}
