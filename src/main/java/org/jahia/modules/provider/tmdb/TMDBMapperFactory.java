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

import org.jahia.modules.provider.tmdb.binding.NodeBinding;
import org.jahia.modules.provider.tmdb.binding.RootNode;
import org.jahia.modules.provider.tmdb.binding.genre.GenreNode;
import org.jahia.modules.provider.tmdb.binding.genre.GenresNode;
import org.jahia.modules.provider.tmdb.binding.movies.*;
import org.jahia.modules.provider.tmdb.binding.persons.PersonNode;
import org.jahia.modules.provider.tmdb.binding.persons.PersonsNode;
import org.jahia.modules.provider.tmdb.data.CreditsCollection;
import org.jahia.modules.provider.tmdb.data.MoviesCollection;
import org.jahia.modules.provider.tmdb.data.ProviderDataCollection;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.swing.text.html.Option;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Jerome Blanchard
 */
@Component(service = TMDBMapperFactory.class, immediate = true, scope = ServiceScope.SINGLETON)
public class TMDBMapperFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(TMDBMapperFactory.class);
    private final List<NodeBinding> nodeBindings = new ArrayList<>();

    @Reference
    private RootNode rootNode;
    @Reference
    private MoviesNode moviesNode;
    @Reference
    private YearsNode yearsNode;
    @Reference
    private MonthsNode monthNode;
    @Reference
    private MovieNode movieNode;
    @Reference
    private CreditsNode creditsNode;
    @Reference
    private PersonsNode personsNode;
    @Reference
    private PersonNode personNode;
    @Reference
    private GenresNode genresNode;
    @Reference
    private GenreNode genreNode;

    @Activate
    public void start() {
        LOGGER.info("Starting TMDBMapperFactory...");
        nodeBindings.add(rootNode);
        nodeBindings.add(moviesNode);
        nodeBindings.add(yearsNode);
        nodeBindings.add(monthNode);
        nodeBindings.add(movieNode);
        nodeBindings.add(creditsNode);
        nodeBindings.add(personsNode);
        nodeBindings.add(personNode);
        nodeBindings.add(genresNode);
        nodeBindings.add(genreNode);
        LOGGER.info("TMDBMapperFactory started with {} node binding", nodeBindings.size());
    }

    public NodeBinding findNodeBindingForPath(String path) throws RepositoryException {
        return nodeBindings.stream().filter(b -> path.matches(b.getPathPattern())).findFirst()
                .orElseThrow(() -> new RepositoryException("Unable to find Node Binding for path: " + path));
    }

    public NodeBinding findNodeBindingForIdentifier(String identifier) throws RepositoryException {
        return nodeBindings.stream().filter(c -> identifier.matches(c.getIdPattern())).findFirst()
                .orElseThrow(() -> new RepositoryException("Unable to find Node Binding for identifier: " + identifier));
    }

    public List<NodeBinding> findNodeBindingForNodeType(String type) {
        return nodeBindings.stream().filter(c -> c.getSupportedNodeTypes().contains(type)).collect(Collectors.toList());
    }

}
