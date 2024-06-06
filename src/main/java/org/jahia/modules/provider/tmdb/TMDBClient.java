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
package org.jahia.modules.provider.tmdb;

import info.movito.themoviedbapi.*;
import info.movito.themoviedbapi.model.configuration.Configuration;
import info.movito.themoviedbapi.tools.TmdbException;
import net.sf.ehcache.Element;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.provider.tmdb.client.TMDBApacheUrlReader;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Façade over the TMDB API client only exposing small parts. Goal is to make the underlying client switchable.
 *
 * @author Jerome Blanchard
 */
@Component(service = TMDBClient.class, immediate = true, scope = ServiceScope.SINGLETON,
        configurationPid = "org.jahia.modules.tmdbprovider")
public class TMDBClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(TMDBClient.class);
    private static final String TMDB_CONFIG_CACHE_KEY = "configuration";
    @Reference
    private TMDBCache cache;
    private TmdbApi client;

    @Activate
    public void start(TMDBDataSourceConfig config) {
        LOGGER.info("Starting TMDBClient...");
        if (StringUtils.isEmpty(config.apiKey())) {
            LOGGER.warn("API key is not set, TMDB client will not initialize.");
            return;
        }
        this.client = new TmdbApi(new TMDBApacheUrlReader(config.apiKey()));
        //TODO Test connectivity
        LOGGER.info("TMDB client started.");
    }

    @Deactivate
    public void stop() {
        LOGGER.info("Stopping TMDBClient...");
        if (this.client != null) {
            this.client = null;
        }
        LOGGER.info("TMDBClient stopped");
    }

    public Configuration getConfiguration() throws TmdbException {
        if (cache.get(TMDB_CONFIG_CACHE_KEY) == null) {
            cache.put(new Element(TMDB_CONFIG_CACHE_KEY, client.getConfiguration().getDetails()));
        }
        return (Configuration) cache.get(TMDB_CONFIG_CACHE_KEY).getObjectValue();
    }

    public TmdbDiscover getDiscover() {
        return client.getDiscover();
    }

    public TmdbGenre getGenre() {
        return client.getGenre();
    }

    public TmdbKeywords getKeywords() {
        return client.getKeywords();
    }

    public TmdbMovies getMovies() {
        return client.getMovies();
    }

    public TmdbPeople getPeople() {
        return client.getPeople();
    }

    public TmdbSearch getSearch() {
        return client.getSearch();
    }
}
