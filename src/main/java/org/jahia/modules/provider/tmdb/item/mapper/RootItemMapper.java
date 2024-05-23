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

import org.apache.poi.sl.draw.geom.GuideIf;
import org.jahia.api.Constants;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.provider.tmdb.helper.Naming;
import org.jahia.modules.provider.tmdb.helper.PathBuilder;
import org.jahia.modules.provider.tmdb.helper.PathHelper;
import org.jahia.modules.provider.tmdb.item.ItemMapper;
import org.jahia.modules.provider.tmdb.item.ItemMapperDescriptor;
import org.jahia.modules.provider.tmdb.item.ItemMapperProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handler for root node of the provider.
 * List predefined children based on root collections exposed
 *
 * @author Jerome Blanchard
 */
@ItemMapperDescriptor(pathPattern = "^/$", idPattern = "^root$", supportedNodeType = {Naming.NodeType.CONTENT_FOLDER},
        hasLazyProperties = false)
public class RootItemMapper extends ItemMapper {

    private static final List<String> CHILDREN = Arrays.asList(MoviesItemMapper.PATH_LABEL, PersonsItemMapper.PATH_LABEL);
    private static ExternalData NODE;
    {
        Map<String, String[]> properties = new HashMap<>();
        properties.put(Constants.JCR_TITLE, new String[] { "TMDB" });
        String path = new PathBuilder().build();
        NODE = new ExternalData(ID_PREFIX, path, Naming.NodeType.CONTENT_FOLDER, properties);
    }
    public static final String ID_PREFIX = "root";

    public RootItemMapper() {
    }

    @Override public List<String> listChildren(String path) {
        return CHILDREN;
    }

    @Override
    public List<ExternalData> listChildrenNodes(String path) {
        ItemMapperProvider provider = ItemMapperProvider.getInstance();
        return CHILDREN.stream()
            .map(this::buildChildrenPath)
            .map(childPath -> provider.findByPath(childPath)
                .map(mapper -> mapper.getData(mapper.getIdFromPath(childPath))))
            .flatMap(Optional::stream)
            .collect(Collectors.toList());
    }

    @Override public ExternalData getData(String identifier) {
        return NODE;
    }

    @Override public String getIdFromPath(String path) {
        return ID_PREFIX;
    }

    @Override public String getPathLabel() {
        return "";
    }

    private String buildChildrenPath(String child) {
        return new PathBuilder().append(child).build();
    }
}
