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

import java.lang.annotation.Annotation;

/**
 * @author Jerome Blanchard
 */
class TestConfig implements TMDBDataSourceConfig {

    @Override public String apiKey() {
        return "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiIyN2Q3MWViNzU3ZmNkZDY1NTk1OTYwM2RlYzQxNWZkMyIsInN1YiI6IjY2NDQ1ZmQ2N2EwYTk1MzIzYWVjM2YxNiIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.GUoWlbqEtU3e8Btf2yTh1cGosGJE-63IKrASY2D8JgE";
    }

    @Override public String mountPoint() {
        return "/dummy";
    }

    @Override public Class<? extends Annotation> annotationType() {
        return null;
    }

    @Override public String originalLanguage() {
        return "fr";
    }
}
