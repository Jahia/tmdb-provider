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

import info.movito.themoviedbapi.model.core.Movie;
import info.movito.themoviedbapi.model.core.MovieResultsPage;
import info.movito.themoviedbapi.model.movies.Cast;
import info.movito.themoviedbapi.model.movies.Credits;
import info.movito.themoviedbapi.model.movies.Crew;
import info.movito.themoviedbapi.model.movies.MovieDb;
import info.movito.themoviedbapi.tools.TmdbException;
import info.movito.themoviedbapi.tools.appendtoresponse.MovieAppendToResponse;
import info.movito.themoviedbapi.tools.builders.discover.DiscoverMovieParamBuilder;
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

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Jerome Blanchard
 */
@Component(service = { CreditsCollection.class, ProviderDataCollection.class}, scope = ServiceScope.SINGLETON, immediate = true)
public class CreditsCollection implements ProviderDataCollection {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreditsCollection.class);
    private static final String LIST_CACHE_KEY = "credits-list-";
    public static final String CAST = "cast_";
    public static final String CREW = "crew_";
    public static final String ID_PREFIX = "credits-";

    @Reference
    private TMDBClient client;
    @Reference
    private TMDBCache cache;

    @Override
    public ProviderData getData(String identifier) {
        Element element = cache.get(identifier);
        if (element != null) {
            return (ProviderData) element.getObjectValue();
        } else {
            try {
                ProviderData data = null;
                String cleanId = identifier.substring(ID_PREFIX.length());
                String creditsId = StringUtils.substringAfter(cleanId, "-");
                String movieId = StringUtils.substringBefore(cleanId, "-");
                Credits credits = client.getMovies().getCredits(Integer.parseInt(movieId), "en");
                if (creditsId.startsWith(CREW)) {
                    String pid = creditsId.substring(CREW.length());
                    Crew crew = credits.getCrew().stream().filter(c -> c.getId() == Integer.parseInt(pid)).findFirst().orElseThrow();
                    data = map(crew);
                }
                if (creditsId.startsWith(CAST)) {
                    String pid = creditsId.substring(CAST.length());
                    Cast cast = credits.getCast().stream().filter(c -> c.getId() == Integer.parseInt(pid)).findFirst().orElseThrow();
                    data = map(cast);
                }
                cache.put(new Element(identifier, data));
                return data;
            } catch (Exception e) {
                LOGGER.warn("Error while getting movie " + identifier, e);
                return null;
            }
        }
    }

    public List<ProviderData> list(String movieId) {
        Element element = cache.get(LIST_CACHE_KEY);
        if (element != null) {
            List<String> ids = (List<String>) element.getObjectValue();
            return ids.stream().map(this::getData).collect(Collectors.toList());
        } else {
            try {
                List<ProviderData> results = new ArrayList<>();
                List<String> ids = new ArrayList<>();
                Credits credits = client.getMovies().getCredits(Integer.parseInt(movieId), "en");
                ids.addAll(credits.getCast().stream()
                        .map(c -> ID_PREFIX.concat(movieId).concat("-").concat(CAST).concat(Integer.toString(c.getId())))
                        .collect(Collectors.toList()));
                ids.addAll(credits.getCrew().stream()
                        .map(c -> ID_PREFIX.concat(movieId).concat("-").concat(CREW).concat(Integer.toString(c.getId())))
                        .collect(Collectors.toList()));
                credits.getCast().stream().map(this::map).filter(Objects::nonNull).forEach(this::cache);
                credits.getCrew().stream().map(this::map).filter(Objects::nonNull).forEach(this::cache);
                cache.put(new Element(LIST_CACHE_KEY, ids));
                return ids.stream().map(this::getData).filter(Objects::nonNull).collect(Collectors.toList());
            } catch (Exception e) {
                LOGGER.warn("Error while getting movies ", e);
                return Collections.emptyList();
            }
        }
    }

    protected ProviderData map(Crew crew) {
        try {
            String baseUrl = client.getConfiguration().getImageConfig().getBaseUrl();
            Map<String, String[]> properties = new HashMap<>();
            if (StringUtils.isNotEmpty(crew.getDepartment())) {
                properties.put("department", new String[] { crew.getDepartment() });
            }
            if (StringUtils.isNotEmpty(crew.getJob())) {
                properties.put("job", new String[] { crew.getJob() });
            }
            properties.put("person", new String[] { "person-" + crew.getId() });
            if (StringUtils.isNotEmpty(crew.getName())) {
                properties.put("name", new String[] { crew.getName() });
            }
            if (StringUtils.isNotEmpty(crew.getProfilePath())) {
                properties.put("profile", new String[] {
                        baseUrl + client.getConfiguration().getImageConfig().getProfileSizes().get(1) + crew.getProfilePath() });
            }
            //TODO Id should include year monthe movie id and the crew id
            return new ProviderData(ID_PREFIX + crew.getId(), Naming.NodeType.CAST, Integer.toString(crew.getId()), properties);
        } catch (TmdbException e) {
            return null;
        }
    }

    private ProviderData map(Cast cast) {
        try {
            String baseUrl = client.getConfiguration().getImageConfig().getBaseUrl();
            Map<String, String[]> properties = new HashMap<>();
            if (StringUtils.isNotEmpty(cast.getCharacter())) {
                properties.put("character", new String[] { cast.getCharacter() });
            }
            properties.put("order", new String[] { Integer.toString(cast.getOrder()) });
            properties.put("cast_id", new String[] { Integer.toString(cast.getCastId()) });
            properties.put("id", new String[] { Integer.toString(cast.getId()) });
            if (StringUtils.isNotEmpty(cast.getName())) {
                properties.put("name", new String[] { cast.getName() });
            }
            if (StringUtils.isNotEmpty(cast.getProfilePath())) {
                properties.put("profile", new String[] {
                        baseUrl + client.getConfiguration().getImageConfig().getProfileSizes().get(1) + cast.getProfilePath() });
            }
            //TODO Id should include year monthe movie id and the crew id
            return new ProviderData(ID_PREFIX + cast.getId(), Naming.NodeType.CAST, Integer.toString(cast.getId()), properties);
        } catch (TmdbException e) {
            return null;
        }
    }

    private void cache(ProviderData data) {
        cache.put(new Element(data.getId(), data));
    }

}
