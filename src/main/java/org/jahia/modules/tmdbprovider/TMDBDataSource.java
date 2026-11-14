package org.jahia.modules.tmdbprovider;

import net.sf.ehcache.CacheException;
import org.apache.commons.lang.StringUtils;
import org.jahia.exceptions.JahiaInitializationException;
import org.jahia.modules.external.ExternalContentStoreProvider;
import org.jahia.modules.external.ExternalContentStoreProviderFactory;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.external.ExternalDataSource;
import org.jahia.modules.external.ExternalQuery;
import org.jahia.modules.external.events.EventService;
import org.jahia.modules.external.query.QueryHelper;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.cache.CacheProvider;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRStoreProvider;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Binary;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Mounts The Movie Database (TMDB) in the JCR.
 *
 * <pre>
 * /movies/YYYY/MM/&lt;id&gt;                 jnt:movie, with cast_* and crew_* children
 * /series/&lt;id&gt;                         jnt:tvSeries (the ids of the seriesIds setting)
 * /series/&lt;id&gt;/season_&lt;n&gt;              jnt:tvSeason
 * /series/&lt;id&gt;/season_&lt;n&gt;/episode_&lt;m&gt;  jnt:tvEpisode
 * /series/&lt;id&gt;/cast_&lt;person&gt;           jnt:cast, from the aggregate credits
 * /collections/&lt;id&gt;                    jnt:movieCollection (the collectionIds setting)
 * /featured/&lt;id&gt;                       jnt:contentReference to a movie (the featuredMovieIds setting)
 * /persons/&lt;id&gt;                        jnt:moviePerson
 * /lists/&lt;id&gt;                          jnt:moviesList of the TMDB account
 * </pre>
 *
 * Identifiers are stable: weak references and the external mapping table of Jahia depend on them.
 */
@Component(service = {ExternalDataSource.class, TMDBDataSource.class}, immediate = true, configurationPid = "org.jahia.modules.tmdbprovider")
@Designate(ocd = TMDBDataSource.Config.class)
public class TMDBDataSource implements ExternalDataSource, ExternalDataSource.LazyProperty, ExternalDataSource.Searchable {

    @ObjectClassDefinition(name = "TMDB Provider", description = "A TMDB Provider configuration")
    public @interface Config {
        @AttributeDefinition(name = "TMDB API read access token", description = "The v4 read access token, sent as a Bearer header. When empty, the TMDB_API_READ_ACCESS_TOKEN environment variable is used.")
        String apiReadAccessToken() default "";

        @AttributeDefinition(name = "TMDB API key", description = "The v3 API key, used when there is no read access token. When empty, the TMDB_API_KEY environment variable is used.")
        String apiKey() default "";

        @AttributeDefinition(name = "TMDB Mount path", description = "The path at which to mount the database in the JCR")
        String mountPoint() default "/sites/digitall/contents/tmdb";

        @AttributeDefinition(name = "Languages", description = "Comma-separated languages whose titles, overviews and posters are read from TMDB")
        String languages() default "en,fr";

        @AttributeDefinition(name = "Series", description = "Comma-separated TMDB ids of the TV series listed under /series")
        String seriesIds() default "";

        @AttributeDefinition(name = "Collections", description = "Comma-separated TMDB ids of the collections listed under /collections")
        String collectionIds() default "";

        @AttributeDefinition(name = "Featured movies", description = "Comma-separated TMDB ids of the movies listed under /featured")
        String featuredMovieIds() default "";

        @AttributeDefinition(name = "Cache time to live", description = "Seconds an answer of TMDB stays in the cache")
        int cacheTimeToLive() default 21600;

        @AttributeDefinition(name = "Poster size", description = "A poster size of the TMDB configuration, such as w342 or w500")
        String posterSize() default "w342";

        @AttributeDefinition(name = "Backdrop size", description = "A backdrop size of the TMDB configuration, such as w780 or w1280")
        String backdropSize() default "w1280";

        @AttributeDefinition(name = "Profile size", description = "A profile size of the TMDB configuration, such as w185 or h632")
        String profileSize() default "w185";

        @AttributeDefinition(name = "Still size", description = "An episode still size of the TMDB configuration, such as w300")
        String stillSize() default "w300";
    }

    private static final Logger logger = LoggerFactory.getLogger(TMDBDataSource.class);

    static final String ENV_READ_ACCESS_TOKEN = "TMDB_API_READ_ACCESS_TOKEN";
    static final String ENV_API_KEY = "TMDB_API_KEY";
    static final String DEFAULT_LANGUAGE = "en";

    public static final List<String> ROOT_NODES = List.of("movies", "series", "collections", "featured", "persons", "lists");
    public static final Set<String> LAZY_PROPERTIES = Set.of("original_title", "homepage", "status", "runtime", "imdb_id", "budget", "revenue");
    public static final Set<String> LAZY_I18N_PROPERTIES = Set.of("jcr:title", "overview", "tagline", "poster_path");
    static final Set<String> SEASON_LAZY_I18N_PROPERTIES = Set.of("jcr:title", "overview", "poster_path");
    static final Set<String> EPISODE_LAZY_I18N_PROPERTIES = Set.of("jcr:title", "overview");
    static final Set<String> COLLECTION_LAZY_I18N_PROPERTIES = Set.of("jcr:title", "overview", "poster_path");

    private static final String API_MOVIE = "/3/movie/";
    private static final String API_TV = "/3/tv/";
    private static final String API_COLLECTION = "/3/collection/";
    private static final String API_DISCOVER_MOVIE = "/3/discover/movie";
    private static final int TMDB_PAGE_SIZE = 20;
    private static final int MAX_SEARCH_RESULTS = 100;

    private static final Pattern NUMBER = Pattern.compile("[0-9]+");
    private static final Pattern YEAR_PATTERN = Pattern.compile("[0-9]{4}");
    private static final Pattern DATE_PATTERN = Pattern.compile("[0-9]{4}/[0-9]{2}");
    private static final Pattern LIST_ID = Pattern.compile("[a-z0-9]+");
    private static final String SEASON_PREFIX = "season_";
    private static final String EPISODE_PREFIX = "episode_";
    private static final String CAST_PREFIX = "cast_";
    private static final String CREW_PREFIX = "crew_";

    private static final List<String> EXTENDABLE_TYPES = Collections.singletonList("nt:base");

