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

import java.util.Collections;
import java.util.List;

/**
 * @author Jerome Blanchard
 */
@Component(service = { MovieNode.class, NodeBinding.class}, scope = ServiceScope.SINGLETON, immediate = true)
public class MovieNode implements NodeBinding {

    private static final String PATH_PATTERN = "^/movies/\\d{4}/\\d{2}/\\d+$";
    private static final String ID_PATTERN = "^movie-\\d+$";
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
        return MoviesCollection.ID_PREFIX.concat(PathHelper.getLeaf(path));
    }

    @Override
    public ExternalData getData(String identifier) {
        String id = identifier.substring(MonthsCollection.ID_PREFIX.length());
        ProviderData data = movies.getData(identifier);
        String year = "0000";
        String month = "00";
        if (data.getProperties().containsKey("release_date")) {
            String [] dateParts = data.getProperties().get("release_date")[0].split("-");
            year = dateParts[0];
            month = dateParts[1];
        }
        String path = new PathBuilder("movies").append(year).append(month).append(id).build();
        return movies.getData(identifier).toExternalData(path, MoviesCollection.LAZY_PROPERTIES, MoviesCollection.LAZY_I18N_PROPERTIES);
    }

    @Override
    public List<ExternalData> listChildren(String path) {
        //TODO Return credits
        return Collections.emptyList();
    }

    @Override public String[] getProperty(String identifier, String lang, String propertyName) {
        //TODO return props
        return new String[0];
    }

}
