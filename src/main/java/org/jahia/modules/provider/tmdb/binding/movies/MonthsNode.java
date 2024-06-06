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
import org.jahia.modules.provider.tmdb.data.MonthsCollection;
import org.jahia.modules.provider.tmdb.data.MoviesCollection;
import org.jahia.modules.provider.tmdb.data.ProviderData;
import org.jahia.modules.provider.tmdb.helper.PathBuilder;
import org.jahia.modules.provider.tmdb.helper.PathHelper;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Binding for the /movies/{year}/{month} nodes.
 * That nodes will list the alphabet letters we want to bind movies for in the browsing tree.
 *
 * @author Jerome Blanchard
 */
@Component(service = { MonthsNode.class, NodeBinding.class}, scope = ServiceScope.SINGLETON, immediate = true)
public class MonthsNode implements NodeBinding {

    private static final String PATH_PATTERN = "^/movies/\\d{4}/\\d{2}$";
    private static final String ID_PATTERN = "^month-\\d{4}-\\d{2}$";
    @Reference
    private MonthsCollection months;
    @Reference
    private MoviesCollection movies;

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
        return MonthsCollection.ID_PREFIX.concat(PathHelper.getParent(path).concat("-").concat(PathHelper.getLeaf(path)));
    }

    @Override
    public ExternalData getData(String identifier) {
        String[] id = identifier.substring(MonthsCollection.ID_PREFIX.length()).split("-");
        String path = new PathBuilder("movies").append(id[0]).append(id[1]).build();
        return months.getData(identifier).toExternalData(path);
    }

    @Override
    public List<ExternalData> listChildren(String path) {
        List<ProviderData> child = movies.list(PathHelper.getParent(path), PathHelper.getLeaf(path), "fr");
        return child.stream()
                .map(data -> data.toExternalData(new PathBuilder(path).append(data.getId()).build(), MoviesCollection.LAZY_PROPERTIES,
                        MoviesCollection.LAZY_I18N_PROPERTIES))
                .collect(Collectors.toList());
    }

    @Override public String[] getProperty(String identifier, String lang, String propertyName) {
        return new String[0];
    }

}