    private CacheProvider cacheProvider;
    private ExternalContentStoreProviderFactory externalContentStoreProviderFactory;
    private ExternalContentStoreProvider externalContentStoreProvider;

    private TMDBApi api;
    private TMDBCache cache = TMDBCache.NONE;
    private boolean indexEvents = true;

    private List<String> languages = List.of(DEFAULT_LANGUAGE, "fr");
    private List<String> seriesIds = Collections.emptyList();
    private List<String> collectionIds = Collections.emptyList();
    private List<String> featuredMovieIds = Collections.emptyList();
    private String posterSize = "w342";
    private String backdropSize = "w1280";
    private String profileSize = "w185";
    private String stillSize = "w300";

    private String accountId;
    private String token;
    private String sessionId;

    @Reference
    public void setCacheProvider(CacheProvider cacheProvider) {
        this.cacheProvider = cacheProvider;
    }

    @Reference
    public void setExternalContentStoreProviderFactory(ExternalContentStoreProviderFactory externalContentStoreProviderFactory) {
        this.externalContentStoreProviderFactory = externalContentStoreProviderFactory;
    }

    @Activate
    public void start(Config config) throws RepositoryException {
        String readAccessToken = firstNonEmpty(config.apiReadAccessToken(), System.getenv(ENV_READ_ACCESS_TOKEN));
        String apiKey = firstNonEmpty(config.apiKey(), System.getenv(ENV_API_KEY));
        if (StringUtils.isEmpty(readAccessToken) && StringUtils.isEmpty(apiKey)) {
            logger.warn("Neither a TMDB read access token nor an API key is set, the TMDB provider will not initialize.");
            return;
        }
        applySettings(config.languages(), config.seriesIds(), config.collectionIds(), config.featuredMovieIds(),
                config.posterSize(), config.backdropSize(), config.profileSize(), config.stillSize());
        api = new TMDBClient(readAccessToken, apiKey);
        cache = createCache(config.cacheTimeToLive());
        // Entries written by an earlier version or under other settings must not survive a restart
        cache.clear();

        externalContentStoreProvider = externalContentStoreProviderFactory.newProvider();
        externalContentStoreProvider.setDataSource(this);
        externalContentStoreProvider.setExtendableTypes(EXTENDABLE_TYPES);
        externalContentStoreProvider.setMountPoint(config.mountPoint());
        externalContentStoreProvider.setKey("TMDBProvider");
        try {
            externalContentStoreProvider.start();
        } catch (JahiaInitializationException e) {
            throw new RepositoryException("Error initializing TMDB Provider", e);
        }
    }

    @Deactivate
    public void stop() {
        if (externalContentStoreProvider != null) {
            externalContentStoreProvider.stop();
            externalContentStoreProvider = null;
        }
        api = null;
    }

    /** Sets what the OSGi configuration carries; tests call it directly. */
    void applySettings(String languages, String seriesIds, String collectionIds, String featuredMovieIds,
                       String posterSize, String backdropSize, String profileSize, String stillSize) {
        List<String> langs = splitList(languages);
        this.languages = langs.isEmpty() ? List.of(DEFAULT_LANGUAGE) : langs;
        this.seriesIds = numbersOnly(splitList(seriesIds));
        this.collectionIds = numbersOnly(splitList(collectionIds));
        this.featuredMovieIds = numbersOnly(splitList(featuredMovieIds));
        this.posterSize = StringUtils.defaultIfEmpty(posterSize, this.posterSize);
        this.backdropSize = StringUtils.defaultIfEmpty(backdropSize, this.backdropSize);
        this.profileSize = StringUtils.defaultIfEmpty(profileSize, this.profileSize);
        this.stillSize = StringUtils.defaultIfEmpty(stillSize, this.stillSize);
    }

    /** Replaces the API client and the cache; tests use it to answer from fixtures. */
    void use(TMDBApi api, TMDBCache cache, boolean indexEvents) {
        this.api = api;
        this.cache = cache;
        this.indexEvents = indexEvents;
    }

    private TMDBCache createCache(int timeToLive) {
        try {
            if (!cacheProvider.getCacheManager().cacheExists("tmdb-cache")) {
                cacheProvider.getCacheManager().addCache("tmdb-cache");
            }
            return TMDBCache.of(cacheProvider.getCacheManager().getCache("tmdb-cache"), timeToLive);
        } catch (IllegalStateException | CacheException e) {
            logger.error("Error while initializing the TMDB cache, TMDB answers will not be cached", e);
            return TMDBCache.NONE;
        }
    }

    // ------------------------------------------------------------------ tree

    @Override
    public List<String> getChildren(String path) throws RepositoryException {
        String[] s = path.split("/");
        try {
            if (s.length == 0) {
                return new ArrayList<>(ROOT_NODES);
            }
            switch (s[1]) {
                case "movies":
                    return movieChildren(s);
                case "series":
                    return seriesChildren(s);
                case "collections":
                    return s.length == 2 ? new ArrayList<>(collectionIds) : Collections.emptyList();
                case "featured":
                    return s.length == 2 ? new ArrayList<>(featuredMovieIds) : Collections.emptyList();
                case "lists":
                    return listChildren(s);
                default:
                    return Collections.emptyList();
            }
        } catch (JSONException e) {
            logger.error("Error while listing the children of {}", path, e);
            return Collections.emptyList();
        }
    }

    private List<String> movieChildren(String[] s) throws RepositoryException, JSONException {
        LocalDate today = LocalDate.now();
        List<String> r = new ArrayList<>();
        switch (s.length) {
            case 2:
                for (int year = 1900; year <= today.getYear(); year++) {
                    r.add(Integer.toString(year));
                }
                return r;
            case 3:
                int lastMonth = s[2].equals(Integer.toString(today.getYear())) ? today.getMonthValue() - 1 : 12;
                for (int month = 1; month <= lastMonth; month++) {
                    r.add(StringUtils.leftPad(Integer.toString(month), 2, "0"));
                }
                return r;
            case 4:
                String month = s[2] + "-" + s[3];
                JSONObject discover = json("discover-" + month, () -> api.get(API_DISCOVER_MOVIE,
                        "release_date.gte", month + "-01", "release_date.lte", month + "-31"));
                JSONArray results = discover.optJSONArray("results");
                for (int i = 0; results != null && i < results.length(); i++) {
                    r.add(str(results.getJSONObject(i), "id"));
                }
                return r;
            case 5:
                JSONObject credits = movieCreditsJson(s[4]);
                Set<String> names = new LinkedHashSet<>();
                JSONArray cast = credits.optJSONArray("cast");
                for (int i = 0; cast != null && i < cast.length(); i++) {
                    JSONObject c = cast.getJSONObject(i);
                    names.add(CAST_PREFIX + str(c, "cast_id") + "_" + str(c, "id"));
                }
                JSONArray crew = credits.optJSONArray("crew");
                for (int i = 0; crew != null && i < crew.length(); i++) {
                    JSONObject c = crew.getJSONObject(i);
                    names.add(CREW_PREFIX + str(c, "job") + "_" + str(c, "id"));
                }
                return new ArrayList<>(names);
            default:
                return Collections.emptyList();
        }
    }

