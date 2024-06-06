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
package org.jahia.modules.provider.tmdb.binding;

import org.jahia.modules.external.ExternalData;
import org.jahia.modules.external.ExternalQuery;
import org.jahia.modules.provider.tmdb.data.ProviderData;

import java.util.Collections;
import java.util.List;

/**
 * Map item on a tree based
 *
 * @author Jerome Blanchard
 */
public interface NodeBinding {

    String getPathPattern();
    String getIdPattern();
    List<ExternalData> listChildren(String path);
    String findNodeId(String path);
    ExternalData getData(String identifier);
    String[] getProperty(String identifier, String lang, String propertyName);
    List<String> getSupportedNodeTypes() ;
    default List<String> search(String nddeType, ExternalQuery query) {
        return Collections.emptyList();
    }

}
