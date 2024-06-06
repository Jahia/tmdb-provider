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
 * Short description of the class
 *
 * @author Jerome Blanchard
 */
//@NodeMapping(pathPattern = "^/persons/\\d+$", idPattern = "^person-\\d+$", supportedNodeType = {Naming.NodeType.MOVIE_PERSON},
//        hasLazyProperties = false)
public class PersonNodeHandler {
    /*

    private static final Logger LOGGER = LoggerFactory.getLogger(PersonNodeHandler.class);
    public static final String PATH_LABEL = "person";
    public static final String ID_PREFIX = "person-";
    public static final String CACHE_PREFIX = "person-";

    public PersonNodeHandler() {
    }

    @Override public List<String> listChildren(String path) {
        return Collections.emptyList();
    }

    @Override
    public List<ExternalData> listChildrenNodes(String path) {
        return Collections.emptyList();
    }

    @Override public ExternalData getData(String identifier) {
        String pid = identifier.substring(ID_PREFIX.length());
        if (getCache().get(CACHE_PREFIX + pid) != null) {
            return (ExternalData) getCache().get(CACHE_PREFIX + pid).getObjectValue();
        } else {
            try {
                PersonDb person = getApiClient().getPeople().getDetails(Integer.parseInt(pid), "en-US");
                String baseUrl = getConfiguration().getImageConfig().getBaseUrl();
                Map<String, String[]> properties = new HashMap<>();
                if (StringUtils.isNotEmpty(person.getName())) {
                    properties.put("name", new String[] { person.getName() });
                }
                if (StringUtils.isNotEmpty(person.getBiography())) {
                    properties.put("biography", new String[] { person.getBiography() });
                }
                if (StringUtils.isNotEmpty(person.getHomepage()) && !person.getHomepage().equals("null")) {
                    properties.put("homepage", new String[] { person.getHomepage() });
                }
                if (StringUtils.isNotEmpty(person.getProfilePath()) && !person.getProfilePath().equals("null")) {
                    properties.put("profile", new String[] {
                            baseUrl.concat(getConfiguration().getImageConfig().getProfileSizes().get(2)).concat(person.getProfilePath()) });
                }
                if (StringUtils.isNotEmpty(person.getBirthday()) && !person.getBirthday().equals("null")) {
                    properties.put("birthday", new String[] { person.getBirthday() + "T00:00:00.000+00:00" });
                }
                if (StringUtils.isNotEmpty(person.getDeathDay()) && !person.getDeathDay().equals("null")) {
                    properties.put("deathday", new String[] { person.getDeathDay() + "T00:00:00.000+00:00" });
                }
                String path = new PathBuilder(PersonsNodeHandler.PATH_LABEL).append(pid).build();
                ExternalData data = new ExternalData(identifier, path, Naming.NodeType.MOVIE_PERSON, properties);
                getCache().put(new Element(CACHE_PREFIX + pid, data));
                return data;
            } catch (TmdbException e) {
                LOGGER.warn("Error while getting person data", e);
                return null;
            }
        }
    }

    @Override public String getIdFromPath(String path) {
        return ID_PREFIX.concat(PathHelper.getLeaf(path));
    }

    @Override public String getPathLabel() {
        return PATH_LABEL;
    }

     */
}