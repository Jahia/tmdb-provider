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

import com.google.common.collect.Sets;

import java.util.Set;

/**
 * Global naming for some variables
 *
 * @author Jerome Blanchard
 */
public class Naming {

    public static final Set<String> NODE_TYPES = Sets.newHashSet(
            NodeType.CONTENT_FOLDER,
            NodeType.CONTENT_REFERENCE,
            NodeType.MOVIES_LIST,
            NodeType.MOVIE,
            NodeType.MOVIE_PERSON,
            NodeType.MOVIE_GENRE,
            NodeType.CAST,
            NodeType.CREW);

    public static class NodeType {
        public static final String CONTENT_FOLDER = "jnt:contentFolder";
        public static final String CONTENT_REFERENCE = "jnt:contentReference";
        public static final String MOVIE ="jnt:movie";
        public static final String MOVIES_LIST = "jnt:moviesList";
        public static final String MOVIE_PERSON = "jnt:moviePerson";
        public static final String MOVIE_GENRE = "jnt:movieGenre";
        public static final String CAST = "jnt:cast";
        public static final String CREW = "jnt:crew";
    }

}
