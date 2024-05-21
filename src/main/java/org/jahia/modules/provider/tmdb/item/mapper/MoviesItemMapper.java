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
package org.jahia.modules.provider.tmdb.item.mapper;

import org.jahia.api.Constants;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.provider.tmdb.helper.Naming;
import org.jahia.modules.provider.tmdb.helper.PathBuilder;
import org.jahia.modules.provider.tmdb.item.ItemMapper;
import org.jahia.modules.provider.tmdb.item.ItemMapperDescriptor;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Handler for movies node.
 * List of years for movies.
 *
 * @author Jerome Blanchard
 */
@ItemMapperDescriptor(pathPattern = "^/movies$", idPattern = "^movies$", supportedNodeType = {Naming.NodeType.CONTENT_FOLDER},
        hasLazyProperties = false)
public class MoviesItemMapper extends ItemMapper {
    public static final String PATH_LABEL = "movies";
    public static final String ID_PREFIX = "movies";

    //Children of the /movies node are the years we want to display in browsing (5 last years)
    private static final List<String> CHILDREN = IntStream
            .rangeClosed(Calendar.getInstance().get(Calendar.YEAR) - 10, Calendar.getInstance().get(Calendar.YEAR))
            .mapToObj(Integer::toString)
            .collect(Collectors.toList());

    public MoviesItemMapper() {
    }

    @Override public List<String> listChildren(String path) {
        return CHILDREN;
    }

    @Override public ExternalData getData(String identifier) {
        Map<String, String[]> properties = new HashMap<>();
        properties.put(Constants.JCR_TITLE, new String[] { PATH_LABEL });
        String path = new PathBuilder(PATH_LABEL).build();
        return new ExternalData(identifier, path, Naming.NodeType.CONTENT_FOLDER, properties);
    }

    @Override public String getIdFromPath(String path) {
        return ID_PREFIX;
    }

    @Override public String getPathLabel() {
        return PATH_LABEL;
    }
}
