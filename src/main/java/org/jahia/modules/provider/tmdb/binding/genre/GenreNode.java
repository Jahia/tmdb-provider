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
package org.jahia.modules.provider.tmdb.binding.genre;

import org.jahia.modules.external.ExternalData;
import org.jahia.modules.provider.tmdb.binding.NodeBinding;
import org.jahia.modules.provider.tmdb.data.GenresCollection;
import org.jahia.modules.provider.tmdb.data.YearsCollection;
import org.jahia.modules.provider.tmdb.helper.PathBuilder;
import org.jahia.modules.provider.tmdb.helper.PathHelper;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import java.util.Collections;
import java.util.List;

/**
 * Binding for the /genres/{gid} node
 * That node will load the detail of a TMDB movies genre
 *
 * @author Jerome Blanchard
 */
@Component(service = { GenreNode.class, NodeBinding.class}, scope = ServiceScope.SINGLETON, immediate = true)
public class GenreNode implements NodeBinding {

    private static final String PATH_PATTERN = "^/genres/[a-z0-9]+$";
    private static final String ID_PATTERN = "^genre-\\d$";

    @Reference
    private GenresCollection genres;

    public GenreNode() {
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
        return GenresCollection.ID_PREFIX.concat(PathHelper.getLeaf(path));
    }
    @Override
    public ExternalData getData(String identifier) {
        String path = new PathBuilder("genres").append(identifier.substring(GenresCollection.ID_PREFIX.length())).build();
        return genres.getData(identifier).toExternalData(path);
    }

    @Override
    public List<ExternalData> listChildren(String path) {
        return Collections.emptyList();
    }

    @Override public String[] getProperty(String identifier, String lang, String propertyName) {
        return new String[0];
    }
}
