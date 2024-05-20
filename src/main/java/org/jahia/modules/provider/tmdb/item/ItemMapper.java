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
 * @author Jerome Blanchard
 */
public abstract class ItemMapper {
    private TmdbApi client;
    private Cache cache;

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

    public List<String> search(String nodeType, ExternalQuery query) throws RepositoryException {
        return Collections.emptyList();
    }

    public String[] getProperty(String identifier, String lang, String propertyName) {
        return new String[] {};
    }

    public Configuration getConfiguration() throws TmdbException {
        Configuration configuration;
        if (getCache().get(Naming.Cache.CONFIGURATION_CACHE_KEY) != null) {
            configuration = (Configuration) getCache().get(Naming.Cache.CONFIGURATION_CACHE_KEY).getObjectValue();
        } else {
            configuration = getApiClient().getConfiguration().getDetails();
            getCache().put(new Element(Naming.Cache.CONFIGURATION_CACHE_KEY, configuration));
        }
        return configuration;
    }



}
