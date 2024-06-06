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
package org.jahia.modules.provider.tmdb.helper;

/**
 * @author Jerome Blanchard
 */
public class PathBuilder {

    public static final String PATH_SEPARATOR = "/";

    StringBuilder builder;

    public PathBuilder() {
        builder = new StringBuilder();
    }

    public PathBuilder(String path) {
        this();
        this.append(path);
    }

    public PathBuilder append(String path) {
        if (!path.startsWith(PATH_SEPARATOR)) {
            builder.append(PATH_SEPARATOR);
        }
        builder.append(path);
        return this;
    }

    public PathBuilder append(int id) {
        builder.append(PATH_SEPARATOR);
        builder.append(id);
        return this;
    }

    public String build() {
        if (builder.length() == 0) {
            builder.append(PATH_SEPARATOR);
        }
        return builder.toString();
    }

}
