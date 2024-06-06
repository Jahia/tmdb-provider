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
package org.jahia.modules.provider.tmdb.data;

import org.jahia.api.Constants;
import org.jahia.modules.provider.tmdb.helper.Naming;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provide simple data (contentFolder) for top level organisation of the DataSource tree
 * All the items are static.
 *
 * @author Jerome Blanchard
 */
@Component(service = { CategoriesCollection.class, ProviderDataCollection.class}, scope = ServiceScope.SINGLETON, immediate = true)
public class CategoriesCollection implements ProviderDataCollection {

    public static final String ROOT_ID = "cid-root";
    public static final String MOVIES_ID = "cid-movies";
    public static final String PERSONS_ID = "cid-persons";
    public static final String GENRES_ID = "cid-genres";

    private static final List<ProviderData> items = new ArrayList<>();
    static {
        Map<String, String[]> properties = new HashMap<>();
        properties.put(Constants.JCR_TITLE, new String[] { "TMDB" });
        items.add(new ProviderData(ROOT_ID, Naming.NodeType.CONTENT_FOLDER, "", properties));
        properties = new HashMap<>();
        properties.put(Constants.JCR_TITLE, new String[] { "Movies" });
        items.add(new ProviderData(MOVIES_ID, Naming.NodeType.CONTENT_FOLDER, "movies", properties));
        properties = new HashMap<>();
        properties.put(Constants.JCR_TITLE, new String[] { "Persons" });
        items.add(new ProviderData(PERSONS_ID, Naming.NodeType.CONTENT_FOLDER, "persons", properties));
        properties = new HashMap<>();
        properties.put(Constants.JCR_TITLE, new String[] { "Genres" });
        items.add(new ProviderData(GENRES_ID, Naming.NodeType.CONTENT_FOLDER, "genres", properties));
    }

    @Override
    public ProviderData getData(String identifier) {
        return items.stream().filter(i -> identifier.equals(i.getId())).findFirst().orElse(null);
    }

}
