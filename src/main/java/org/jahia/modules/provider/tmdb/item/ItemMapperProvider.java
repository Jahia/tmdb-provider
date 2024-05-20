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
import net.sf.ehcache.Cache;
import org.jahia.modules.provider.tmdb.helper.PathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Load all ItemMapper.
 *
 * @author Jerome Blanchard
 */
public class ItemMapperProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemMapperProvider.class);
    private Map<Class<? extends ItemMapper>, ItemMapper> mappers;
    private Map<ItemMapperDescriptor, Class<? extends ItemMapper>> descriptors;
    private boolean initiliazed = false;

    private static class ItemMapperRegistryHolder {
        private final static ItemMapperProvider INSTANCE = new ItemMapperProvider();
    }

    private ItemMapperProvider() {
        mappers = new HashMap<>();
        descriptors = new HashMap<>();
    }

    public static ItemMapperProvider getInstance() {
        return ItemMapperRegistryHolder.INSTANCE;
    }

    public synchronized ItemMapperProvider initialize(Cache cache, TmdbApi apiClient) {
        LOGGER.info("Initializing ItemMapperProvider");
        if (!initiliazed) {
            ServiceLoader<ItemMapper> loader = ServiceLoader.load(ItemMapper.class);
            loader.forEach(n -> {
                ItemMapperDescriptor descriptor = n.getClass().getAnnotation(ItemMapperDescriptor.class);
                if (descriptor != null) {
                    descriptors.put(descriptor, n.getClass());
                    mappers.put(n.getClass(), n.withCache(cache).withApiClient(apiClient));
                    LOGGER.info("Registering ItemMapper: {}", n.getClass().getName());
                }
            });
            initiliazed = true;
        }
        return this;
    }

    public Optional<ItemMapper> findByPath(String path) {
        PathHelper.ensureValidity(path);
        Optional<ItemMapperDescriptor> descriptor = descriptors.keySet().stream().filter(e -> path.matches(e.pathPattern())).findFirst();
        return descriptor.map(i -> mappers.get(descriptors.get(i)));
    }

    public Optional<ItemMapper> findByPathForProps(String path) {
        PathHelper.ensureValidity(path);
        Optional<ItemMapperDescriptor> descriptor =
                descriptors.keySet().stream().filter(e -> e.hasLazyProperties() && path.matches(e.pathPattern())).findFirst();
        return descriptor.map(i -> mappers.get(descriptors.get(i)));
    }

    public Optional<ItemMapper> findById(String id) {
        Optional<ItemMapperDescriptor> descriptor = descriptors.keySet().stream().filter(e -> id.matches(e.idPattern())).findFirst();
        return descriptor.map(i -> mappers.get(descriptors.get(i)));
    }

    public List<ItemMapper> listForType(String type) {
        Set<ItemMapperDescriptor> descriptors =
                this.descriptors.keySet().stream().filter(e -> Arrays.asList(e.supportedNodeType()).contains(type)).collect(Collectors.toSet());
        return descriptors.stream().map(i -> mappers.get(this.descriptors.get(i))).collect(Collectors.toList());
    }
}