    private List<String> seriesChildren(String[] s) throws RepositoryException, JSONException {
        if (s.length == 2) {
            return new ArrayList<>(seriesIds);
        }
        if (s.length == 3 && isNumber(s[2])) {
            Set<String> names = new LinkedHashSet<>();
            JSONArray seasons = tvJson(s[2], DEFAULT_LANGUAGE).optJSONArray("seasons");
            for (int i = 0; seasons != null && i < seasons.length(); i++) {
                names.add(SEASON_PREFIX + str(seasons.getJSONObject(i), "season_number"));
            }
            JSONObject credits = tvCreditsJson(s[2]);
            JSONArray cast = credits.optJSONArray("cast");
            for (int i = 0; cast != null && i < cast.length(); i++) {
                names.add(CAST_PREFIX + str(cast.getJSONObject(i), "id"));
            }
            JSONArray crew = credits.optJSONArray("crew");
            for (int i = 0; crew != null && i < crew.length(); i++) {
                JSONObject person = crew.getJSONObject(i);
                JSONArray jobs = person.optJSONArray("jobs");
                for (int j = 0; jobs != null && j < jobs.length(); j++) {
                    names.add(CREW_PREFIX + str(jobs.getJSONObject(j), "job") + "_" + str(person, "id"));
                }
            }
            return new ArrayList<>(names);
        }
        if (s.length == 4 && isNumber(s[2]) && s[3].startsWith(SEASON_PREFIX)) {
            String season = StringUtils.substringAfter(s[3], SEASON_PREFIX);
            List<String> r = new ArrayList<>();
            JSONArray episodes = seasonJson(s[2], season, DEFAULT_LANGUAGE).optJSONArray("episodes");
            for (int i = 0; episodes != null && i < episodes.length(); i++) {
                r.add(EPISODE_PREFIX + str(episodes.getJSONObject(i), "episode_number"));
            }
            return r;
        }
        return Collections.emptyList();
    }

    private List<String> listChildren(String[] s) throws RepositoryException, JSONException {
        List<String> r = new ArrayList<>();
        if (s.length == 2) {
            JSONArray lists = api.get("/3/account/" + getAccountId() + "/lists", "session_id", getSessionId()).getJSONArray("results");
            for (int i = 0; i < lists.length(); i++) {
                r.add(str(lists.getJSONObject(i), "id"));
            }
        } else if (s.length == 3) {
            JSONArray items = listJson(s[2]).getJSONArray("items");
            for (int i = 0; i < Math.min(items.length(), TMDB_PAGE_SIZE); i++) {
                r.add(str(items.getJSONObject(i), "id"));
            }
        }
        return r;
    }

    @Override
    public ExternalData getItemByIdentifier(String identifier) throws ItemNotFoundException {
        try {
            ExternalData data = itemByIdentifier(identifier);
            if (data != null) {
                return data;
            }
        } catch (ItemNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new ItemNotFoundException(e);
        }
        throw new ItemNotFoundException(identifier);
    }

    private ExternalData itemByIdentifier(String identifier) throws RepositoryException, JSONException {
        if (identifier.equals("root")) {
            return folder(identifier, "/");
        }
        if (identifier.endsWith("-rootfolder")) {
            String root = StringUtils.substringBefore(identifier, "-rootfolder");
            return ROOT_NODES.contains(root) ? folder(identifier, "/" + root) : null;
        }
        if (identifier.startsWith("movies-folder-")) {
            String date = StringUtils.substringAfter(identifier, "movies-folder-");
            return YEAR_PATTERN.matcher(date).matches() || DATE_PATTERN.matcher(date).matches()
                    ? folder(identifier, "/movies/" + date) : null;
        }
        if (identifier.startsWith("movie-")) {
            return getMovieData(identifier, numberAfter(identifier, "movie-"));
        }
        if (identifier.startsWith("moviecredits-")) {
            return getMovieCreditData(identifier);
        }
        if (identifier.startsWith("tv-")) {
            return getSeriesData(identifier, numberAfter(identifier, "tv-"));
        }
        if (identifier.startsWith("tvseason-")) {
            String[] ids = StringUtils.substringAfter(identifier, "tvseason-").split("-");
            requireNumbers(identifier, ids, 2);
            return getSeasonData(identifier, ids[0], ids[1]);
        }
        if (identifier.startsWith("tvepisode-")) {
            String[] ids = StringUtils.substringAfter(identifier, "tvepisode-").split("-");
            requireNumbers(identifier, ids, 3);
            return getEpisodeData(identifier, ids[0], ids[1], ids[2]);
        }
        if (identifier.startsWith("tvcredits-")) {
            String rest = StringUtils.substringAfter(identifier, "tvcredits-");
            return getSeriesCreditData(identifier, numberBefore(identifier, rest), StringUtils.substringAfter(rest, "-"));
        }
        if (identifier.startsWith("collection-")) {
            return getCollectionData(identifier, numberAfter(identifier, "collection-"));
        }
        if (identifier.startsWith("featuredref-")) {
            String movieId = numberAfter(identifier, "featuredref-");
            return reference(identifier, "/featured/" + movieId, "movie-" + movieId);
        }
        if (identifier.startsWith("lists-")) {
            return getListData(identifier);
        }
        if (identifier.startsWith("movieref-")) {
            String rest = StringUtils.substringAfter(identifier, "movieref-");
            String listId = StringUtils.substringBefore(rest, "-");
            String movieId = StringUtils.substringAfter(rest, "-");
            if (!isNumber(movieId) || !LIST_ID.matcher(listId).matches()) {
                throw new ItemNotFoundException(identifier);
            }
            return reference(identifier, "/lists/" + listId + "/" + movieId, "movie-" + movieId);
        }
        if (identifier.startsWith("person-")) {
            return getPersonData(identifier, numberAfter(identifier, "person-"));
        }
        return null;
    }

