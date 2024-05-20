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
public class MovieReferenceItemMapper extends ItemMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(MovieReferenceItemMapper.class);

    public MovieReferenceItemMapper() {
    }

    @Override public List<String> listChildren(String path) {
        return Collections.emptyList();
    }

    @Override public ExternalData getData(String identifier) {
        String cleanId = StringUtils.substring(identifier, ItemMapperDescriptor.MOVIE_REF.getIdPrefix().length());
        String listId = StringUtils.substringBefore(cleanId, "-");
        int movieId = Integer.parseInt(StringUtils.substringAfter(cleanId, "-"));
        Map<String, String[]> properties = new HashMap<>();
        properties.put("j:node", new String[] { ItemMapperDescriptor.MOVIE_ID.getIdPrefix() + movieId });
        String path = new PathBuilder(ItemMapperDescriptor.MOVIE_REF).append(listId).append(movieId).build();
        return new ExternalData(identifier, path, Naming.NodeType.CONTENT_REFERENCE, properties);
    }

    @Override public String getIdFromPath(String path) {
        return ItemMapperDescriptor.MOVIE_REF.getIdPrefix().concat(PathHelper.getLeaf(path));
    }
}
