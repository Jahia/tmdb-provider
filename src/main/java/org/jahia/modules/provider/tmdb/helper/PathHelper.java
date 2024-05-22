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
 *
 * @author Jerome Blanchard
 */
public class PathHelper {

    public static void ensureValidity(String path) {
        if (!path.matches("^(/[^/ ]*)+/?$")) {
            throw new IllegalArgumentException("Invalid path: " + path);
        }
    }

    public static void ensureMatches(String path, String pattern) {
        if (!path.matches(pattern)) {
            throw new IllegalArgumentException("Invalid path: " + path);
        }
    }

    public static String getLeaf(String path) {
        if (path == null || path.isEmpty() || path.equals("/")) {
            return "";
        }
        String[] segments = path.split("/");
        return segments[segments.length - 1];
    }

    public static String getParent(String path) {
        if (path == null || path.isEmpty() || path.equals("/")) {
            return null; // root has no parent
        }
        String[] segments = path.split("/");
        if (segments.length == 1) {
            return null; // root has no parent
        }
        if (segments.length == 2) {
            return ""; // top-level nodes have root as parent
        }
        return segments[segments.length - 2];
    }


}
