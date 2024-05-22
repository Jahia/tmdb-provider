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
import org.jahia.modules.provider.tmdb.item.ItemMapperDescriptor;
import org.jahia.modules.provider.tmdb.item.mapper.RootItemMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jerome Blanchard
 */
public class RootItemMapperTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RootItemMapperTest.class);

    @Test
    public void testPathPattern() {
        ItemMapperDescriptor descriptor = RootItemMapper.class.getAnnotation(ItemMapperDescriptor.class);
        assertNotNull(descriptor);
        assertTrue("/".matches(descriptor.pathPattern()));
        assertFalse("".matches(descriptor.pathPattern()));
        assertFalse("//".matches(descriptor.pathPattern()));
        assertFalse("/others".matches(descriptor.pathPattern()));
        assertFalse("root".matches(descriptor.pathPattern()));
    }

    @Test
    public void testIdPattern() {
        ItemMapperDescriptor descriptor = RootItemMapper.class.getAnnotation(ItemMapperDescriptor.class);
        assertNotNull(descriptor);
        assertTrue("root".matches(descriptor.idPattern()));
        assertFalse("/root".matches(descriptor.idPattern()));
        assertFalse("myroot".matches(descriptor.idPattern()));
        assertFalse("".matches(descriptor.idPattern()));
        assertFalse("root/".matches(descriptor.idPattern()));
    }

    @Test
    public void testListChildren() {
        RootItemMapper rootItemMapper = new RootItemMapper();
        List<String> children = rootItemMapper.listChildren("/");
        LOGGER.info("Root children: {}", children);
        assertTrue(children.contains("movies"));
        assertTrue(children.contains("persons"));
        assertEquals(2, children.size());
    }

    @Test
    public void testGetData() {
        RootItemMapper rootItemMapper = new RootItemMapper();
        ExternalData data = rootItemMapper.getData("root");
        LOGGER.info("Root data: {}", data);
        assertEquals(Naming.NodeType.CONTENT_FOLDER, data.getType());
        assertEquals("root", data.getId());
        assertEquals("/", data.getPath());
        assertEquals("", data.getName());
        assertTrue(data.getProperties().containsKey(Constants.JCR_TITLE));
        assertArrayEquals(new String [] {"TMDB"}, data.getProperties().get(Constants.JCR_TITLE));
    }

    @Test
    public void testGetIdFromPath() {
        RootItemMapper rootItemMapper = new RootItemMapper();
        assertEquals("root", rootItemMapper.getIdFromPath("/"));
    }

    @Test
    public void testGetPathLabel() {
        RootItemMapper rootItemMapper = new RootItemMapper();
        assertEquals("", rootItemMapper.getPathLabel());
    }

}
