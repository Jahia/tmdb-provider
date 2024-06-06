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
package org.jahia.modules.provider.tmdb.binding.movies;

import org.jahia.modules.external.ExternalData;
import org.jahia.modules.provider.tmdb.binding.NodeBinding;
import org.jahia.modules.provider.tmdb.data.CategoriesCollection;
import org.jahia.modules.provider.tmdb.data.YearsCollection;
import org.jahia.modules.provider.tmdb.helper.PathBuilder;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Binding for the /movies node.
 * Children of that path are movie's genres from TMDB
 *
 * @author Jerome Blanchard
 */
@Component(service = { MoviesNode.class, NodeBinding.class}, scope = ServiceScope.SINGLETON, immediate = true)
public class MoviesNode implements NodeBinding {

    private static final String PATH_PATTERN = "^/movies";
    private static final String ID_PATTERN = "^cid-movies$";
    @Reference
    private CategoriesCollection categories;
    @Reference
    private YearsCollection years;

    public MoviesNode() {
    }

    @Override
    public String getPathPattern() {
        return PATH_PATTERN;
    }

    @Override
    public String getIdPattern() {
        return ID_PATTERN;
    }

    @Override
    public String findNodeId(String path) {
        return CategoriesCollection.MOVIES_ID;
    }

    @Override
    public ExternalData getData(String identifier) {
        return categories.getData(identifier).toExternalData(new PathBuilder("movies").build());
    }

    @Override
    public List<ExternalData> listChildren(String path) {
        return years.list().stream()
                .map(g -> g.toExternalData(new PathBuilder(path).append(g.getName()).build()))
                .sorted(Comparator.comparing(ExternalData::getName).reversed())
                .collect(Collectors.toList());
    }

    @Override public String[] getProperty(String identifier, String lang, String propertyName) {
        return new String[0];
    }
}
