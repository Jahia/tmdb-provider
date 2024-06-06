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
package org.jahia.modules.provider.tmdb.old;

/**
 * @author Jerome Blanchard
 */
public class MovieMapper {//implements ItemMapper {

    /*

    private static final Logger LOGGER = LoggerFactory.getLogger(MovieMapper.class);
    public static final String ID_PREFIX = "movie-";
    private static final Set<String> LAZY_PROPERTIES = Set.of("original_title", "homepage", "status", "runtime", "imdb_id", "budget",
            "revenue", "genres", "keywords");
    private static final Set<String> LAZY_I18N_PROPERTIES = Set.of(Constants.JCR_TITLE, "overview", "tagline", "poster_path");
    private static final String MOVIES_CACHE_KEY = "movies-";
    private static final String MOVIES_DB_CACHE_KEY = "movies-db-";

    public static List<ExternalData> listByDate(NodeMapper handler, String year, String month, long max) throws TmdbException {
        Calendar instance = Calendar.getInstance();
        instance.set(Calendar.YEAR, Integer.parseInt(year));
        instance.set(Calendar.MONTH, Integer.parseInt(month));
        instance.set(Calendar.DAY_OF_MONTH, 1);
        instance.roll(Calendar.DAY_OF_MONTH, false);
        DiscoverMovieParamBuilder builder = new DiscoverMovieParamBuilder()
                .primaryReleaseDateGte(year.concat("-").concat(month).concat("-01"))
                .primaryReleaseDateLte(year.concat("-").concat(month).concat("-").concat(Integer.toString(instance.get(Calendar.DAY_OF_MONTH))))
                .page(1);
        List<ExternalData> children = new ArrayList<>();
        Configuration config = handler.getConfiguration();
        try {
            MovieResultsPage page;
            do {
                page = handler.getApiClient().getDiscover().getMovie(builder);
                children.addAll(page.getResults().stream().map(m -> fromMovie(config, m)).collect(Collectors.toList()));
                builder.page(page.getPage() + 1);
            } while (page.getPage() < page.getTotalPages() && children.size() < max);
        } catch (Exception e) {
            LOGGER.warn("Error while getting movies ", e);
        }
        //getCache().put(new Element(cacheKey, children));
        return children;
    }

    public static ExternalData fromMovie(Configuration configuration, Movie movie) {
        String baseUrl = configuration.getImageConfig().getBaseUrl();
        Map<String, String[]> properties = new HashMap<>();
        if (StringUtils.isNotEmpty(movie.getBackdropPath())) {
            String backdropPath = baseUrl.concat(configuration.getImageConfig().getBackdropSizes().get(1)).concat(movie.getBackdropPath());
            properties.put("backdrop_path", new String[] { backdropPath });
        }
        if (StringUtils.isNotEmpty(movie.getReleaseDate())) {
            properties.put("release_date", new String[] { movie.getReleaseDate() + "T00:00:00.000+00:00" });
        }
        properties.put("adult", new String[] { Boolean.toString(movie.getAdult()) });
        properties.put("vote_average", new String[] { Double.toString(movie.getVoteAverage()) });
        properties.put("vote_count", new String[] { Integer.toString(movie.getVoteCount()) });
        properties.put("popularity", new String[] { Double.toString(movie.getPopularity()) });
        String path = buildMoviePath(Integer.toString(movie.getId()), movie.getReleaseDate());
        String identifier = ID_PREFIX + movie.getId();
        ExternalData data = new ExternalData(identifier, path, Naming.NodeType.MOVIE, properties);
        data.setLazyProperties(new HashSet<>(LAZY_PROPERTIES));
        data.setLazyI18nProperties(Map.of("en", new HashSet<>(LAZY_I18N_PROPERTIES), "fr", new HashSet<>(LAZY_I18N_PROPERTIES)));
        return data;
    }

    public static ExternalData fromMovieDb(Configuration configuration, MovieDb movie) {
        String baseUrl = configuration.getImageConfig().getBaseUrl();
        Map<String, String[]> properties = new HashMap<>();
        if (StringUtils.isNotEmpty(movie.getBackdropPath())) {
            String backdropPath = baseUrl.concat(configuration.getImageConfig().getBackdropSizes().get(1)).concat(movie.getBackdropPath());
            properties.put("backdrop_path", new String[] { backdropPath });
        }
        if (StringUtils.isNotEmpty(movie.getReleaseDate())) {
            properties.put("release_date", new String[] { movie.getReleaseDate() + "T00:00:00.000+00:00" });
        }
        if (!movie.getGenres().isEmpty()) {
            String[] genres = movie.getGenres().stream().map(g -> g.getName()).collect(Collectors.toList()).toArray(new String[0]);
            properties.put("j:tagList", genres);
        }
        if (!movie.getKeywords().getKeywords().isEmpty()) {
            String[] keywords = movie.getKeywords().getKeywords().stream().map(g -> g.getName()).collect(Collectors.toList()).toArray(new String[0]);
            properties.put("j:tagList", keywords);
        }

        properties.put("adult", new String[] { Boolean.toString(movie.getAdult()) });
        properties.put("vote_average", new String[] { Double.toString(movie.getVoteAverage()) });
        properties.put("vote_count", new String[] { Integer.toString(movie.getVoteCount()) });
        properties.put("popularity", new String[] { Double.toString(movie.getPopularity()) });
        String path = buildMoviePath(Integer.toString(movie.getId()), movie.getReleaseDate());
        String identifier = ID_PREFIX + movie.getId();
        ExternalData data = new ExternalData(identifier, path, Naming.NodeType.MOVIE, properties);
        data.setLazyProperties(new HashSet<>(LAZY_PROPERTIES));
        data.setLazyI18nProperties(Map.of("en", new HashSet<>(LAZY_I18N_PROPERTIES), "fr", new HashSet<>(LAZY_I18N_PROPERTIES)));
        return data;
    }

    public String[] getProperty(Configuration configuration, MovieDb movie, String lang, String propertyName) {
        String[] result = null;
        if (propertyName.equals(Constants.JCR_TITLE) && StringUtils.isNotEmpty(movie.getTitle()) && !movie.getTitle()
                .equals("null")) {
            result = new String[] { movie.getTitle() };
        } else if (propertyName.equals(Naming.Property.POSTER_PATH) && StringUtils.isNotEmpty(movie.getPosterPath())
                && !movie.getPosterPath().equals("null")) {
            String baseUrl = configuration.getImageConfig().getBaseUrl();
            String posterPath = baseUrl.concat(configuration.getImageConfig().getPosterSizes().get(1)).concat(movie.getPosterPath());
            result = new String[] { posterPath };
        } else if (propertyName.equals("runtime")) {
            result = new String[] { "0" };
        } else {
            try {
                String methodName = "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
                Method method = MovieDb.class.getMethod(methodName);
                Object value = method.invoke(movie);
                if (value != null && !String.valueOf(value).equals("null")) {
                    result = new String[] { String.valueOf(value) };
                }
            } catch (Exception e) {
                LOGGER.debug("Unable to access movie property: " + propertyName, e);
            }
        }
        return result;
    }

    private static String buildMoviePath(String mid, String releaseDate) {
        String year = releaseDate.split( "-")[0];
        String month = releaseDate.split( "-")[1];
        return new PathBuilder(MoviesNodeHandler.PATH_LABEL).append(year).append(month).append(mid).build();
    }

     */

}
