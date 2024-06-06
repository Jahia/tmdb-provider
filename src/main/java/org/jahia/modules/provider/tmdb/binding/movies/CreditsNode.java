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

import org.apache.commons.lang.StringUtils;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.provider.tmdb.binding.NodeBinding;
import org.jahia.modules.provider.tmdb.data.CreditsCollection;
import org.jahia.modules.provider.tmdb.data.MoviesCollection;
import org.jahia.modules.provider.tmdb.data.ProviderData;
import org.jahia.modules.provider.tmdb.helper.Naming;
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
@Component(service = { CreditsNode.class, NodeBinding.class}, scope = ServiceScope.SINGLETON, immediate = true)
public class CreditsNode implements NodeBinding {

    private static final String PATH_PATTERN = "^/movies/\\d{4}/\\d{2}/\\d+/(cast_|crew_)\\d+$";
    private static final String ID_PATTERN = "^credits-\\d+-(cast_|crew_)\\d+$";
    @Reference
    private CreditsCollection credits;
    @Reference
    private MoviesCollection movies;

    @Override
    public List<String> getSupportedNodeTypes() {
        return List.of(Naming.NodeType.CREW, Naming.NodeType.CAST);
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
        return CreditsCollection.ID_PREFIX.concat(PathHelper.getParent(path)).concat("-").concat(PathHelper.getLeaf(path));
    }

    @Override
    public ExternalData getData(String identifier) {
        ProviderData credit = credits.getData(identifier);
        String id = identifier.substring(CreditsCollection.ID_PREFIX.length());
        String movieId = StringUtils.substringBefore("-", id);
        String creditsId = StringUtils.substringAfter("-", id);
        ProviderData data = movies.getData(identifier);
        String year = "0000";
        String month = "00";
        if (data.getProperties().containsKey("release_date")) {
            String [] dateParts = data.getProperties().get("release_date")[0].split("-");
            year = dateParts[0];
            month = dateParts[1];
        }
        String path = new PathBuilder("movies").append(year).append(month).append(movieId).append(creditsId).build();
        return credit.toExternalData(path);
    }

    @Override
    public List<ExternalData> listChildren(String path) {
        return Collections.emptyList();
    }

    @Override
    public String[] getProperty(String identifier, String lang, String propertyName) {
        return new String[0];
    }

}
