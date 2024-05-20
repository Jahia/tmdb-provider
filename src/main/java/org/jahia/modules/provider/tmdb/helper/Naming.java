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
            NodeType.CAST,
            NodeType.CREW);

    public static class Cache {
        public static final String TMDB_CACHE = "tmdb-cache";
        public static final String INDEXED_FULL_MOVIE_CACHE_PREFIX = "indexed-full-movie-";
        public static final String MOVIE_CREDITS_LIST_CACHE_PREFIX = "movie-credits-list-";
        public static final String MOVIE_CREDITS_CACHE_PREFIX = "movie-credits-";
        public static final String MOVIE_CREDITS_QUERY_CACHE_KEY_PREFIX = "movie_credits_query_";
        public static final String MOVIE_CACHE_PREFIX = "movie-";
        public static final String MOVIE_API_CACHE_PREFIX = "movie-api-";
        public static final String MOVIES_FOLDER_CACHE_PREFIX = "movies-date-";
        public static final String CONFIGURATION_CACHE_KEY = "configuration";
    }

    public static class NodeType {
        public static final String CONTENT_FOLDER = "jnt:contentFolder";
        public static final String CONTENT_REFERENCE = "jnt:contentReference";
        public static final String MOVIE ="jnt:movie";
        public static final String MOVIES_LIST = "jnt:moviesList";
        public static final String MOVIE_PERSON = "jnt:moviePerson";
        public static final String CAST = "jnt:cast";
        public static final String CREW = "jnt:crew";
    }

    public static class Property {
        public static final String POSTER_PATH = "poster_path";
        public static final String IMAGES = "images";
        public static final String BASE_URL = "base_url";
    }

}
