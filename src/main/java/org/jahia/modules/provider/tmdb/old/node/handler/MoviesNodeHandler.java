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
package org.jahia.modules.provider.tmdb.old.node.handler;

/**
 * Handler for movies node.
 * List of years for movies.
 *
 * @author Jerome Blanchard
 */
//@NodeMapping(pathPattern = "^/movies$", idPattern = "^movies$", supportedNodeType = {Naming.NodeType.CONTENT_FOLDER},
//        hasLazyProperties = false)
public class MoviesNodeHandler {
    /*
    public static final String PATH_LABEL = "movies";
    public static final String ID_PREFIX = "movies";

    //Children of the /movies node are the years we want to display in browsing (5 last years)
    private static final List<String> CHILDREN = IntStream
            .rangeClosed(Calendar.getInstance().get(Calendar.YEAR) - 10, Calendar.getInstance().get(Calendar.YEAR))
            .mapToObj(Integer::toString)
            .collect(Collectors.toList());
    private static ExternalData NODE;
    {
        Map<String, String[]> properties = new HashMap<>();
        properties.put(Constants.JCR_TITLE, new String[] { PATH_LABEL });
        String path = new PathBuilder(PATH_LABEL).build();
        NODE = new ExternalData(ID_PREFIX, path, Naming.NodeType.CONTENT_FOLDER, properties);
    }

    public MoviesNodeHandler() {
    }

    @Override public List<String> listChildren(String path) {
        return CHILDREN;
    }

    @Override public List<ExternalData> listChildrenNodes(String path) {
        // ${TODO} Use the discover API to get the movies for the month
        return null;
    }

    @Override public ExternalData getData(String identifier) {
        return NODE;
    }

    @Override public String getIdFromPath(String path) {
        return ID_PREFIX;
    }

    @Override public String getPathLabel() {
        return PATH_LABEL;
    }

    private String buildChildrenPath(String child) {
        return new PathBuilder(PATH_LABEL).append(child).build();
    }

     */
}