    @Override
    public ExternalData getItemByPath(String path) throws PathNotFoundException {
        String[] s = path.split("/");
        for (String segment : s) {
            // j:acl, j:translation_xx and other technical children are never stored in TMDB
            if (segment.startsWith("j:")) {
                throw new PathNotFoundException(path);
            }
        }
        try {
            String identifier = identifierForPath(s);
            if (identifier != null) {
                return getItemByIdentifier(identifier);
            }
        } catch (ItemNotFoundException e) {
            throw new PathNotFoundException(e);
        }
        throw new PathNotFoundException(path);
    }

    /** The identifier of the item at a path; the reverse of the paths built in this class. */
    static String identifierForPath(String[] s) {
        if (s.length <= 1) {
            return "root";
        }
        if (s.length == 2) {
            return s[1] + "-rootfolder";
        }
        switch (s[1]) {
            case "movies":
                switch (s.length) {
                    case 3:
                        return "movies-folder-" + s[2];
                    case 4:
                        return "movies-folder-" + s[2] + "/" + s[3];
                    case 5:
                        return "movie-" + s[4];
                    case 6:
                        return "moviecredits-" + s[4] + "-" + s[5];
                    default:
                        return null;
                }
            case "series":
                if (s.length == 3) {
                    return "tv-" + s[2];
                }
                if (s.length == 4 && s[3].startsWith(SEASON_PREFIX)) {
                    return "tvseason-" + s[2] + "-" + StringUtils.substringAfter(s[3], SEASON_PREFIX);
                }
                if (s.length == 4) {
                    return "tvcredits-" + s[2] + "-" + s[3];
                }
                if (s.length == 5 && s[3].startsWith(SEASON_PREFIX) && s[4].startsWith(EPISODE_PREFIX)) {
                    return "tvepisode-" + s[2] + "-" + StringUtils.substringAfter(s[3], SEASON_PREFIX)
                            + "-" + StringUtils.substringAfter(s[4], EPISODE_PREFIX);
                }
                return null;
            case "collections":
                return s.length == 3 ? "collection-" + s[2] : null;
            case "featured":
                return s.length == 3 ? "featuredref-" + s[2] : null;
            case "lists":
                if (s.length == 3) {
                    return "lists-" + s[2];
                }
                return s.length == 4 ? "movieref-" + s[2] + "-" + s[3] : null;
            case "persons":
                return s.length == 3 ? "person-" + s[2] : null;
            default:
                return null;
        }
    }

    // ------------------------------------------------------------------ items

    private ExternalData getMovieData(String identifier, String movieId) throws RepositoryException, JSONException {
        JSONObject movie = movieJson(movieId, DEFAULT_LANGUAGE);
        Map<String, String[]> p = new HashMap<>();
        put(p, "backdrop_path", imageUrl(backdropSize, str(movie, "backdrop_path")));
        put(p, "release_date", date(str(movie, "release_date")));
        put(p, "adult", str(movie, "adult"));
        put(p, "vote_average", str(movie, "vote_average"));
        put(p, "vote_count", str(movie, "vote_count"));
        put(p, "popularity", str(movie, "popularity"));
        put(p, "tmdb_id", movieId);
        JSONObject collection = movie.optJSONObject("belongs_to_collection");
        if (collection != null && str(collection, "id") != null) {
            put(p, "collection", "collection-" + str(collection, "id"));
        }
        putAll(p, "j:tagList", names(movie.optJSONArray("genres")));
        JSONObject keywords = movie.optJSONObject("keywords");
        putAll(p, "j:keywords", names(keywords != null ? keywords.optJSONArray("keywords") : null));

        ExternalData data = new ExternalData(identifier, getPathForMovie(movie), "jnt:movie", p);
        data.setLazyProperties(new HashSet<>(LAZY_PROPERTIES));
        data.setLazyI18nProperties(lazyI18n(LAZY_I18N_PROPERTIES));
        sendIndexEvent(data, movieId);
        return data;
    }

    private ExternalData getMovieCreditData(String identifier) throws RepositoryException, JSONException {
        String rest = StringUtils.substringAfter(identifier, "moviecredits-");
        String movieId = numberBefore(identifier, rest);
        String name = StringUtils.substringAfter(rest, "-");
        ExternalData movie = getItemByIdentifier("movie-" + movieId);
        JSONObject credits = movieCreditsJson(movieId);
        Map<String, String[]> p = new HashMap<>();
        if (name.startsWith(CREW_PREFIX)) {
            String jobAndPerson = StringUtils.substringAfter(name, CREW_PREFIX);
            String job = StringUtils.substringBeforeLast(jobAndPerson, "_");
            String personId = StringUtils.substringAfterLast(jobAndPerson, "_");
            JSONObject crew = find(credits.optJSONArray("crew"), c -> personId.equals(str(c, "id")) && job.equals(str(c, "job")));
            if (crew != null) {
                put(p, "department", str(crew, "department"));
                put(p, "job", job);
                creditPerson(p, crew);
                return new ExternalData(identifier, movie.getPath() + "/" + name, "jnt:crew", p);
            }
        } else if (name.startsWith(CAST_PREFIX)) {
            String castAndPerson = StringUtils.substringAfter(name, CAST_PREFIX);
            String castId = StringUtils.substringBefore(castAndPerson, "_");
            String personId = StringUtils.substringAfter(castAndPerson, "_");
            JSONObject cast = find(credits.optJSONArray("cast"), c -> personId.equals(str(c, "id")) && castId.equals(str(c, "cast_id")));
            if (cast != null) {
                put(p, "character", str(cast, "character"));
                put(p, "order", str(cast, "order"));
                put(p, "cast_id", castId);
                creditPerson(p, cast);
                return new ExternalData(identifier, movie.getPath() + "/" + name, "jnt:cast", p);
            }
        }
        throw new ItemNotFoundException(identifier);
    }

