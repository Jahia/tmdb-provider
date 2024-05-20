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

import info.movito.themoviedbapi.model.core.MovieResultsPage;
import info.movito.themoviedbapi.tools.builders.discover.DiscoverMovieParamBuilder;
import net.sf.ehcache.Element;
import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.provider.tmdb.helper.Naming;
import org.jahia.modules.provider.tmdb.helper.PathBuilder;
import org.jahia.modules.provider.tmdb.helper.PathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handler for movies node.
 * List
 *
 * @author Jerome Blanchard
 */
public class MovieMonthItemMapper extends ItemMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(MovieMonthItemMapper.class);

    public MovieMonthItemMapper() {
    }

    @Override public List<String> listChildren(String path) {
        String node = PathHelper.getLeaf(path);
        //TODO Maybe the cache name is not the same than for the cache key prefix
        if (getCache().get(Naming.Cache.MOVIES_FOLDER_CACHE_PREFIX + node) != null) {
            return (List<String>) getCache().get(Naming.Cache.MOVIES_FOLDER_CACHE_PREFIX + node).getObjectValue();
        } else {
            Calendar instance = Calendar.getInstance();
            instance.set(Calendar.YEAR, Integer.parseInt(node.split("-")[0]));
            instance.set(Calendar.MONTH, Integer.parseInt(node.split("-")[1]));
            instance.set(Calendar.DAY_OF_MONTH, 1);
            instance.roll(Calendar.DAY_OF_MONTH, false);
            DiscoverMovieParamBuilder builder = new DiscoverMovieParamBuilder()
                    .primaryReleaseDateGte(node.concat("-01"))
                    .primaryReleaseDateLte(node.concat("-").concat(Integer.toString(instance.get(Calendar.DAY_OF_MONTH))))
                    .page(1);
            List<String> children = new ArrayList<>();
            try {
                MovieResultsPage page;
                do {
                    page = getApiClient().getDiscover().getMovie(builder);
                    children.addAll(page.getResults().stream()
                            .map(m -> Integer.toString(m.getId()))
                            .collect(Collectors.toList()));
                    builder.page(page.getPage() + 1);
                } while (page.getPage() < page.getTotalPages());
            } catch (Exception e) {
                LOGGER.warn("Error while getting movies ", e);
            }
            getCache().put(new Element(Naming.Cache.MOVIES_FOLDER_CACHE_PREFIX + node, children));
            return children;
        }
    }

    @Override public ExternalData getData(String identifier) {
        final String date = StringUtils.substring(identifier, ItemMapperDescriptor.MOVIE_MONTH.getIdPrefix().length());
        final String year = date.split("-")[0];
        Map<String, String[]> properties = new HashMap<>();
        properties.put(Constants.JCR_TITLE, new String[] { date });
        String path = new PathBuilder(ItemMapperDescriptor.MOVIE_REF).append(year).append(date).build();
        return new ExternalData(identifier, path, Naming.NodeType.CONTENT_FOLDER, properties);
    }

    @Override public String getIdFromPath(String path) {
        return ItemMapperDescriptor.MOVIE_MONTH.getIdPrefix().concat(PathHelper.getLeaf(path));
    }
}
