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

import info.movito.themoviedbapi.model.core.Genre;
import net.sf.ehcache.Element;
import org.jahia.api.Constants;
import org.jahia.modules.provider.tmdb.TMDBCache;
import org.jahia.modules.provider.tmdb.TMDBClient;
import org.jahia.modules.provider.tmdb.helper.Naming;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Load the movie db genres
 *
 * @author Jerome Blanchard
 */
@Component(service = { GenresCollection.class, ProviderDataCollection.class}, scope = ServiceScope.SINGLETON, immediate = true)
public class GenresCollection implements ProviderDataCollection {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenresCollection.class);
    public static final String ID_PREFIX = "gid-";
    private static final String LIST_ID_CACHE_KEY = "genre-list-";

    @Reference
    private TMDBClient client;
    @Reference
    private TMDBCache cache;

    @Override
    public ProviderData getData(String identifier) {
        if (cache.get(identifier) == null) {
            try {
                String id = identifier.replace(ID_PREFIX, "");
                List<ProviderData> genres = list();
                return genres.stream().filter(genre -> genre.getId().equals(id)).findFirst().orElse(null);
            } catch (Exception e) {
                LOGGER.error("Error while fetching genres", e);
                return null;
            }
        } else {
            return (ProviderData) cache.get(identifier).getObjectValue();
        }
    }

    public List<ProviderData> list() {
        List<ProviderData> genres = new ArrayList<>();
        try {
            Element element = cache.get(LIST_ID_CACHE_KEY);
            if (element != null) {
                List<String> ids = (List<String>) element.getObjectValue();
                return ids.stream().map(this::getData).collect(Collectors.toList());
            } else {
                List<Genre> genreList = client.getGenre().getMovieList("");
                genres = genreList.stream().map(this::map).collect(Collectors.toList());
                genres.forEach(genre -> cache.put(new Element(genre.getId(), genre)));
                cache.put(new Element(LIST_ID_CACHE_KEY, genres.stream().map(ProviderData::getId).collect(Collectors.toList())));
            }
        } catch (Exception e) {
            LOGGER.error("Error while fetching genres", e);
        }
        return genres;
    }

    public ProviderData findByName(String name) {
        return list().stream().filter(genre -> genre.getName().equals(name)).findFirst().orElse(null);
    }

    protected ProviderData map(Genre genre) {
        Map<String, String[]> properties = new HashMap<>();
        properties.put(Constants.JCR_TITLE, new String[] { genre.getName() });
        String name = genre.getName().toLowerCase().replaceAll("[^a-z0-9]", "");
        return new ProviderData(ID_PREFIX + genre.getId(), Naming.NodeType.CONTENT_FOLDER, name, properties);
    }
}
