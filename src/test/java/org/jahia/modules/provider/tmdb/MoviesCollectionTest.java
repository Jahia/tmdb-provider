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

import net.sf.ehcache.Element;
import org.jahia.modules.provider.tmdb.data.MoviesCollection;
import org.jahia.modules.provider.tmdb.data.ProviderData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jerome Blanchard
 */
public class MoviesCollectionTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MoviesCollectionTest.class);
    @Mock
    private TMDBCache cache;
    @InjectMocks
    private TMDBClient client;
    @InjectMocks
    private MoviesCollection collection;

    private List<Element> elements = new ArrayList<>();

    @BeforeEach
    public void setUp() {
        collection = new MoviesCollection();
        client = new TMDBClient();
        MockitoAnnotations.initMocks(this);
        client.start(new TestConfig());
    }

    @Test
    public void testListMovies() {
        List<ProviderData> data = collection.list("2024", "04", "fr");
        LOGGER.info("Movies: {}", data);
    }

}
