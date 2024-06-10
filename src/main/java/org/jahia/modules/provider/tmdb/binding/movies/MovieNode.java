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
package org.jahia.modules.provider.tmdb.binding.movies;

import info.movito.themoviedbapi.model.core.MovieResultsPage;
import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.external.ExternalQuery;
import org.jahia.modules.external.query.QueryHelper;
import org.jahia.modules.provider.tmdb.binding.NodeBinding;
import org.jahia.modules.provider.tmdb.data.CreditsCollection;
import org.jahia.modules.provider.tmdb.data.MoviesCollection;
import org.jahia.modules.provider.tmdb.data.ProviderData;
import org.jahia.modules.provider.tmdb.helper.Naming;
import org.jahia.modules.provider.tmdb.helper.PathBuilder;
import org.jahia.modules.provider.tmdb.helper.PathHelper;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Jerome Blanchard
 */
@Component(service = { MovieNode.class, NodeBinding.class}, scope = ServiceScope.SINGLETON, immediate = true)
public class MovieNode implements NodeBinding {

    private static final Logger LOGGER = LoggerFactory.getLogger(MovieNode.class);
    private static final String PATH_PATTERN = "^/movies/\\d{4}/\\d{2}/\\d+(?:/j:translation_[a-z]{2})?$";
    private static final String ID_PATTERN = "^movie-\\d+$";
    @Reference
    private MoviesCollection movies;
    @Reference
    private CreditsCollection credits;

    @Override
    public String getPathPattern() {
        return PATH_PATTERN;
    }

    @Override
    public String getIdPattern() {
        return ID_PATTERN;
    }

    @Override
    public List<String> getSupportedNodeTypes() {
        return List.of(Naming.NodeType.MOVIE);
    }

    @Override
    public String findNodeId(String path) {
        return MoviesCollection.ID_PREFIX.concat(PathHelper.getLeaf(path));
    }

    @Override
    public ExternalData getData(String identifier) {
        LOGGER.debug("Getting data for movie with identifier: {}", identifier);
        ProviderData data = movies.getData(identifier);
        String path = buildPath(data);
        return movies.getData(identifier).toExternalData(path, MoviesCollection.LAZY_PROPERTIES, MoviesCollection.LAZY_I18N_PROPERTIES);
    }

    @Override
    public List<ExternalData> listChildren(String path) {
        LOGGER.debug("Listing children for path: {}", path);
        if (PathHelper.getLeaf(path).startsWith("j:translation_")) {
            return Collections.emptyList();
        }
        List<ProviderData> child = credits.list(PathHelper.getLeaf(path));
        return child.stream()
                .map(data -> data.toExternalData(new PathBuilder(path).append(StringUtils.substringAfterLast(data.getId(), "-")).build()))
                .collect(Collectors.toList());
    }

    @Override
    public String[] getProperty(String identifier, String propertyName) {
        LOGGER.debug("Getting property {} for movie with identifier: {}", propertyName, identifier);
        return movies.getData(identifier, "en", true).getProperty(propertyName);
    }

    @Override
    public String[] getI18nProperty(String identifier, String lang, String propertyName) {
        LOGGER.debug("Getting property {} for movie with identifier: {} and language: {}", propertyName, identifier, lang);
        return movies.getData(identifier, lang, true).getProperty(lang, propertyName);
    }

    @Override
    public List<String> search(String nodeType, ExternalQuery query) {
        try {
            Map<String, Value> m = QueryHelper.getSimpleOrConstraints(query.getConstraint());
            String lang = QueryHelper.getLanguage(query.getConstraint());
            MovieResultsPage page;
            if (m.containsKey(Constants.JCR_TITLE)) {
                String title = m.get(Constants.JCR_TITLE).getString();
                return movies.searchByTitle(title, lang, query.getLimit()).stream().map(this::buildPath).collect(Collectors.toList());
            } else {
                return movies.discover(query.getOffset(), query.getLimit()).stream().map(this::buildPath).collect(Collectors.toList());
            }
        } catch (RepositoryException e) {
        }
        return Collections.emptyList();
    }

    private String buildPath(ProviderData data) {
        String[] dateParts = new String[] {"0000", "00"};
        if (data.getProperties().containsKey("release_date")) {
            dateParts = data.getProperties().get("release_date")[0].split("-");
        }
        String localid = data.getId().substring(MoviesCollection.ID_PREFIX.length());
        return new PathBuilder("movies").append(dateParts[0]).append(dateParts[1]).append(localid).build();
    }

    public static class MoviePathBuilder extends PathBuilder {

        @Override public String build() {
            return super.build();
        }
    }
}
