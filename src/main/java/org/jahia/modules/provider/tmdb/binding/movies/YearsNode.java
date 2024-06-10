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

import org.apache.commons.lang.StringUtils;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.provider.tmdb.binding.NodeBinding;
import org.jahia.modules.provider.tmdb.data.MonthsCollection;
import org.jahia.modules.provider.tmdb.data.YearsCollection;
import org.jahia.modules.provider.tmdb.helper.PathBuilder;
import org.jahia.modules.provider.tmdb.helper.PathHelper;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Binding for the /movies/{year} nodes.
 * That nodes will list the months we want to bind movies for in the browsing tree.
 *
 * @author Jerome Blanchard
 */
@Component(service = { YearsNode.class, NodeBinding.class}, scope = ServiceScope.SINGLETON, immediate = true)
public class YearsNode implements NodeBinding {

    private static final String PATH_PATTERN = "^/movies/\\d{4}$";
    private static final String ID_PATTERN = "^" + YearsCollection.ID_PREFIX + "\\d{4}$";
    @Reference
    private YearsCollection years;
    @Reference
    private MonthsCollection months;

    @Override
    public List<String> getSupportedNodeTypes() {
        return Collections.emptyList();
    }

    @Override
    public String getPathPattern() {
        return PATH_PATTERN;
    }

    @Override
    public String getIdPattern() {
        return ID_PATTERN;
    }

    @Override
    public String findNodeId(String path) {
        return YearsCollection.ID_PREFIX.concat(PathHelper.getLeaf(path));
    }

    @Override
    public ExternalData getData(String identifier) {
        String path = new PathBuilder("movies").append(identifier.substring(YearsCollection.ID_PREFIX.length())).build();
        return years.getData(identifier).toExternalData(path);
    }

    @Override
    public List<ExternalData> listChildren(String path) {
        String year = PathHelper.getLeaf(path);
        return months.list(year).stream()
                .map(month -> month.toExternalData(new PathBuilder(path).append(StringUtils.substringAfter(month.getName(), "-")).build()))
                .sorted(Comparator.comparing(ExternalData::getName))
                .collect(Collectors.toList());
    }

    @Override
    public String[] getProperty(String identifier, String propertyName) {
        return new String[0];
    }

    @Override
    public String[] getI18nProperty(String identifier, String lang, String propertyName) {
        return new String[0];
    }

}
