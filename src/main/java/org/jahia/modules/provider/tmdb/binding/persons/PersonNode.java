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
package org.jahia.modules.provider.tmdb.binding.persons;

import org.jahia.modules.external.ExternalData;
import org.jahia.modules.provider.tmdb.binding.NodeBinding;
import org.jahia.modules.provider.tmdb.data.CategoriesCollection;
import org.jahia.modules.provider.tmdb.data.PersonsCollection;
import org.jahia.modules.provider.tmdb.data.ProviderData;
import org.jahia.modules.provider.tmdb.helper.PathBuilder;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import java.util.Collections;
import java.util.List;

/**
 * Mapper for /persons/{person} node of the provider.
 *
 * @author Jerome Blanchard
 */
@Component(service = { PersonNode.class, NodeBinding.class}, scope = ServiceScope.SINGLETON, immediate = true)
public class PersonNode implements NodeBinding {

    private static final String PATH_PATTERN = "^/persons/\\d+$";
    private static final String ID_PATTERN = "^person-$";
    @Reference
    private PersonsCollection persons;

    public PersonNode() {
    }

    @Override
    public List<String> getSupportedNodeTypes() {
        return Collections.emptyList();
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
        return CategoriesCollection.PERSONS_ID;
    }

    @Override
    public ExternalData getData(String identifier) {
        ProviderData person = persons.getData(identifier);
        String path = new PathBuilder("persons").append(person.getId().substring(PersonsCollection.ID_PREFIX.length())).build();
        return person.toExternalData(path);
    }

    @Override
    public List<ExternalData> listChildren(String path) {
        return Collections.emptyList();
    }

    @Override public String[] getProperty(String identifier, String lang, String propertyName) {
        return new String[0];
    }
}
