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
 * Handler for movies node.
 * List
 *
 * @author Jerome Blanchard
 */
//@NodeMapping(pathPattern = "^/movies/\\d{4}/\\d{2}$", idPattern = "^movies-\\d{4}-\\d{2}$", supportedNodeType =
//        {Naming.NodeType.CONTENT_FOLDER}, hasLazyProperties = false)
public class MonthNodeHandler {

    /*
    private static final Logger LOGGER = LoggerFactory.getLogger(MonthNodeHandler.class);
    private static final int MAX_CHILD = 100;
    public static final String ID_PREFIX = "movies-";
    public static final String CACHE_PREFIX = "months-";

    public MonthNodeHandler() {
    }

    @Override public List<String> listChildren(String path) {
        String year = PathHelper.getParent(path);
        String month = PathHelper.getLeaf(path);
        String cacheKey = Naming.Cache.MOVIES_LIST_CACHE_PREFIX + year + month;
        if (getCache().get(cacheKey) != null) {
            return (List<String>) getCache().get(cacheKey).getObjectValue();
        } else {
            Calendar instance = Calendar.getInstance();
            instance.set(Calendar.YEAR, Integer.parseInt(year));
            instance.set(Calendar.MONTH, Integer.parseInt(month));
            instance.set(Calendar.DAY_OF_MONTH, 1);
            instance.roll(Calendar.DAY_OF_MONTH, false);
            DiscoverMovieParamBuilder builder = new DiscoverMovieParamBuilder()
                    .primaryReleaseDateGte(year.concat("-").concat(month).concat("-01"))
                    .primaryReleaseDateLte(year.concat("-").concat(month).concat("-").concat(Integer.toString(instance.get(Calendar.DAY_OF_MONTH))))
                    .page(1);
            List<String> children = new ArrayList<>();
            try {
                MovieResultsPage page;
                do {
                    page = getApiClient().getDiscover().getMovie(builder);
                    children.addAll(page.getResults().stream()
                            .map(m -> Integer.toString(m.getId()))
                            .collect(Collectors.toList()));
                    builder.page(page.getPage() + 1);
                } while (page.getPage() < page.getTotalPages() && children.size() < MAX_CHILD);
            } catch (Exception e) {
                LOGGER.warn("Error while getting movies ", e);
            }
            getCache().put(new Element(cacheKey, children));
            return children;
        }
    }

    @Override public List<ExternalData> listChildrenNodes(String path) {
        // ${TODO} Use the discover API to get the movies for the month
        return null;
    }

    @Override public ExternalData getData(String identifier) {
        if (getCache().get(CACHE_PREFIX + identifier) != null) {
            return (ExternalData) getCache().get(CACHE_PREFIX + identifier).getObjectValue();
        } else {
            final String date = StringUtils.substring(identifier, ID_PREFIX.length());
            final String year = date.split("-")[0];
            final String month = date.split("-")[1];
            Map<String, String[]> properties = new HashMap<>();
            properties.put(Constants.JCR_TITLE, new String[] { date });
            String path = new PathBuilder(MoviesNodeHandler.PATH_LABEL).append(year).append(month).build();
            ExternalData data = new ExternalData(identifier, path, Naming.NodeType.CONTENT_FOLDER, properties);
            getCache().put(new Element(CACHE_PREFIX + identifier, data));
            return data;
        }
    }

    @Override public String getIdFromPath(String path) {
        return ID_PREFIX.concat(PathHelper.getParent(path)).concat("-").concat(PathHelper.getLeaf(path));
    }

    @Override public String getPathLabel() {
        return "";
    }

     */
}
