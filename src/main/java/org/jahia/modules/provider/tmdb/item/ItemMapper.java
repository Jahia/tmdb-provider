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

import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.model.configuration.Configuration;
import info.movito.themoviedbapi.tools.TmdbException;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.external.ExternalQuery;
import org.jahia.modules.provider.tmdb.helper.Naming;

import javax.jcr.RepositoryException;
import java.util.Collections;
import java.util.List;

/**
 * Abstract class that define a ItemMapper to take part in the Data Provider.
 * TODO: Make the node hierarchy in the contract by adding a parent reference to an Item Mapper allowing the Path generation of a node to
 * be automatically generated. (may be a pb if many path can link the same identifier)
 *
 * @author Jerome Blanchard
 */
public abstract class ItemMapper {
    private TmdbApi client;
    private Cache cache;
    private Configuration configuration;

    public ItemMapper withApiClient(TmdbApi client) {
        this.client = client;
        return this;
    }

    public ItemMapper withCache(Cache cache) {
        this.cache = cache;
        return this;
    }

    public TmdbApi getApiClient() {
        return client;
    }

    public Cache getCache() {
        return cache;
    }

    abstract public List<String> listChildren(String path);

    abstract public ExternalData getData(String identifier);

    abstract public String getIdFromPath(String path);

    abstract public String getPathLabel();

    public List<String> search(String nodeType, ExternalQuery query) throws RepositoryException {
        return Collections.emptyList();
    }

    public String[] getProperty(String identifier, String lang, String propertyName) {
        return new String[] {};
    }

    public synchronized Configuration getConfiguration() throws TmdbException {
        if ( configuration == null ) {
            configuration = getApiClient().getConfiguration().getDetails();
        }
        return configuration;
    }
}