    private ExternalData getSeriesData(String identifier, String tvId) throws RepositoryException, JSONException {
        JSONObject tv = tvJson(tvId, DEFAULT_LANGUAGE);
        Map<String, String[]> p = new HashMap<>();
        put(p, "backdrop_path", imageUrl(backdropSize, str(tv, "backdrop_path")));
        put(p, "original_name", str(tv, "original_name"));
        put(p, "first_air_date", date(str(tv, "first_air_date")));
        put(p, "last_air_date", date(str(tv, "last_air_date")));
        put(p, "status", str(tv, "status"));
        put(p, "series_type", str(tv, "type"));
        put(p, "in_production", str(tv, "in_production"));
        put(p, "number_of_seasons", str(tv, "number_of_seasons"));
        put(p, "number_of_episodes", str(tv, "number_of_episodes"));
        putAll(p, "episode_run_time", values(tv.optJSONArray("episode_run_time")));
        putAll(p, "networks", names(tv.optJSONArray("networks")));
        List<String> creators = new ArrayList<>();
        JSONArray createdBy = tv.optJSONArray("created_by");
        for (int i = 0; createdBy != null && i < createdBy.length(); i++) {
            creators.add("person-" + str(createdBy.getJSONObject(i), "id"));
        }
        putAll(p, "created_by", creators);
        put(p, "homepage", str(tv, "homepage"));
        JSONObject externalIds = tv.optJSONObject("external_ids");
        put(p, "imdb_id", externalIds != null ? str(externalIds, "imdb_id") : null);
        put(p, "tmdb_id", tvId);
        put(p, "vote_average", str(tv, "vote_average"));
        put(p, "vote_count", str(tv, "vote_count"));
        put(p, "popularity", str(tv, "popularity"));
        putAll(p, "j:tagList", names(tv.optJSONArray("genres")));
        JSONObject keywords = tv.optJSONObject("keywords");
        putAll(p, "j:keywords", names(keywords != null ? keywords.optJSONArray("results") : null));

        ExternalData data = new ExternalData(identifier, "/series/" + tvId, "jnt:tvSeries", p);
        data.setLazyI18nProperties(lazyI18n(LAZY_I18N_PROPERTIES));
        return data;
    }

    private ExternalData getSeasonData(String identifier, String tvId, String seasonNumber) throws RepositoryException, JSONException {
        JSONObject season = seasonJson(tvId, seasonNumber, DEFAULT_LANGUAGE);
        Map<String, String[]> p = new HashMap<>();
        put(p, "season_number", str(season, "season_number"));
        put(p, "air_date", date(str(season, "air_date")));
        JSONArray episodes = season.optJSONArray("episodes");
        put(p, "episode_count", Integer.toString(episodes != null ? episodes.length() : 0));
        put(p, "vote_average", str(season, "vote_average"));
        ExternalData data = new ExternalData(identifier, "/series/" + tvId + "/" + SEASON_PREFIX + seasonNumber, "jnt:tvSeason", p);
        data.setLazyI18nProperties(lazyI18n(SEASON_LAZY_I18N_PROPERTIES));
        return data;
    }

    private ExternalData getEpisodeData(String identifier, String tvId, String seasonNumber, String episodeNumber) throws RepositoryException, JSONException {
        JSONObject episode = episodeJson(tvId, seasonNumber, episodeNumber, DEFAULT_LANGUAGE);
        if (episode == null) {
            throw new ItemNotFoundException(identifier);
        }
        Map<String, String[]> p = new HashMap<>();
        put(p, "season_number", seasonNumber);
        put(p, "episode_number", episodeNumber);
        put(p, "air_date", date(str(episode, "air_date")));
        put(p, "runtime", str(episode, "runtime"));
        put(p, "vote_average", str(episode, "vote_average"));
        put(p, "still_path", imageUrl(stillSize, str(episode, "still_path")));
        ExternalData data = new ExternalData(identifier,
                "/series/" + tvId + "/" + SEASON_PREFIX + seasonNumber + "/" + EPISODE_PREFIX + episodeNumber, "jnt:tvEpisode", p);
        data.setLazyI18nProperties(lazyI18n(EPISODE_LAZY_I18N_PROPERTIES));
        return data;
    }

    private ExternalData getSeriesCreditData(String identifier, String tvId, String name) throws RepositoryException, JSONException {
        JSONObject credits = tvCreditsJson(tvId);
        Map<String, String[]> p = new HashMap<>();
        String path = "/series/" + tvId + "/" + name;
        if (name.startsWith(CAST_PREFIX)) {
            String personId = StringUtils.substringAfter(name, CAST_PREFIX);
            JSONObject cast = find(credits.optJSONArray("cast"), c -> personId.equals(str(c, "id")));
            if (cast != null) {
                JSONArray roles = cast.optJSONArray("roles");
                if (roles != null && roles.length() > 0) {
                    put(p, "character", str(roles.getJSONObject(0), "character"));
                }
                put(p, "order", str(cast, "order"));
                put(p, "episode_count", str(cast, "total_episode_count"));
                creditPerson(p, cast);
                return new ExternalData(identifier, path, "jnt:cast", p);
            }
        } else if (name.startsWith(CREW_PREFIX)) {
            String jobAndPerson = StringUtils.substringAfter(name, CREW_PREFIX);
            String job = StringUtils.substringBeforeLast(jobAndPerson, "_");
            String personId = StringUtils.substringAfterLast(jobAndPerson, "_");
            JSONObject crew = find(credits.optJSONArray("crew"), c -> personId.equals(str(c, "id")));
            JSONObject jobEntry = crew != null ? find(crew.optJSONArray("jobs"), j -> job.equals(str(j, "job"))) : null;
            if (jobEntry != null) {
                put(p, "job", job);
                put(p, "department", str(crew, "department"));
                put(p, "episode_count", str(jobEntry, "episode_count"));
                creditPerson(p, crew);
                return new ExternalData(identifier, path, "jnt:crew", p);
            }
        }
        throw new ItemNotFoundException(identifier);
    }

