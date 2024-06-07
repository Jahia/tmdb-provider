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
package org.jahia.modules.provider.tmdb.data;

import info.movito.themoviedbapi.model.core.Genre;
import info.movito.themoviedbapi.model.people.PersonDb;
import info.movito.themoviedbapi.tools.TmdbException;
import net.sf.ehcache.Element;
import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.modules.provider.tmdb.TMDBCache;
import org.jahia.modules.provider.tmdb.TMDBClient;
import org.jahia.modules.provider.tmdb.helper.Naming;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Load the movie db persons
 *
 * @author Jerome Blanchard
 */
@Component(service = { PersonsCollection.class, ProviderDataCollection.class}, scope = ServiceScope.SINGLETON, immediate = true)
public class PersonsCollection implements ProviderDataCollection {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersonsCollection.class);
    public static final String ID_PREFIX = "pid-";

    @Reference
    private TMDBClient client;
    @Reference
    private TMDBCache cache;

    @Override
    public ProviderData getData(String identifier) {
        if (cache.get(identifier) == null) {
            try {
                String pid = identifier.substring(ID_PREFIX.length());
                PersonDb person = client.getPeople().getDetails(Integer.parseInt(pid), "en-US");
                ProviderData data = this.map(person);
                cache.put(new Element(identifier, data));
                return data;
            } catch (Exception e) {
                LOGGER.error("Error while fetching genres", e);
                return null;
            }
        } else {
            return (ProviderData) cache.get(identifier).getObjectValue();
        }
    }

    protected ProviderData map(PersonDb person) {
        try {
            String baseUrl = client.getConfiguration().getImageConfig().getBaseUrl();
            String name = person.getName().toLowerCase().replaceAll("[^a-z0-9]", "");
            ProviderData data = new ProviderData().withId(ID_PREFIX + person.getId()).withType(Naming.NodeType.MOVIE_PERSON).withName(name);
            if (StringUtils.isNotEmpty(person.getName())) {
                data.withProperty("name", new String[] { person.getName() });
            }
            if (StringUtils.isNotEmpty(person.getBiography())) {
                data.withProperty("biography", new String[] { person.getBiography() });
            }
            if (StringUtils.isNotEmpty(person.getHomepage()) && !person.getHomepage().equals("null")) {
                data.withProperty("homepage", new String[] { person.getHomepage() });
            }
            if (StringUtils.isNotEmpty(person.getProfilePath()) && !person.getProfilePath().equals("null")) {
                String profileSize = client.getConfiguration().getImageConfig().getProfileSizes().get(2);
                data.withProperty("profile", new String[] { baseUrl.concat(profileSize).concat(person.getProfilePath()) });
            }
            if (StringUtils.isNotEmpty(person.getBirthday()) && !person.getBirthday().equals("null")) {
                data.withProperty("birthday", new String[] { person.getBirthday() + "T00:00:00.000+00:00" });
            }
            if (StringUtils.isNotEmpty(person.getDeathDay()) && !person.getDeathDay().equals("null")) {
                data.withProperty("deathday", new String[] { person.getDeathDay() + "T00:00:00.000+00:00" });
            }
            return data;
        } catch (TmdbException e) {
            return null;
        }
    }
}
