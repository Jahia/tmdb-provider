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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import org.jahia.services.cache.CacheProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jerome Blanchard
 */
@Component(service = TMDBCache.class, scope = ServiceScope.SINGLETON, immediate = true)
public class TMDBCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(TMDBCache.class);
    private static final String CACHE_NAME = "tmdb";

    @Reference
    private CacheProvider cacheProvider;
    private Cache cache;

    @Activate
    public void start() {
        LOGGER.info("Starting TMDB Cache...");
        try {
            if (!cacheProvider.getCacheManager().cacheExists(CACHE_NAME)) {
                cacheProvider.getCacheManager().addCache(CACHE_NAME);
            }
            cacheProvider.getCacheManager().getCache(CACHE_NAME).flush();
            cache = cacheProvider.getCacheManager().getCache(CACHE_NAME);
        } catch (IllegalStateException | CacheException e) {
            LOGGER.error("Error while initializing cache for TMDB", e);
        }
        LOGGER.info("TMDB Cache started");
    }

    public void put(Element element) {
        cache.put(element);
    }

    public Element get(Object key) {
        return cache.get(key);
    }
}