    private ExternalData getCollectionData(String identifier, String collectionId) throws RepositoryException, JSONException {
        JSONObject collection = collectionJson(collectionId, DEFAULT_LANGUAGE);
        Map<String, String[]> p = new HashMap<>();
        put(p, "backdrop_path", imageUrl(backdropSize, str(collection, "backdrop_path")));
        put(p, "tmdb_id", collectionId);
        List<JSONObject> parts = new ArrayList<>();
        JSONArray array = collection.optJSONArray("parts");
        for (int i = 0; array != null && i < array.length(); i++) {
            parts.add(array.getJSONObject(i));
        }
        // TMDB does not sort the parts; an empty release date (an announced film) goes last
        parts.sort(Comparator.comparing((JSONObject part) -> StringUtils.defaultIfEmpty(str(part, "release_date"), "9999")));
        putAll(p, "parts", parts.stream().map(part -> "movie-" + str(part, "id")).collect(Collectors.toList()));
        ExternalData data = new ExternalData(identifier, "/collections/" + collectionId, "jnt:movieCollection", p);
        data.setLazyI18nProperties(lazyI18n(COLLECTION_LAZY_I18N_PROPERTIES));
        return data;
    }

    private ExternalData getListData(String identifier) throws RepositoryException, JSONException {
        String listId = StringUtils.substringAfter(identifier, "lists-");
        if (!LIST_ID.matcher(listId).matches()) {
            throw new ItemNotFoundException(identifier);
        }
        JSONObject list = listJson(listId);
        Map<String, String[]> p = new HashMap<>();
        put(p, "jcr:title", str(list, "name"));
        put(p, "jcr:description", str(list, "description"));
        put(p, "poster_path", imageUrl(posterSize, str(list, "poster_path")));
        return new ExternalData(identifier, "/lists/" + listId, "jnt:moviesList", p);
    }

    private ExternalData getPersonData(String identifier, String personId) throws RepositoryException, JSONException {
        JSONObject person = json("person-" + personId, () -> api.get("/3/person/" + personId));
        Map<String, String[]> p = new HashMap<>();
        put(p, "name", str(person, "name"));
        put(p, "biography", str(person, "biography"));
        put(p, "homepage", str(person, "homepage"));
        put(p, "profile", imageUrl("h632", str(person, "profile_path")));
        put(p, "birthday", date(str(person, "birthday")));
        put(p, "deathday", date(str(person, "deathday")));
        return new ExternalData(identifier, "/persons/" + personId, "jnt:moviePerson", p);
    }

    private static ExternalData folder(String identifier, String path) {
        return new ExternalData(identifier, path, "jnt:contentFolder", new HashMap<>());
    }

    private static ExternalData reference(String identifier, String path, String target) {
        Map<String, String[]> p = new HashMap<>();
        p.put("j:node", new String[]{target});
        return new ExternalData(identifier, path, "jnt:contentReference", p);
    }

    private void creditPerson(Map<String, String[]> p, JSONObject credit) throws RepositoryException, JSONException {
        put(p, "name", str(credit, "name"));
        put(p, "person", "person-" + str(credit, "id"));
        put(p, "profile", imageUrl(profileSize, str(credit, "profile_path")));
    }

