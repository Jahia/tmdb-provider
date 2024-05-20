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
package org.jahia.modules.provider.tmdb.item;

import org.apache.commons.lang.StringUtils;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.provider.tmdb.helper.Naming;
import org.jahia.modules.provider.tmdb.helper.PathBuilder;
import org.jahia.modules.provider.tmdb.helper.PathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Short description of the class
 *
 * @author Jerome Blanchard
 */
@ItemMapperDescriptor(pathPattern = "^/lists/[a-z0-9]+/\\d+$", idPattern = "^mref-[a-z0-9]+\\d+$", supportedNodeType =
        {Naming.NodeType.CONTENT_REFERENCE}, hasLazyProperties = false)
public class MovieReferenceItemMapper extends ItemMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(MovieReferenceItemMapper.class);
    public static final String PATH_LABEL = "lists";
    public static final String ID_PREFIX = "mref-";

    public MovieReferenceItemMapper() {
    }

    @Override public List<String> listChildren(String path) {
        return Collections.emptyList();
    }

    @Override public ExternalData getData(String identifier) {
        String cleanId = StringUtils.substring(identifier, ID_PREFIX.length());
        String listId = StringUtils.substringBefore(cleanId, "-");
        int movieId = Integer.parseInt(StringUtils.substringAfter(cleanId, "-"));
        Map<String, String[]> properties = new HashMap<>();
        properties.put("j:node", new String[] { MovieItemMapper.ID_PREFIX + movieId });
        String path = new PathBuilder(PATH_LABEL).append(listId).append(movieId).build();
        return new ExternalData(identifier, path, Naming.NodeType.CONTENT_REFERENCE, properties);
    }

    @Override public String getIdFromPath(String path) {
        return ID_PREFIX.concat(PathHelper.getLeaf(path));
    }

    @Override public String getPathLabel() {
        return PATH_LABEL;
    }
}
