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

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * @author Jerome Blanchard
 */
@ObjectClassDefinition(name = "TMDB Provider", description = "A TMDB Provider configuration")
public @interface TMDBDataSourceConfig {
    @AttributeDefinition(name = "TMDB API key", defaultValue = "", description = "The API key to use for The Movie Database") String apiKey() default "";
    @AttributeDefinition(name = "TMDB Mount path", defaultValue = "/sites/systemsite/contents/tmdb", description = "The path at which "
                + "to mount the database in the JCR") String mountPoint() default "/sites/systemsite/contents/tmdb";
    @AttributeDefinition(name = "TMDB Movies Original Language", defaultValue = "fr",
            description = "The original language of movies to browse") String originalLanguage() default "fr";

}
