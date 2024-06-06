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
package org.jahia.modules.provider.tmdb.old.node;

/**
 * Load all ItemMapper and find the right one based on path or id
 *
 * @author Jerome Blanchard
 */
public class NodeHandlerProvider {

    /*
    private static final Logger LOGGER = LoggerFactory.getLogger(NodeHandlerProvider.class);
    private final Map<Class<? extends NodeMapperOld>, NodeMapperOld> mappers;
    private final Map<NodeMapping, Class<? extends NodeMapperOld>> descriptors;
    private boolean initialized = false;

    private static class ItemMapperRegistryHolder {
        private final static NodeHandlerProvider INSTANCE = new NodeHandlerProvider();
    }

    private NodeHandlerProvider() {
        mappers = new HashMap<>();
        descriptors = new HashMap<>();
    }

    public static NodeHandlerProvider getInstance() {
        return ItemMapperRegistryHolder.INSTANCE;
    }

    public synchronized NodeHandlerProvider initialize(Cache cache, TmdbApi apiClient) {
        LOGGER.debug("Initializing ItemMapperProvider");
        if (!initialized) {
            ServiceLoader<NodeMapperOld> loader = ServiceLoader.load(NodeMapperOld.class, this.getClass().getClassLoader());
            loader.forEach(n -> {
                NodeMapping descriptor = n.getClass().getAnnotation(NodeMapping.class);
                if (descriptor != null) {
                    descriptors.put(descriptor, n.getClass());
                    mappers.put(n.getClass(), n.withCache(cache).withApiClient(apiClient));
                    LOGGER.info("Registering ItemMapper: {}", n.getClass().getName());
                }
            });
            initialized = true;
        }
        return this;
    }

    public Optional<NodeMapperOld> findByPathLabel(String pathLabel) {
        return mappers.values().stream().filter(e -> e.getPathLabel().equals(pathLabel)).findFirst();
    }

    public Optional<NodeMapperOld> findByPath(String path) {
        PathHelper.ensureValidity(path);
        Optional<NodeMapping> descriptor = descriptors.keySet().stream().filter(e -> path.matches(e.pathPattern())).findFirst();
        return descriptor.map(i -> mappers.get(descriptors.get(i)));
    }

    public Optional<NodeMapperOld> findByPathForProps(String path) {
        PathHelper.ensureValidity(path);
        Optional<NodeMapping> descriptor =
                descriptors.keySet().stream().filter(e -> e.hasLazyProperties() && path.matches(e.pathPattern())).findFirst();
        return descriptor.map(i -> mappers.get(descriptors.get(i)));
    }

    public Optional<NodeMapperOld> findById(String id) {
        Optional<NodeMapping> descriptor = descriptors.keySet().stream().filter(e -> id.matches(e.idPattern())).findFirst();
        return descriptor.map(i -> mappers.get(descriptors.get(i)));
    }

    public List<NodeMapperOld> listForType(String type) {
        Set<NodeMapping> descriptors =
                this.descriptors.keySet().stream().filter(e -> Arrays.asList(e.supportedNodeType()).contains(type)).collect(Collectors.toSet());
        return descriptors.stream().map(i -> mappers.get(this.descriptors.get(i))).collect(Collectors.toList());
    }

     */
}