    /** Asks the search index to add a movie the first time it is read, as earlier versions did. */
    protected void sendIndexEvent(ExternalData data, String movieId) {
        if (!indexEvents || cache.get("indexedfullmovie-" + movieId) != null) {
            return;
        }
        EventService eventService = BundleUtils.getOsgiService(EventService.class, null);
        JCRStoreProvider jcrStoreProvider = JCRSessionFactory.getInstance().getProviders().get("TMDBProvider");
        if (eventService == null || jcrStoreProvider == null) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                eventService.sendAddedNodes(Collections.singletonList(data), jcrStoreProvider);
                cache.put("indexedfullmovie-" + movieId, "indexed");
            } catch (RepositoryException e) {
                logger.warn("Could not index movie {}", movieId, e);
            }
        });
    }

    static String getPathForMovie(JSONObject movie) {
        String releaseDate = str(movie, "release_date");
        if (StringUtils.isEmpty(releaseDate)) {
            return null;
        }
        return "/movies/" + StringUtils.substringBeforeLast(releaseDate, "-").replace("-", "/") + "/" + str(movie, "id");
    }

    @Override
    public Set<String> getSupportedNodeTypes() {
        return Set.of("jnt:contentFolder", "jnt:movie", "jnt:moviesList", "jnt:cast", "jnt:crew", "jnt:tvSeries", "jnt:movieCollection");
    }

    @Override
    public boolean isSupportsHierarchicalIdentifiers() {
        return false;
    }

    @Override
    public boolean isSupportsUuid() {
        return false;
    }

    @Override
    public boolean itemExists(String path) {
        return false;
    }

    // ------------------------------------------------------------------ lazy properties

    @Override
    public String[] getPropertyValues(String path, String propertyName) throws PathNotFoundException {
        return getI18nPropertyValues(path, DEFAULT_LANGUAGE, propertyName);
    }

    @Override
    public String[] getI18nPropertyValues(String path, String lang, String propertyName) throws PathNotFoundException {
        try {
            String value = i18nValue(path, lang, propertyName);
            // TMDB answers an empty text when a translation is missing: show the English one instead
            if (StringUtils.isEmpty(value) && !DEFAULT_LANGUAGE.equals(lang)) {
                value = i18nValue(path, DEFAULT_LANGUAGE, propertyName);
            }
            return StringUtils.isEmpty(value) ? new String[0] : new String[]{value};
        } catch (JSONException | RepositoryException e) {
            throw new PathNotFoundException(e);
        }
    }

    private String i18nValue(String path, String lang, String propertyName) throws RepositoryException, JSONException {
        String[] s = path.split("/");
        if (s.length == 5 && "movies".equals(s[1]) && isNumber(s[4])) {
            return field(movieJson(s[4], lang), propertyName, "title");
        }
        if (s.length >= 3 && "series".equals(s[1]) && isNumber(s[2])) {
            if (s.length == 3) {
                return field(tvJson(s[2], lang), propertyName, "name");
            }
            if (s[3].startsWith(SEASON_PREFIX)) {
                String season = StringUtils.substringAfter(s[3], SEASON_PREFIX);
                if (s.length == 4) {
                    return field(seasonJson(s[2], season, lang), propertyName, "name");
                }
                if (s.length == 5 && s[4].startsWith(EPISODE_PREFIX)) {
                    JSONObject episode = episodeJson(s[2], season, StringUtils.substringAfter(s[4], EPISODE_PREFIX), lang);
                    return episode != null ? field(episode, propertyName, "name") : null;
                }
            }
        }
        if (s.length == 3 && "collections".equals(s[1]) && isNumber(s[2])) {
            return field(collectionJson(s[2], lang), propertyName, "name");
        }
        return null;
    }

    private String field(JSONObject o, String propertyName, String titleField) throws RepositoryException, JSONException {
        switch (propertyName) {
            case "jcr:title":
                return str(o, titleField);
            case "poster_path":
                return imageUrl(posterSize, str(o, "poster_path"));
            default:
                return str(o, propertyName);
        }
    }

    @Override
    public Binary[] getBinaryPropertyValues(String path, String propertyName) throws PathNotFoundException {
        return new Binary[0];
    }

    // ------------------------------------------------------------------ search

    @Override
    public List<String> search(ExternalQuery query) throws RepositoryException {
        List<String> results = new ArrayList<>();
        String nodeType = QueryHelper.getNodeType(query.getSource());
        long limit = query.getLimit() > 0 ? Math.min(query.getLimit(), MAX_SEARCH_RESULTS) : TMDB_PAGE_SIZE;
        String language = StringUtils.defaultIfEmpty(query.getLanguage(), DEFAULT_LANGUAGE);
        try {
            if (isA("jnt:movie", nodeType)) {
                String title = titleConstraint(query);
                if (title != null) {
                    addMoviePaths(results, api.get("/3/search/movie", "query", title, "language", language).optJSONArray("results"), limit);
                } else if ("jnt:movie".equals(nodeType)) {
                    // Only a query on jnt:movie itself lists popular movies: a broad query on a site path
                    // must not page through TMDB
                    long page = query.getOffset() / TMDB_PAGE_SIZE + 1;
                    while (results.size() < limit && page <= query.getOffset() / TMDB_PAGE_SIZE + 5) {
                        JSONObject discover = api.get(API_DISCOVER_MOVIE, "sort_by", "popularity.desc", "page", Long.toString(page));
                        addMoviePaths(results, discover.optJSONArray("results"), limit);
                        if (page >= discover.optInt("total_pages", 0)) {
                            break;
                        }
                        page++;
                    }
                }
            }
            if ("jnt:tvSeries".equals(nodeType)) {
                String title = titleConstraint(query);
                if (title != null) {
                    addPaths(results, api.get("/3/search/tv", "query", title, "language", language).optJSONArray("results"), "/series/", limit);
                } else {
                    seriesIds.stream().limit(limit).forEach(id -> results.add("/series/" + id));
                }
            }
            if ("jnt:movieCollection".equals(nodeType)) {
                String title = titleConstraint(query);
                if (title != null) {
                    addPaths(results, api.get("/3/search/collection", "query", title, "language", language).optJSONArray("results"), "/collections/", limit);
                } else {
                    collectionIds.stream().limit(limit).forEach(id -> results.add("/collections/" + id));
                }
            }
            if (isA("jnt:moviesList", nodeType) && QueryHelper.getSimpleAndConstraints(query.getConstraint()).isEmpty()) {
                JSONArray lists = api.get("/3/account/" + getAccountId() + "/lists", "session_id", getSessionId()).getJSONArray("results");
                for (int i = 0; i < lists.length(); i++) {
                    results.add("/lists/" + str(lists.getJSONObject(i), "id"));
                }
            }
            if (isA("jnt:cast", nodeType)) {
                searchPersonCredits(query, "cast", results);
            }
            if (isA("jnt:crew", nodeType)) {
                searchPersonCredits(query, "crew", results);
            }
        } catch (UnsupportedRepositoryOperationException e) {
            // a constraint TMDB cannot answer: no result
        } catch (JSONException e) {
            throw new RepositoryException(e);
        }
        return results;
    }

    private void searchPersonCredits(ExternalQuery query, String kind, List<String> results) throws RepositoryException, JSONException {
        Map<String, Value> m = QueryHelper.getSimpleAndConstraints(query.getConstraint());
        if (!m.containsKey("id")) {
            return;
        }
        String personId = m.get("id").getString();
        JSONObject search = json("movie_credits_query_" + personId, () -> api.get("/3/person/" + personId + "/movie_credits"));
        JSONArray credits = search.optJSONArray(kind);
        for (int i = 0; credits != null && i < credits.length(); i++) {
            JSONObject credit = credits.getJSONObject(i);
            ExternalData movie = getItemByIdentifier("movie-" + str(credit, "id"));
            if (movie.getPath() == null) {
                continue;
            }
            String suffix = "cast".equals(kind) ? "_" + personId : "_" + str(credit, "job") + "_" + personId;
            for (String child : getChildren(movie.getPath())) {
                if (child.startsWith(kind + "_") && child.endsWith(suffix)) {
                    results.add(movie.getPath() + "/" + child);
                    if (results.size() >= TMDB_PAGE_SIZE) {
                        return;
                    }
                }
            }
        }
    }

    private static String titleConstraint(ExternalQuery query) throws RepositoryException {
        Map<String, Value> m = QueryHelper.getSimpleOrConstraints(query.getConstraint());
        return m.containsKey("jcr:title") ? m.get("jcr:title").getString() : null;
    }

    private static boolean isA(String type, String queriedType) {
        try {
            return NodeTypeRegistry.getInstance().getNodeType(type).isNodeType(queriedType);
        } catch (RepositoryException e) {
            return false;
        }
    }

    private void addMoviePaths(List<String> results, JSONArray movies, long limit) throws JSONException {
        for (int i = 0; movies != null && i < movies.length() && results.size() < limit; i++) {
            String path = getPathForMovie(movies.getJSONObject(i));
            if (path != null) {
                results.add(path);
            }
        }
    }

    private static void addPaths(List<String> results, JSONArray items, String prefix, long limit) throws JSONException {
        for (int i = 0; items != null && i < items.length() && results.size() < limit; i++) {
            results.add(prefix + str(items.getJSONObject(i), "id"));
        }
    }

    // ------------------------------------------------------------------ TMDB resources, cached

    @FunctionalInterface
    private interface JsonLoader {
        JSONObject load() throws RepositoryException, JSONException;
    }

    private JSONObject json(String key, JsonLoader loader) throws RepositoryException, JSONException {
        String cached = cache.get(key);
        if (cached != null) {
            return new JSONObject(cached);
        }
        if (api == null) {
            throw new RepositoryException("The TMDB provider has no API credentials");
        }
        JSONObject value = loader.load();
        cache.put(key, value.toString());
        return value;
    }

    private JSONObject movieJson(String movieId, String lang) throws RepositoryException, JSONException {
        return json("movie-" + lang + "-" + movieId, () -> api.get(API_MOVIE + movieId, "language", lang, "append_to_response", "keywords"));
    }

    private JSONObject movieCreditsJson(String movieId) throws RepositoryException, JSONException {
        return json("moviecredits-" + movieId, () -> api.get(API_MOVIE + movieId + "/credits"));
    }

    private JSONObject tvJson(String tvId, String lang) throws RepositoryException, JSONException {
        return json("tv-" + lang + "-" + tvId, () -> api.get(API_TV + tvId, "language", lang, "append_to_response", "keywords,external_ids"));
    }

    private JSONObject seasonJson(String tvId, String season, String lang) throws RepositoryException, JSONException {
        return json("tvseason-" + lang + "-" + tvId + "-" + season, () -> api.get(API_TV + tvId + "/season/" + season, "language", lang));
    }

    private JSONObject episodeJson(String tvId, String season, String episodeNumber, String lang) throws RepositoryException, JSONException {
        return find(seasonJson(tvId, season, lang).optJSONArray("episodes"), e -> episodeNumber.equals(str(e, "episode_number")));
    }

    private JSONObject tvCreditsJson(String tvId) throws RepositoryException, JSONException {
        return json("tvcredits-" + tvId, () -> api.get(API_TV + tvId + "/aggregate_credits"));
    }

    private JSONObject collectionJson(String collectionId, String lang) throws RepositoryException, JSONException {
        return json("collection-" + lang + "-" + collectionId, () -> api.get(API_COLLECTION + collectionId, "language", lang));
    }

    private JSONObject listJson(String listId) throws RepositoryException, JSONException {
        return json("list-" + listId, () -> api.get("/3/list/" + listId));
    }

    private String imageUrl(String size, String imagePath) throws RepositoryException, JSONException {
        if (StringUtils.isEmpty(imagePath)) {
            return null;
        }
        JSONObject images = json("configuration", () -> api.get("/3/configuration")).getJSONObject("images");
        String base = StringUtils.defaultIfEmpty(str(images, "secure_base_url"), str(images, "base_url"));
        return base + size + imagePath;
    }

    // ------------------------------------------------------------------ TMDB account lists (unchanged flow)

    private String getAccountId() throws RepositoryException, JSONException {
        if (accountId == null) {
            accountId = str(api.get("/3/account", "session_id", getSessionId()), "id");
        }
        return accountId;
    }

    private String getSessionId() throws RepositoryException {
        if (token != null && sessionId == null) {
            JSONObject session = api.get("/3/authentication/session/new", "request_token", token);
            token = null;
            sessionId = str(session, "session_id");
        }
        if (sessionId == null) {
            throw new RepositoryException("No open session");
        }
        return sessionId;
    }

    public String createToken() throws RepositoryException {
        token = str(api.get("/3/authentication/token/new"), "request_token");
        sessionId = null;
        return token;
    }

    // ------------------------------------------------------------------ helpers

    private Map<String, Set<String>> lazyI18n(Set<String> properties) {
        Map<String, Set<String>> lazy = new HashMap<>();
        for (String language : languages) {
            lazy.put(language, new HashSet<>(properties));
        }
        return lazy;
    }

    /** A value of a JSON object as text; null when it is missing, null or empty. */
    static String str(JSONObject o, String key) {
        if (o == null || !o.has(key) || o.isNull(key)) {
            return null;
        }
        String value = String.valueOf(o.opt(key));
        return value.isEmpty() || "null".equals(value) ? null : value;
    }

    private static String date(String isoDate) {
        return StringUtils.isEmpty(isoDate) ? null : isoDate + "T00:00:00.000+00:00";
    }

    private static void put(Map<String, String[]> p, String name, String value) {
        if (StringUtils.isNotEmpty(value)) {
            p.put(name, new String[]{value});
        }
    }

    private static void putAll(Map<String, String[]> p, String name, List<String> values) {
        if (!values.isEmpty()) {
            p.put(name, values.toArray(new String[0]));
        }
    }

    private static List<String> names(JSONArray array) throws JSONException {
        List<String> names = new ArrayList<>();
        for (int i = 0; array != null && i < array.length(); i++) {
            String name = str(array.getJSONObject(i), "name");
            if (name != null) {
                names.add(name);
            }
        }
        return names;
    }

    private static List<String> values(JSONArray array) {
        List<String> values = new ArrayList<>();
        for (int i = 0; array != null && i < array.length(); i++) {
            values.add(String.valueOf(array.opt(i)));
        }
        return values;
    }

    @FunctionalInterface
    private interface JsonPredicate {
        boolean test(JSONObject o) throws JSONException;
    }

    private static JSONObject find(JSONArray array, JsonPredicate predicate) throws JSONException {
        for (int i = 0; array != null && i < array.length(); i++) {
            JSONObject o = array.getJSONObject(i);
            if (predicate.test(o)) {
                return o;
            }
        }
        return null;
    }

    private static boolean isNumber(String s) {
        return s != null && NUMBER.matcher(s).matches();
    }

    private static String numberAfter(String identifier, String prefix) throws ItemNotFoundException {
        String id = StringUtils.substringAfter(identifier, prefix);
        if (!isNumber(id)) {
            throw new ItemNotFoundException(identifier);
        }
        return id;
    }

    private static String numberBefore(String identifier, String rest) throws ItemNotFoundException {
        String id = StringUtils.substringBefore(rest, "-");
        if (!isNumber(id)) {
            throw new ItemNotFoundException(identifier);
        }
        return id;
    }

    private static void requireNumbers(String identifier, String[] ids, int count) throws ItemNotFoundException {
        if (ids.length != count || !Arrays.stream(ids).allMatch(TMDBDataSource::isNumber)) {
            throw new ItemNotFoundException(identifier);
        }
    }

    private static List<String> splitList(String value) {
        if (StringUtils.isBlank(value)) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(",")).map(String::trim).filter(StringUtils::isNotEmpty).distinct().collect(Collectors.toList());
    }

    private static List<String> numbersOnly(List<String> ids) {
        List<String> numbers = ids.stream().filter(TMDBDataSource::isNumber).collect(Collectors.toList());
        if (numbers.size() != ids.size()) {
            logger.warn("Ignoring TMDB ids that are not numbers in {}", ids);
        }
        return numbers;
    }

    private static String firstNonEmpty(String first, String second) {
        return StringUtils.isNotEmpty(first) ? first : StringUtils.defaultString(second);
    }
}
