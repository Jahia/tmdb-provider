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

import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.provider.tmdb.helper.Naming;
import org.jahia.modules.provider.tmdb.helper.PathBuilder;
import org.jahia.modules.provider.tmdb.helper.PathHelper;
import org.jahia.modules.provider.tmdb.item.ItemMapper;
import org.jahia.modules.provider.tmdb.item.ItemMapperDescriptor;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Handler for movies years node.
 * List month of the year limiting to current month if parent is current year
 *
 * @author Jerome Blanchard
 */
@ItemMapperDescriptor(pathPattern = "^/movies/\\d{4}$", idPattern = "^movies-\\d{4}$", supportedNodeType =
        {Naming.NodeType.CONTENT_FOLDER}, hasLazyProperties = false)
public class MovieYearItemMapper extends ItemMapper {
    public static final String ID_PREFIX = "movies-";
    private static final List<String> CHILDREN = IntStream.rangeClosed(1, 12)
            .mapToObj(i -> StringUtils.leftPad(Integer.toString(i), 2, "0"))
            .collect(Collectors.toList());

    public MovieYearItemMapper() {
    }

    //Children of the /movies/{year} node are the months we want to display in browsing, current year is limited to current month
    @Override public List<String> listChildren(String path) {
        String node = PathHelper.getLeaf(path);
        if (node != null) {
            Calendar calendar = Calendar.getInstance();
            if (calendar.get(Calendar.YEAR) == Integer.parseInt(node)) {
                int limit = calendar.get(Calendar.MONTH);
                return IntStream.rangeClosed(1, limit)
                        .mapToObj(i -> StringUtils.leftPad(Integer.toString(i), 2, "0"))
                        .collect(Collectors.toList());
            } else {
                return CHILDREN;
            }
        }
        return Collections.emptyList();
    }

    @Override public ExternalData getData(String identifier) {
        final String year = StringUtils.substring(identifier, ID_PREFIX.length());
        Map<String, String[]> properties = new HashMap<>();
        properties.put(Constants.JCR_TITLE, new String[] { year });
        String path = new PathBuilder(MoviesItemMapper.PATH_LABEL).append(year).build();
        return new ExternalData(identifier, path, Naming.NodeType.CONTENT_FOLDER, properties);
    }

    @Override public String getIdFromPath(String path) {
        return ID_PREFIX.concat(PathHelper.getLeaf(path));
    }

    @Override public String getPathLabel() {
        return "";
    }
}
