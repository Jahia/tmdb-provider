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
package org.jahia.modules.provider.tmdb.binding;

import org.jahia.modules.external.ExternalData;
import org.jahia.modules.external.ExternalQuery;
import org.jahia.modules.provider.tmdb.data.CategoriesCollection;
import org.jahia.modules.provider.tmdb.data.ProviderData;
import org.jahia.modules.provider.tmdb.helper.PathBuilder;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import java.util.Collections;
import java.util.List;

/**
 * Binding of the root node.
 * Children are a fixed list of predefined content folder (organisational nodes)
 *
 * @author Jerome Blanchard
 */
@Component(service = { RootNode.class, NodeBinding.class}, scope = ServiceScope.SINGLETON, immediate = true)
public class RootNode implements NodeBinding {

    private static String PATH_PATTERN = "^/$";
    private static String ID_PATTERN = "^".concat(CategoriesCollection.ROOT_ID).concat("$");
    @Reference
    private CategoriesCollection categories;

    public RootNode() {
    }

    @Override
    public List<String> getSupportedNodeTypes() {
        return Collections.emptyList();
    }

    @Reference
    public void setCategories(CategoriesCollection categories) {
        this.categories = categories;
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
        return CategoriesCollection.ROOT_ID;
    }

    @Override
    public ExternalData getData(String identifier) {
        return categories.getData(identifier).toExternalData(new PathBuilder().build());
    }

    @Override
    public List<ExternalData> listChildren(String path) {
        return List.of(categories.getData(CategoriesCollection.MOVIES_ID).toExternalData("/movies"),
                categories.getData(CategoriesCollection.PERSONS_ID).toExternalData("/persons"),
                categories.getData(CategoriesCollection.GENRES_ID).toExternalData("/genres"));
    }
    @Override public String[] getProperty(String identifier, String lang, String propertyName) {
        return new String[0];
    }
}
