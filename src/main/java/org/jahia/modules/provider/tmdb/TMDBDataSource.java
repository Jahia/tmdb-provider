package org.jahia.modules.provider.tmdb;

import info.movito.themoviedbapi.TmdbApi;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import org.apache.commons.lang.StringUtils;
import org.jahia.exceptions.JahiaInitializationException;
import org.jahia.modules.external.*;
import org.jahia.modules.external.query.QueryHelper;
import org.jahia.modules.provider.tmdb.helper.Naming;
import org.jahia.modules.provider.tmdb.http.TmdbApacheHttpClient;
import org.jahia.modules.provider.tmdb.item.ItemMapper;
import org.jahia.modules.provider.tmdb.item.ItemMapperDescriptor;
import org.jahia.services.cache.CacheProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import java.util.*;

@Component(service = { ExternalDataSource.class, TMDBDataSource.class }, immediate = true, configurationPid = "org.jahia.modules"
        + ".tmdbprovider") @Designate(ocd = TMDBDataSource.Config.class)
public class TMDBDataSource implements ExternalDataSource, ExternalDataSource.LazyProperty, ExternalDataSource.Searchable {

    @ObjectClassDefinition(name = "TMDB Provider", description = "A TMDB Provider configuration")
    public @interface Config {
        @AttributeDefinition(name = "TMDB API key", defaultValue = "", description = "The API key to use for The Movie Database") String apiKey() default "";
        @AttributeDefinition(name = "TMDB Mount path", defaultValue = "/sites/systemsite/contents/tmdb", description = "The path at which "
                + "to mount the database in the JCR") String mountPoint() default "/sites/systemsite/contents/tmdb";
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(TMDBDataSource.class);
    private static final List<String> EXTENDABLE_TYPES = List.of("nt:base");

    private CacheProvider cacheProvider;
    private Cache cache;
    private TmdbApi apiClient;
    private ExternalContentStoreProviderFactory externalContentStoreProviderFactory;
    private ExternalContentStoreProvider externalContentStoreProvider;

    public TMDBDataSource() {
    }

    @Reference
    public void setCacheProvider(CacheProvider cacheProvider) {
        this.cacheProvider = cacheProvider;
    }

    @Reference
    public void setExternalContentStoreProviderFactory(ExternalContentStoreProviderFactory externalContentStoreProviderFactory) {
        this.externalContentStoreProviderFactory = externalContentStoreProviderFactory;
    }

    @Activate
    public void start(Config config) throws RepositoryException {
        if (StringUtils.isEmpty(config.apiKey())) {
            LOGGER.warn("API key is not set, TMDB provider will not initialize.");
            return;
        }
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

        try {
            if (!cacheProvider.getCacheManager().cacheExists(Naming.Cache.TMDB_CACHE)) {
                cacheProvider.getCacheManager().addCache(Naming.Cache.TMDB_CACHE);
            }
            cache = cacheProvider.getCacheManager().getCache(Naming.Cache.TMDB_CACHE);
        } catch (IllegalStateException | CacheException e) {
            LOGGER.error("Error while initializing cache for IMDB", e);
        }

        apiClient = new TmdbApi(new TmdbApacheHttpClient(config.apiKey()));
    }

    @Deactivate
    public void stop() {
        if (apiClient != null) {
            apiClient = null;
        }
        if (externalContentStoreProvider != null) {
            externalContentStoreProvider.stop();
        }
    }

    /**
     * @param path path where to get children
     * @return list of paths as String
     */
    @Override
    public List<String> getChildren(String path) throws RepositoryException {
        Optional<ItemMapperDescriptor> itemMapper = ItemMapperDescriptor.findByPath(path);
        return getMapperInstance(itemMapper.orElse(ItemMapperDescriptor.ERROR)).listChildren(path);
    }

    /**
     * identifier is unique for an ExternalData
     *
     * @param identifier
     * @return ExternalData defined by the identifier
     * @throws javax.jcr.ItemNotFoundException
     */
    @Override public ExternalData getItemByIdentifier(String identifier) throws ItemNotFoundException {
        try {
            Optional<ItemMapperDescriptor> itemMapper = ItemMapperDescriptor.findById(identifier);
            ExternalData data = getMapperInstance(itemMapper.orElse(ItemMapperDescriptor.ERROR)).getData(identifier);
            if (data == null) throw new ItemNotFoundException("No item found for identifier: " + identifier);
            return data;
        } catch (RepositoryException e) {
            throw new ItemNotFoundException("Error while getting item for identifier " + identifier, e);
        }
    }

    /**
     * As getItemByIdentifier, get an ExternalData by its path
     *
     * @param path
     * @return ExternalData
     * @throws javax.jcr.PathNotFoundException
     */
    @Override public ExternalData getItemByPath(String path) throws PathNotFoundException {
        try {
            Optional<ItemMapperDescriptor> itemMapper = ItemMapperDescriptor.findByPath(path);
            ItemMapper mapper = getMapperInstance(itemMapper.orElse(ItemMapperDescriptor.ERROR));
            ExternalData data = mapper.getData(mapper.getIdFromPath(path));
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
     * Returns <code>true</code> if an item exists at <code>path</code>; otherwise returns <code>false</code>.
     *
     * @param path item path
     * @return <code>true</code> if an item exists at <code>path</code>; otherwise returns <code>false</code>
     */
    @Override public boolean itemExists(String path) {
        return false;
    }

    @Override public String[] getPropertyValues(String path, String propertyName) throws PathNotFoundException {
        return getI18nPropertyValues(path, "en", propertyName);
    }

    @Override public String[] getI18nPropertyValues(String path, String lang, String propertyName) throws PathNotFoundException {
        try {
            Optional<ItemMapperDescriptor> itemMapper = ItemMapperDescriptor.findByPathForProps(path);
            ItemMapper mapper = getMapperInstance(itemMapper.orElse(ItemMapperDescriptor.ERROR));
            return mapper.getProperty(mapper.getIdFromPath(path), lang, propertyName);
        } catch (RepositoryException e) {
            throw new PathNotFoundException("No item found for path: " + path, e);
        }
    }

    @Override public Binary[] getBinaryPropertyValues(String path, String propertyName) {
        return new Binary[0];
    }

    @Override public List<String> search(ExternalQuery query) throws RepositoryException {
        try {
            List<String> results = new ArrayList<>();
            String nodeType = QueryHelper.getNodeType(query.getSource());
            List<ItemMapperDescriptor> itemMappers = ItemMapperDescriptor.findByType(nodeType);
            for (ItemMapperDescriptor mapper : itemMappers) {
                results.addAll(getMapperInstance(mapper).search(nodeType, query));
            }
            return results;
        } catch (RepositoryException e) {
            throw new PathNotFoundException("Error while searching item for query " + query, e);
        }
    }

    private ItemMapper getMapperInstance(ItemMapperDescriptor mapper) throws RepositoryException {
        try {
            return Class.forName(mapper.getMapperClass().getName())
                        .asSubclass(ItemMapper.class).getDeclaredConstructor().newInstance()
                        .withCache(cache).withApiClient(apiClient);
        } catch (ReflectiveOperationException e) {
            throw new RepositoryException("Error while building mapper instance for mapper: " + mapper, e);
        }
    }
}
