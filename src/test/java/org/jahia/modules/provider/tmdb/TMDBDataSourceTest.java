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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import org.jahia.modules.external.ExternalContentStoreProvider;
import org.jahia.modules.external.ExternalContentStoreProviderFactory;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.provider.tmdb.helper.Naming;
import org.jahia.services.cache.CacheProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.lang.annotation.Annotation;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jerome Blanchard
 */
public class TMDBDataSourceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TMDBDataSourceTest.class);

    @Mock
    private ExternalContentStoreProviderFactory externalContentStoreProviderFactory;
    @Mock
    private ExternalContentStoreProvider externalContentStoreProvider;
    @Mock
    private CacheProvider cacheProvider;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache cache;

    @InjectMocks
    private TMDBDataSource tmdbDataSource;


    @BeforeEach
    public void setUp() throws RepositoryException {
        tmdbDataSource = new TMDBDataSource();
        MockitoAnnotations.initMocks(this);
        Mockito.when(externalContentStoreProviderFactory.newProvider()).thenReturn(externalContentStoreProvider);
        Mockito.when(cacheProvider.getCacheManager()).thenReturn(cacheManager);
        Mockito.when(cacheManager.cacheExists(Naming.Cache.TMDB_CACHE)).thenReturn(true);
        Mockito.when(cacheManager.getCache(Naming.Cache.TMDB_CACHE)).thenReturn(cache);
        tmdbDataSource.start(new TestConfig());
    }

    @AfterEach
    public void tearDown() {
        tmdbDataSource.stop();
    }

    @Test
    public void testGetByPath() throws RepositoryException {
        ExternalData root = tmdbDataSource.getItemByPath("/");
        assertNotNull(root);
        assertEquals("root", root.getId());
        assertEquals("/", root.getPath());
        assertEquals(Naming.NodeType.CONTENT_FOLDER, root.getType());

        ExternalData movies = tmdbDataSource.getItemByPath("/movies");
        assertNotNull(movies);
        assertEquals("movies", movies.getId());
        assertEquals("/movies", movies.getPath());
        assertEquals(Naming.NodeType.CONTENT_FOLDER, movies.getType());
    }

    @Test
    public void testGetChildren() throws RepositoryException {
        List<String> root = tmdbDataSource.getChildren("/");
        assertFalse(root.isEmpty());
        assertTrue(root.contains("movies"));
        assertTrue(root.contains("persons"));

        List<String> movies = tmdbDataSource.getChildren("/movies");
        assertFalse(movies.isEmpty());
        assertTrue(movies.contains("2024"));
        assertTrue(movies.contains("2023"));

        List<String> year2024 = tmdbDataSource.getChildren("/movies/2024");
        assertFalse(year2024.isEmpty());
        assertTrue(year2024.contains("2024-01"));
        assertTrue(year2024.contains("2024-02"));

        List<String> month202401 = tmdbDataSource.getChildren("/movies/2024/2024-01");
        LOGGER.info("GetChildren /movies/2024/2024-01: " + month202401.size());
        assertFalse(month202401.isEmpty());
        assertTrue(month202401.contains("1247285"));
        Mockito.verify(cache, Mockito.times(1)).put(Mockito.any());

        //Test that it should have been in cache
        //Test a second call to check that cache is used
        //Maybe mock the cache response to check the result size

        List<String> movie = tmdbDataSource.getChildren("/movies/2024/2024-01/866398");
        LOGGER.info("GetChildren /movies/2024/2024-01/866398: " + movie.size());
        assertFalse(movie.isEmpty());
        assertTrue(movie.contains("cast_976"));
        assertTrue(movie.contains("crew_72102"));

        //There is no more children under a cast or crew node
        List<String> cast = tmdbDataSource.getChildren("/movies/2024/2024-01/866398/cast_976");
        assertTrue(cast.isEmpty());
        List<String> crew = tmdbDataSource.getChildren("/movies/2024/2024-01/866398/crew_72102");
        assertTrue(crew.isEmpty());


        //There is no children under persons
        List<String> persons = tmdbDataSource.getChildren("/persons");
        assertTrue(persons.isEmpty());

        List<String> person = tmdbDataSource.getChildren("/persons/976");
        assertTrue(person.isEmpty());
    }

    @Test
    public void testGetByIdentifier() throws RepositoryException {
        ExternalData root = tmdbDataSource.getItemByIdentifier("root");
        assertNotNull(root);
        assertEquals("root", root.getId());
        assertEquals("/", root.getPath());
        assertEquals(Naming.NodeType.CONTENT_FOLDER, root.getType());

        ExternalData movies = tmdbDataSource.getItemByIdentifier("movies");
        assertNotNull(movies);
        assertEquals("movies", movies.getId());
        assertEquals("/movies", movies.getPath());
        assertEquals(Naming.NodeType.CONTENT_FOLDER, movies.getType());

        ExternalData onemovie = tmdbDataSource.getItemByIdentifier("mid-14");
        assertNotNull(onemovie);
        assertEquals("mid-14", onemovie.getId());
        assertEquals("/movies/1999/1999-09/14", onemovie.getPath());
        assertEquals(Naming.NodeType.MOVIE, onemovie.getType());

        ExternalData crew = tmdbDataSource.getItemByIdentifier("mcredits-14-crew_72102");
        assertNotNull(crew);
        assertEquals("mcredits-14-crew_72102", crew.getId());
        assertEquals("/movies/1999/1999-09/14/crew_72102", crew.getPath());
        assertEquals(Naming.NodeType.CREW, crew.getType());

        ExternalData cast = tmdbDataSource.getItemByIdentifier("mcredits-14-cast_976");
        assertNotNull(cast);
        assertEquals("mcredits-14-cast_976", cast.getId());
        assertEquals("/movies/1999/1999-09/14/cast_976", cast.getPath());
        assertEquals(Naming.NodeType.CAST, cast.getType());

        ExternalData persons = tmdbDataSource.getItemByIdentifier("persons");
        assertNotNull(persons);
        assertEquals("persons", persons.getId());
        assertEquals("/persons", persons.getPath());
        assertEquals(Naming.NodeType.CONTENT_FOLDER, persons.getType());

        ExternalData person = tmdbDataSource.getItemByIdentifier("person-976");
        assertNotNull(person);
        assertEquals("person-976", person.getId());
        assertEquals("/persons/976", person.getPath());
        assertEquals(Naming.NodeType.MOVIE_PERSON, person.getType());
    }

    @Test
    public void testSearch() throws RepositoryException {

    }

    class TestConfig implements TMDBDataSource.Config {

        @Override public String apiKey() {
            return "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiIyN2Q3MWViNzU3ZmNkZDY1NTk1OTYwM2RlYzQxNWZkMyIsInN1YiI6IjY2NDQ1ZmQ2N2EwYTk1MzIzYWVjM2YxNiIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.GUoWlbqEtU3e8Btf2yTh1cGosGJE-63IKrASY2D8JgE";
        }

        @Override public String mountPoint() {
            return "/dummy";
        }

        @Override public Class<? extends Annotation> annotationType() {
            return null;
        }
    }


}
