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
 * Handler for persons node.
 *
 * @author Jerome Blanchard
 */
//@NodeMapping(pathPattern = "^/persons$", idPattern = "^persons$", supportedNodeType =
//        {Naming.NodeType.CONTENT_FOLDER},
 //       hasLazyProperties = false)
public class PersonsNodeHandler {
    /*

    public static final String PATH_LABEL = "persons";
    public static final String ID_PREFIX = "persons";
    private static ExternalData NODE;
    {
        Map<String, String[]> properties = new HashMap<>();
        properties.put(Constants.JCR_TITLE, new String[] { PATH_LABEL });
        String path = new PathBuilder(PATH_LABEL).build();
        NODE = new ExternalData(ID_PREFIX, path, Naming.NodeType.CONTENT_FOLDER, properties);
    }

    public PersonsNodeHandler() {
    }

    @Override public List<String> listChildren(String path) {
        return Collections.emptyList();
    }

    @Override
    public List<ExternalData> listChildrenNodes(String path) {
        return Collections.emptyList();
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

     */
}
