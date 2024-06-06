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

import org.jahia.api.Constants;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.provider.tmdb.helper.Naming;
import org.jahia.modules.provider.tmdb.data.CategoriesCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jerome Blanchard
 */
public class CategoriesCollectionTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CategoriesCollectionTest.class);
    private CategoriesCollection collection;

    @BeforeEach
    public void setUp() {
        collection = new CategoriesCollection();
    }

    @Test
    public void testGetRootItem() {
        ExternalData data = collection.getData("cid-root").toExternalData("/");
        LOGGER.info("Root item: {}", data);
        assertEquals(Naming.NodeType.CONTENT_FOLDER, data.getType());
        assertEquals("cid-root", data.getId());
        assertEquals("/", data.getPath());
        assertEquals("", data.getName());
        assertTrue(data.getProperties().containsKey(Constants.JCR_TITLE));
        assertArrayEquals(new String [] {"TMDB"}, data.getProperties().get(Constants.JCR_TITLE));
    }

    @Test
    public void testGetMoviesItem() {
        ExternalData data = collection.getData("cid-movies").toExternalData("/movies");
        LOGGER.info("Movies item: {}", data);
        assertEquals(Naming.NodeType.CONTENT_FOLDER, data.getType());
        assertEquals("cid-movies", data.getId());
        assertEquals("/movies", data.getPath());
        assertEquals("movies", data.getName());
        assertTrue(data.getProperties().containsKey(Constants.JCR_TITLE));
        assertArrayEquals(new String [] {"Movies"}, data.getProperties().get(Constants.JCR_TITLE));
    }

}
