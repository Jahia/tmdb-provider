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

import org.jahia.modules.external.ExternalData;
import org.jahia.modules.provider.tmdb.data.CategoriesCollection;
import org.jahia.modules.provider.tmdb.binding.RootNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jerome Blanchard
 */
public class RootBindingTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RootBindingTest.class);
    private RootNode mapper;

    @BeforeEach
    public void setUp() {
        mapper = new RootNode();
        mapper.setCategories(new CategoriesCollection());
    }

    @Test
    public void testPathPattern() {
        assertTrue("/".matches(mapper.getPathPattern()));
        assertFalse("".matches(mapper.getPathPattern()));
        assertFalse("//".matches(mapper.getPathPattern()));
        assertFalse("/others".matches(mapper.getPathPattern()));
        assertFalse("root".matches(mapper.getPathPattern()));
    }

    @Test
    public void testIdPattern() {
        assertNotNull(mapper);
        LOGGER.info("ID pattern: {}", mapper.getIdPattern());
        assertTrue("cid-root".matches(mapper.getIdPattern()));
        assertFalse("/root".matches(mapper.getIdPattern()));
        assertFalse("blabla".matches(mapper.getIdPattern()));
        assertFalse("".matches(mapper.getIdPattern()));
        assertFalse("root/".matches(mapper.getIdPattern()));
    }

    @Test
    public void testListChildren() {
        List<ExternalData> children = mapper.listChildren("/");
        LOGGER.info("Root children: {}", children);
        assertEquals(3, children.size());
        assertTrue(children.stream().anyMatch(c -> c.getId().equals("cid-movies")));
        assertTrue(children.stream().anyMatch(c -> c.getId().equals("cid-persons")));
        assertTrue(children.stream().anyMatch(c -> c.getId().equals("cid-genres")));
    }

    @Test
    public void testGetIdFromPath() {
        assertEquals("cid-root", mapper.findNodeId("/"));
    }

}
