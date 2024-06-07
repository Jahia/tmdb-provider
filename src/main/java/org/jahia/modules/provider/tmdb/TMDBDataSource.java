package org.jahia.modules.provider.tmdb;

import org.jahia.exceptions.JahiaInitializationException;
import org.jahia.modules.external.*;
import org.jahia.modules.external.query.QueryHelper;
import org.jahia.modules.provider.tmdb.binding.NodeBinding;
import org.jahia.modules.provider.tmdb.helper.Naming;
import org.jahia.modules.provider.tmdb.helper.PathHelper;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Binary;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component(service = { ExternalDataSource.class, TMDBDataSource.class }, immediate = true,
        configurationPid = "org.jahia.modules.tmdbprovider")
@Designate(ocd = TMDBDataSourceConfig.class)
public class TMDBDataSource implements ExternalDataSource, ExternalDataSource.LazyProperty, ExternalDataSource.Searchable, ExternalDataSource.CanLoadChildrenInBatch {

    private static final Logger LOGGER = LoggerFactory.getLogger(TMDBDataSource.class);
    private static final List<String> EXTENDABLE_TYPES = List.of("nt:base");

    @Reference
    private ExternalContentStoreProviderFactory externalContentStoreProviderFactory;
    private ExternalContentStoreProvider externalContentStoreProvider;
    @Reference
    private TMDBMapperFactory mapperFactory;

    public TMDBDataSource() {
    }

    @Activate
    public void start(TMDBDataSourceConfig config) throws RepositoryException {
        LOGGER.info("Starting TMDBDataSource...");
        externalContentStoreProvider = externalContentStoreProviderFactory.newProvider();
        externalContentStoreProvider.setDataSource(this);
        externalContentStoreProvider.setExtendableTypes(EXTENDABLE_TYPES);
        externalContentStoreProvider.setMountPoint(config.mountPoint());
        externalContentStoreProvider.setKey("TMDBProvider");
        try {
            externalContentStoreProvider.start();
        } catch (JahiaInitializationException e) {
            throw new RepositoryException("Error initializing TMDB Provider", e);
        }
        LOGGER.info("TMDBDataSource started");
    }

    @Deactivate
    public void stop() {
        LOGGER.info("Stopping TMDBDataSource...");
        if (externalContentStoreProvider != null) {
            externalContentStoreProvider.stop();
        }
        LOGGER.info("TMDBDataSource stopped");
    }

    /**
     * @param path path where to get children
     * @return list of paths as String
     */
    @Override
    public List<String> getChildren(String path) throws RepositoryException {
        LOGGER.debug("getChildren for path: " + path);
        NodeBinding mapper = mapperFactory.findNodeBindingForPath(path);
        return mapper.listChildren(path).stream().map(ExternalData::getPath).map(PathHelper::getLeaf).collect(Collectors.toList());
    }

    @Override
    public List<ExternalData> getChildrenNodes(String path) throws RepositoryException {
        LOGGER.debug("getChildrenNodes for path: " + path);
        NodeBinding mapper = mapperFactory.findNodeBindingForPath(path);
        return mapper.listChildren(path);
    }

    /**
     * identifier is unique for an ExternalData
     *
     * @param identifier
     * @return ExternalData defined by the identifier
     * @throws javax.jcr.ItemNotFoundException
     */
    @Override
    public ExternalData getItemByIdentifier(String identifier) throws ItemNotFoundException {
        try {
            LOGGER.debug("getItemByIdentifier for identifier: " + identifier);
            NodeBinding mapper = mapperFactory.findNodeBindingForIdentifier(identifier);
            return mapper.getData(identifier);
        } catch (RepositoryException e) {
            throw new ItemNotFoundException("No item found for identifier: " + identifier, e);
        }
    }

    /**
     * As getItemByIdentifier, get an ExternalData by its path
     *
     * @param path
     * @return ExternalData
     * @throws javax.jcr.PathNotFoundException
     */
    @Override
    public ExternalData getItemByPath(String path) throws PathNotFoundException {
        try {
            LOGGER.debug("getItemByPath for path: " + path);
            NodeBinding mapper = mapperFactory.findNodeBindingForPath(path);
            String identifier = mapper.findNodeId(path);
            ExternalData data = mapper.getData(identifier);
            if (data == null) throw new PathNotFoundException("No item found for path: " + path);
            return data;
        } catch (RepositoryException e) {
            throw new PathNotFoundException("No item found for path: " + path, e);
        }
    }

    /**
     * Returns a set of supported node types.
     *
     * @return a set of supported node types
     */
    @Override public Set<String> getSupportedNodeTypes() {
        return Naming.NODE_TYPES;
    }

    /**
     * Indicates if this data source has path-like hierarchical external identifiers, e.g. IDs that are using file system paths.
     *
     * @return <code>true</code> if this data source has path-like hierarchical external identifiers, e.g. IDs that are using file system
     * paths; <code>false</code> otherwise.
     */
    @Override public boolean isSupportsHierarchicalIdentifiers() {
        return false;
    }

    /**
     * Indicates if the data source supports UUIDs.
     *
     * @return <code>true</code> if the data source supports UUIDs
     */
    @Override public boolean isSupportsUuid() {
        return false;
    }

    /**
     * Test existence of an Item with the specified path.
     *
     * @param path item path
     * @return <code>true</code> if an item exists at <code>path</code>; otherwise returns <code>false</code>
     */
    @Override public boolean itemExists(String path) {
        LOGGER.debug("itemExists for path: " + path);
        return false;
    }

    @Override public String[] getPropertyValues(String path, String propertyName) throws PathNotFoundException {
        LOGGER.debug("getPropertyValues for path: " + path + ", and property: " + propertyName);
        return getI18nPropertyValues(path, "en", propertyName);
    }

    @Override public String[] getI18nPropertyValues(String path, String lang, String propertyName) throws PathNotFoundException {
        try {
            LOGGER.info("getI18nPropertyValues for path: " + path);
            NodeBinding mapper = mapperFactory.findNodeBindingForPath(path);
            String identifier = mapper.findNodeId(path);
            return mapper.getProperty(identifier, lang, propertyName);
        } catch (RepositoryException e) {
            throw new PathNotFoundException("No item found for path: " + path, e);
        }
    }

    @Override public Binary[] getBinaryPropertyValues(String path, String propertyName) {
        return new Binary[0];
    }

    @Override public List<String> search(ExternalQuery query) throws RepositoryException {
        try {
            LOGGER.info("search for query: " + query);
            List<String> results = new ArrayList<>();
            String nodeType = QueryHelper.getNodeType(query.getSource());
            List<NodeBinding> bindings = mapperFactory.findNodeBindingForNodeType(nodeType);
            for (NodeBinding binding : bindings) {
                results.addAll(binding.search(nodeType, query));
            }
            return results;
        } catch (RepositoryException e) {
            throw new PathNotFoundException("Error while searching item for query " + query, e);
        }
    }
}
