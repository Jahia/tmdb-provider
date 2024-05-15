package org.jahia.modules.tmdbprovider;

import com.google.common.collect.Sets;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.jahia.api.Constants;
import org.jahia.exceptions.JahiaInitializationException;
import org.jahia.modules.external.*;
import org.jahia.modules.external.events.EventService;
import org.jahia.modules.external.query.QueryHelper;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.cache.CacheProvider;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRStoreProvider;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.joda.time.DateTime;
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

import javax.jcr.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

@Component(service = { ExternalDataSource.class,
        TMDBDataSource.class }, immediate = true, configurationPid = "org.jahia.modules.tmdbprovider") @Designate(ocd = TMDBDataSource.Config.class) public class TMDBDataSource
        implements ExternalDataSource, ExternalDataSource.LazyProperty, ExternalDataSource.Searchable {

    @ObjectClassDefinition(name = "TMDB Provider", description = "A TMDB Provider configuration") public @interface Config {
        @AttributeDefinition(name = "TMDB API key", defaultValue = "", description = "The API key to use for The Movie Database") String apiKey() default "";

        @AttributeDefinition(name = "TMDB Mount path", defaultValue = "/sites/digitall/contents/tmdb", description = "The path at which to mount the database in the JCR") String mountPoint() default "/sites/digitall/contents/tmdb";

    }

    public static final String HOMEPAGE = "homepage";
    public static final String POSTER_PATH = "poster_path";
    public static final String MOVIES = "movies";
    public static final String PERSONS = "persons";
    public static final String BACKDROP_PATH = "backdrop_path";
    public static final String RELEASE_DATE = "release_date";
    public static final String ADULT = "adult";
    public static final String VOTE_AVERAGE = "vote_average";
    public static final String VOTE_COUNT = "vote_count";
    public static final String POPULARITY = "popularity";
    public static final String INDEXEDFULLMOVIECACHEKEYPREFIX = "indexedfullmovie-";
    public static final String JNT_MOVIE = "jnt:movie";
    public static final String CONFIGURATION = "configuration";
    public static final String MOVIE_CREDITS_QUERYCACHEKEYPREFIX = "movie_credits_query_";
    public static final String TMDB_CACHE = "tmdb-cache";
    public static final String BIRTHDAY = "birthday";
    public static final String DEATHDAY = "deathday";
    public static final String BIOGRAPHY = "biography";
    public static final String MOVIES_FOLDERCACHEKEY_PREFIX = "movies-folder-";
    public static final String RESULTS = "results";
    public static final String MOVIES_CREDITS = "movies-credits-";
    public static final String CAST = "cast_";
    public static final String CAST_ID = "cast_id";
    public static final String CREW = "crew_";
    public static final String CONTENT_FOLDER = "jnt:contentFolder";
    public static final String FOLDER = "-folder-";
    public static final String MOVIE_PREFIX = "movie-";
    public static final String MOVIECREDITS = "moviecredits-";
    public static final String IMAGES = "images";
    public static final String BASE_URL = "base_url";
    public static final String PROFILE_PATH = "profile_path";
    public static final String TIMESTAMP = "T00:00:00.000+00:00";
    public static final String CHARACTER = "character";
    public static final String JNT_CAST = "jnt:cast";
    public static final String ORDER = "order";
    public static final String DEPARTMENT = "department";
    public static final String JNT_CREW = "jnt:crew";
    public static final String PERSON_PREFIX = "person-";
    public static final String FULLMOVIE_PREFIX = "fullmovie-";
    public static final String KEYWORDS = "keywords";

    private static final Set<String> LAZY_PROPERTIES = Sets.newHashSet("original_title", HOMEPAGE, "status", "runtime", "imdb_id", "budget",
            "revenue");
    private static final Set<String> LAZY_I18N_PROPERTIES = Sets.newHashSet(Constants.JCR_TITLE, "overview", "tagline", POSTER_PATH);
    private static final Set<String> ROOT_NODES = Sets.newHashSet(MOVIES, PERSONS);
    private static final Logger logger = LoggerFactory.getLogger(TMDBDataSource.class);
    public static final int SOCKET_TIMEOUT = 60000;
    public static final int CONNECT_TIMEOUT = 15000;
    public static final int MAX_CONNECTIONS = 10;
    public static final int DEFAULT_MAX_PER_ROUTE = 2;

    private static String API_URL = "api.themoviedb.org";
    private static String API_CONFIGURATION = "/3/configuration";
    private static String API_MOVIE = "/3/movie/";
    private static final String API_PERSON = "/3/person/";
    private static String API_DISCOVER_MOVIE = "/3/discover/movie";
    private static String API_SEARCH_MOVIE = "/3/search/movie";
    private static String API_KEY = "api_key";

    private static Pattern YEAR_PATTERN = Pattern.compile("[0-9]{4,4}");
    private static Pattern DATE_PATTERN = Pattern.compile("[0-9]{4,4}/[0-9]{2,2}");

    private static final List<String> EXTENDABLE_TYPES = Arrays.asList("nt:base");

    private CacheProvider cacheProvider;
    private Cache cache;
    private String apiKeyValue = "";

    private HttpClient httpClient;

    private ExternalContentStoreProviderFactory externalContentStoreProviderFactory;

    private ExternalContentStoreProvider externalContentStoreProvider;

    public TMDBDataSource() {
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Reference
    public void setCacheProvider(CacheProvider cacheProvider) {
        this.cacheProvider = cacheProvider;
    }

    public void setApiKeyValue(String apiKeyValue) {
        if (apiKeyValue.startsWith("'")) {
            this.apiKeyValue = apiKeyValue.replace("'", "");
        } else {
            this.apiKeyValue = apiKeyValue;
        }
    }

    @Reference
    public void setExternalContentStoreProviderFactory(ExternalContentStoreProviderFactory externalContentStoreProviderFactory) {
        this.externalContentStoreProviderFactory = externalContentStoreProviderFactory;
    }

    @Activate public void start(Config config) throws RepositoryException {
        if (StringUtils.isEmpty(config.apiKey())) {
            logger.warn("API key is not set, TMDB provider will not initialize.");
            return;
        }
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(SOCKET_TIMEOUT).setConnectTimeout(CONNECT_TIMEOUT)
                .setConnectionRequestTimeout(CONNECT_TIMEOUT).build();

        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory()).build();

        PoolingHttpClientConnectionManager httpConnectionManager = new PoolingHttpClientConnectionManager(registry);
        httpConnectionManager.setMaxTotal(MAX_CONNECTIONS);
        httpConnectionManager.setDefaultMaxPerRoute(DEFAULT_MAX_PER_ROUTE);

        httpClient = HttpClients.custom().setConnectionManager(httpConnectionManager).setDefaultRequestConfig(requestConfig)
                .disableCookieManagement().build();
        this.apiKeyValue = config.apiKey();
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

        try {
            if (!cacheProvider.getCacheManager().cacheExists(TMDB_CACHE)) {
                cacheProvider.getCacheManager().addCache(TMDB_CACHE);
            }
            cache = cacheProvider.getCacheManager().getCache(TMDB_CACHE);
        } catch (IllegalStateException | CacheException e) {
            logger.error("Error while initializing cache for IMDB", e);
        }
    }

    @Deactivate public void stop() {
        if (httpClient != null) {
            httpClient = null;
        }
        if (externalContentStoreProvider != null) {
            externalContentStoreProvider.stop();
        }
    }

    /**
     * @param path path where to get children
     * @return list of paths as String
     */
    @Override public List<String> getChildren(String path) throws RepositoryException {
        DateTime currentDate = new DateTime();
        int yearLimit = currentDate.getYear();
        int monthLimit = currentDate.getMonthOfYear() - 1;

        List<String> r = new ArrayList<>();

        String[] splitPath = path.split("/");
        try {
            if (splitPath.length == 0) {
                r.addAll(ROOT_NODES);
            } else if (splitPath[1].equals(MOVIES)) {
                getMovieSplit(splitPath, r, yearLimit, monthLimit);
            }
        } catch (JSONException e) {
            logger.error("Error while parsing json {}", e.getMessage(), e);
        }

        return r;
    }

    private void getMovieSplit(String[] splitPath, List<String> r, int yearLimit, int monthLimit)
            throws RepositoryException, JSONException {
        switch (splitPath.length) {
            case 2:
                getYearsAsStrings(r, yearLimit);
                return;
            case 3:
                if (splitPath[2].equals(Integer.toString(yearLimit))) {
                    getMonths(r, monthLimit);
                } else {
                    getMonths(r, 12);
                }
                break;
            case 4:
                getMoviesForDate(r, splitPath[2], splitPath[3]);
                break;
            case 5:
                String movieID = splitPath[4];
                getMovieCredits(movieID, r);
                break;
            default:
        }
    }

    private void getMovieCredits(String movieID, List<String> r) throws JSONException, RepositoryException {
        String movieCreditsKey = MOVIES_CREDITS + movieID;
        JSONObject o;
        if (cache.get(movieCreditsKey) != null) {
            o = new JSONObject((String) cache.get(movieCreditsKey).getObjectValue());
        } else {
            o = queryTMDB(API_MOVIE + movieID + "/credits");
            cache.put(new Element(movieCreditsKey, o.toString()));
        }
        if (o.has("cast")) {
            JSONArray result = o.getJSONArray("cast");
            for (int i = 0; i < result.length(); i++) {
                JSONObject cast = result.getJSONObject(i);
                r.add(CAST + cast.getString(CAST_ID) + "_" + cast.getString("id"));
            }
        }
        if (o.has("crew")) {
            JSONArray result = o.getJSONArray("crew");
            for (int i = 0; i < result.length(); i++) {
                JSONObject crew = result.getJSONObject(i);
                if (!r.contains(CREW + crew.getString("id"))) {
                    r.add(CREW + crew.getString("job") + "_" + crew.getString("id"));
                }
            }
        }
    }

    private void getMoviesForDate(List<String> r, String year, String month) throws RepositoryException, JSONException {
        String date = year + "-" + month;
        if (cache.get(MOVIES_FOLDERCACHEKEY_PREFIX + date) != null) {
            r.addAll((List<String>) cache.get(MOVIES_FOLDERCACHEKEY_PREFIX + date).getObjectValue());
        } else {
            Calendar instance = Calendar.getInstance();
            instance.set(Calendar.YEAR, Integer.valueOf(year));
            instance.set(Calendar.MONTH, Integer.valueOf(month));
            instance.set(Calendar.DAY_OF_MONTH, 1);
            instance.roll(Calendar.DAY_OF_MONTH, false);
            JSONObject o = queryTMDB(API_DISCOVER_MOVIE, "primary_release_date.gte", date + "-01", "primary_release_date.lte",
                    date + "-" + instance.get(Calendar.DAY_OF_MONTH));
            JSONArray result = o.getJSONArray(RESULTS);
            for (int i = 0; i < result.length(); i++) {
                JSONObject movie = result.getJSONObject(i);
                r.add(Integer.toString(movie.getInt("id")));
                cache.put(new Element(MOVIE_PREFIX + movie.getInt("id"), movie.toString()));
            }
            cache.put(new Element(MOVIES_FOLDERCACHEKEY_PREFIX + date, r));
        }
    }

    private void getMonths(List<String> r, int monthLimit) {
        for (int i = 1; i <= monthLimit; i++) {
            r.add(StringUtils.leftPad(Integer.toString(i), 2, "0"));
        }
    }

    private void getYearsAsStrings(List<String> r, int yearLimit) {
        for (int i = yearLimit; i >= 1900; i--) {
            r.add(Integer.toString(i));
        }
    }

    /**
     * identifier is unique for an ExternalData
     *
     * @param identifier
     * @return ExternalData defined by the identifier
     * @throws javax.jcr.ItemNotFoundException
     */
    @Override public ExternalData getItemByIdentifier(String identifier) throws ItemNotFoundException {
        try {
            if (identifier.equals("root")) {
                return getRootData(identifier, "TMDB", "/");
            }
            if (identifier.contains("-rootfolder")) {
                ExternalData identifier1 = getRootFolderData(identifier);
                if (identifier1 != null)
                    return identifier1;
            } else if (identifier.contains(FOLDER)) {
                ExternalData identifier1 = getDateFolderData(identifier);
                if (identifier1 != null)
                    return identifier1;
            } else if (identifier.startsWith(MOVIE_PREFIX)) {
                String movieId = getIdentifier(identifier, MOVIE_PREFIX);
                return getMovieData(identifier, movieId);
            } else if (identifier.startsWith(MOVIECREDITS)) {
                String movieId = StringUtils.substringAfter(identifier, MOVIECREDITS);
                String creditsId = StringUtils.substringAfter(movieId, "-");
                movieId = StringUtils.substringBefore(movieId, "-");

                ExternalData data = getMovieCreditsData(identifier, movieId, creditsId);
                if (data != null)
                    return data;
            } else if (identifier.startsWith("movieref-")) {
                String movieId = StringUtils.substringAfter(identifier, "movieref-");
                String listId = StringUtils.substringBefore(movieId, "-");
                movieId = StringUtils.substringAfter(movieId, "-");
                return getMovieRefData(identifier, movieId, listId);
            } else if (identifier.startsWith(PERSON_PREFIX)) {
                String personId = getIdentifier(identifier, PERSON_PREFIX);
                return getPersonData(identifier, personId);
            }
        } catch (Exception e) {
            throw new ItemNotFoundException(e);
        }

        throw new ItemNotFoundException(identifier);
    }

    private ExternalData getMovieRefData(String identifier, String movieId, String listId) throws ItemNotFoundException {
        try {
            Integer.parseInt(movieId);
        } catch (NumberFormatException e) {
            throw new ItemNotFoundException(identifier);
        }
        if (!Pattern.compile("[a-z0-9]+").matcher(listId).matches()) {
            throw new ItemNotFoundException(identifier);
        }

        Map<String, String[]> properties = new HashMap<>();
        properties.put("j:node", new String[] { MOVIE_PREFIX + movieId });
        return new ExternalData(identifier, "/lists/" + listId + "/" + movieId, "jnt:contentReference", properties);
    }

    private ExternalData getPersonData(String identifier, String personId) throws JSONException, RepositoryException {
        JSONObject person;
        if (cache.get(PERSON_PREFIX + personId) != null) {
            person = new JSONObject((String) cache.get(PERSON_PREFIX + personId).getObjectValue());
        } else {
            person = queryTMDB(API_PERSON + personId);
            cache.put(new Element(PERSON_PREFIX + personId, person.toString()));
        }

        JSONObject configuration = getConfiguration();
        String baseUrl = configuration.getJSONObject(IMAGES).getString(BASE_URL);

        Map<String, String[]> properties = new HashMap<>();
        if (person.getString("name") != null)
            properties.put("name", new String[] { person.getString("name") });
        if (person.getString(BIOGRAPHY) != null)
            properties.put(BIOGRAPHY, new String[] { person.getString(BIOGRAPHY) });
        if (!StringUtils.isEmpty(person.getString(HOMEPAGE)) && !person.getString(HOMEPAGE).equals("null"))
            properties.put(HOMEPAGE, new String[] { person.getString(HOMEPAGE) });
        if (!StringUtils.isEmpty(person.getString(PROFILE_PATH)) && !person.getString(PROFILE_PATH).equals("null"))
            properties.put("profile", new String[] {
                    baseUrl + configuration.getJSONObject(IMAGES).getJSONArray("profile_sizes").get(2) + person.getString(PROFILE_PATH) });
        if (!StringUtils.isEmpty(person.getString(BIRTHDAY)) && !person.getString(BIRTHDAY).equals("null"))
            properties.put(BIRTHDAY, new String[] { person.getString(BIRTHDAY) + TIMESTAMP });
        if (!StringUtils.isEmpty(person.getString(DEATHDAY)) && !person.getString(DEATHDAY).equals("null"))
            properties.put(DEATHDAY, new String[] { person.getString(DEATHDAY) + TIMESTAMP });

        return new ExternalData(identifier, "/persons/" + personId, "jnt:moviePerson", properties);
    }

    private ExternalData getMovieCreditsData(String identifier, String movieId, String creditsId)
            throws JSONException, RepositoryException {
        ExternalData movie = getItemByIdentifier(MOVIE_PREFIX + movieId);

        JSONObject o;
        if (cache.get(MOVIES_CREDITS + movieId) != null) {
            o = new JSONObject((String) cache.get(MOVIES_CREDITS + movieId).getObjectValue());
        } else {
            o = queryTMDB(API_MOVIE + movieId + "/credits");
            cache.put(new Element(MOVIES_CREDITS + movieId, o.toString()));
        }

        if (creditsId.startsWith(CREW)) {
            return getCrewData(identifier, creditsId, movie, o);
        } else if (creditsId.startsWith(CAST)) {
            return getCastData(identifier, creditsId, movie, o);
        }
        return null;
    }

    private void addNameAndProfile(JSONObject credit, Map<String, String[]> properties) throws JSONException, RepositoryException {
        JSONObject configuration = getConfiguration();
        String baseUrl = configuration.getJSONObject(IMAGES).getString(BASE_URL);
        if (credit.getString("name") != null)
            properties.put("name", new String[] { credit.getString("name") });
        if (!StringUtils.isEmpty(credit.getString(PROFILE_PATH)) && !credit.getString(PROFILE_PATH).equals("null"))
            properties.put("profile", new String[] {
                    baseUrl + configuration.getJSONObject(IMAGES).getJSONArray("profile_sizes").get(1) + credit.getString(PROFILE_PATH) });
    }

    private ExternalData getCastData(String identifier, String creditsId, ExternalData movie, JSONObject o)
            throws JSONException, RepositoryException {
        String id = StringUtils.substringAfter(creditsId, CAST);
        String castId = StringUtils.substringBefore(id, "_");
        id = StringUtils.substringAfter(id, "_");
        JSONArray a = o.getJSONArray("cast");
        for (int i = 0; i < a.length(); i++) {
            JSONObject cast = a.getJSONObject(i);
            if (cast.getString("id").equals(id) && cast.getString(CAST_ID).equals(castId)) {
                Map<String, String[]> properties = new HashMap<>();
                ExternalData data = new ExternalData(identifier, movie.getPath() + "/" + creditsId, JNT_CAST, properties);
                parseCast(cast, properties);
                return data;
            }
        }
        return null;
    }

    private void parseCast(JSONObject cast, Map<String, String[]> properties) throws JSONException, RepositoryException {
        if (cast.getString(CHARACTER) != null)
            properties.put(CHARACTER, new String[] { cast.getString(CHARACTER) });
        if (cast.getString(ORDER) != null)
            properties.put(ORDER, new String[] { cast.getString(ORDER) });
        if (cast.getString(CAST_ID) != null)
            properties.put(CAST_ID, new String[] { cast.getString(CAST_ID) });
        if (cast.getString("id") != null)
            properties.put("person", new String[] { PERSON_PREFIX + cast.getString("id") });
        addNameAndProfile(cast, properties);
    }

    private ExternalData getCrewData(String identifier, String creditsId, ExternalData movie, JSONObject o)
            throws JSONException, RepositoryException {
        String id = StringUtils.substringAfter(creditsId, CREW);
        String job = StringUtils.substringBefore(id, "_");
        id = StringUtils.substringAfter(id, "_");
        JSONArray a = o.getJSONArray("crew");
        for (int i = 0; i < a.length(); i++) {
            JSONObject crew = a.getJSONObject(i);
            if (crew.getString("id").equals(id) && crew.getString("job").equals(job)) {
                Map<String, String[]> properties = new HashMap<>();
                ExternalData data = new ExternalData(identifier, movie.getPath() + "/" + creditsId, JNT_CREW, properties);
                if (crew.getString(DEPARTMENT) != null)
                    properties.put(DEPARTMENT, new String[] { crew.getString(DEPARTMENT) });
                if (crew.getString("job") != null)
                    properties.put("job", new String[] { crew.getString("job") });
                if (crew.getString("id") != null)
                    properties.put("person", new String[] { PERSON_PREFIX + crew.getString("id") });
                addNameAndProfile(crew, properties);
                return data;
            }
        }
        return null;
    }

    private String getIdentifier(String identifier, String identifierPrefix) throws ItemNotFoundException {
        String id = StringUtils.substringAfter(identifier, identifierPrefix);
        try {
            Integer.parseInt(id);
        } catch (NumberFormatException e) {
            throw new ItemNotFoundException(identifier);
        }
        return id;
    }

    private ExternalData getRootFolderData(String identifier) {
        final String s = StringUtils.substringBefore(identifier, "-rootfolder");
        if (ROOT_NODES.contains(s)) {
            return getRootData(identifier, StringUtils.capitalize(s), "/" + s);
        }
        return null;
    }

    private ExternalData getDateFolderData(String identifier) {
        final String s = StringUtils.substringBefore(identifier, FOLDER);
        final String date = StringUtils.substringAfter(identifier, FOLDER);
        if (ROOT_NODES.contains(s) && (YEAR_PATTERN.matcher(date).matches() || DATE_PATTERN.matcher(date).matches())) {
            Map<String, String[]> properties = new HashMap<>();
            properties.put(Constants.JCR_TITLE, new String[] { date });
            return new ExternalData(identifier, "/" + s + "/" + date, CONTENT_FOLDER, properties);
        }
        return null;
    }

    private ExternalData getRootData(String identifier, String tmdb, String s2) {
        Map<String, String[]> properties = new HashMap<>();
        properties.put(Constants.JCR_TITLE, new String[] { tmdb });
        return new ExternalData(identifier, s2, CONTENT_FOLDER, properties);
    }

    private ExternalData getMovieData(String identifier, String movieId) throws JSONException, RepositoryException {
        JSONObject movie;

        String lang = "en";
        if (cache.get(MOVIE_PREFIX + movieId) != null) {
            movie = new JSONObject((String) cache.get(MOVIE_PREFIX + movieId).getObjectValue());
        } else if (cache.get(FULLMOVIE_PREFIX + lang + "-" + movieId) != null) {
            movie = new JSONObject((String) cache.get(FULLMOVIE_PREFIX + lang + "-" + movieId).getObjectValue());
        } else {
            try {
                movie = queryTMDB(API_MOVIE + movieId, "language", lang);
                JSONObject keywords = queryTMDB(API_MOVIE + movieId + "/keywords");
                if (keywords.has(KEYWORDS)) {
                    movie.put(KEYWORDS, keywords.getJSONArray(KEYWORDS));
                }
                cache.put(new Element(FULLMOVIE_PREFIX + lang + "-" + movieId, movie.toString()));
            } catch (RepositoryException | JSONException | IllegalArgumentException | IllegalStateException | CacheException e) {
                logger.error("Error while getting movie", e);
                movie = new JSONObject();
            }
        }

        Map<String, String[]> properties = null;
        try {
            JSONObject configuration = getConfiguration();
            String baseUrl = configuration.getJSONObject(IMAGES).getString(BASE_URL);

            properties = new HashMap<>();
            parseMovie(movie, properties, configuration, baseUrl);
            getArrayPropsAsMultiValue(movie, properties, "genres", "j:tagList");
            //Get keywords
            getArrayPropsAsMultiValue(movie, properties, KEYWORDS, "j:keywords");
        } catch (JSONException | RepositoryException e) {
            logger.error("Error while getting movie", e);
        }
        ExternalData data = new ExternalData(identifier, getPathForMovie(movie), JNT_MOVIE, properties);

        data.setLazyProperties(new HashSet<>(LAZY_PROPERTIES));

        Map<String, Set<String>> lazy18 = new HashMap<>();
        lazy18.put("en", new HashSet<>(LAZY_I18N_PROPERTIES));
        lazy18.put("fr", new HashSet<>(LAZY_I18N_PROPERTIES));
        data.setLazyI18nProperties(lazy18);
        if (cache.get(INDEXEDFULLMOVIECACHEKEYPREFIX + movieId) == null) {
            EventService eventService = BundleUtils.getOsgiService(EventService.class, null);
            JCRStoreProvider jcrStoreProvider = JCRSessionFactory.getInstance().getProviders().get("TMDBProvider");
            CompletableFuture.supplyAsync(() -> {
                try {
                    eventService.sendAddedNodes(Arrays.asList(data), jcrStoreProvider);
                    cache.put(new Element(INDEXEDFULLMOVIECACHEKEYPREFIX + movieId, "indexed"));
                } catch (RepositoryException e) {
                    e.printStackTrace();
                }
                return "eventSent";
            });
        }
        return data;
    }

    private void getArrayPropsAsMultiValue(JSONObject movie, Map<String, String[]> properties, String genres2, String s)
            throws JSONException {
        if (movie.has(genres2)) {
            JSONArray genres = movie.getJSONArray(genres2);
            String[] strings = new String[genres.length()];
            for (int i = 0; i < genres.length(); i++) {
                JSONObject genre = genres.getJSONObject(i);
                strings[i] = genre.getString("name");
            }
            properties.put(s, strings);
        }
    }

    private void parseMovie(JSONObject movie, Map<String, String[]> properties, JSONObject configuration, String baseUrl)
            throws JSONException {
        if (movie.has(BACKDROP_PATH) && !movie.getString(BACKDROP_PATH).equals("null"))
            properties.put(BACKDROP_PATH, new String[] {
                    baseUrl + configuration.getJSONObject(IMAGES).getJSONArray("backdrop_sizes").get(1) + movie.getString(BACKDROP_PATH) });
        if (movie.has(RELEASE_DATE)) {
            properties.put(RELEASE_DATE, new String[] { movie.getString(RELEASE_DATE) + TIMESTAMP });
        }
        if (movie.has(ADULT)) {
            properties.put(ADULT, new String[] { Boolean.toString(movie.getBoolean(ADULT)) });
        }
        if (movie.has(VOTE_AVERAGE)) {
            properties.put(VOTE_AVERAGE, new String[] { Float.toString(movie.getFloat(VOTE_AVERAGE)) });
        }
        if (movie.has(VOTE_COUNT)) {
            properties.put(VOTE_COUNT, new String[] { Integer.toString(movie.getInt(VOTE_COUNT)) });
        }
        if (movie.has(POPULARITY)) {
            properties.put(POPULARITY, new String[] { Float.toString(movie.getFloat(POPULARITY)) });
        }
    }

    private String getPathForMovie(JSONObject movie) throws JSONException {
        if (!movie.has(RELEASE_DATE) || StringUtils.isEmpty(movie.getString(RELEASE_DATE))) {
            return null;
        }
        return "/movies/" + StringUtils.substringBeforeLast(movie.getString(RELEASE_DATE), "-").replace("-", "/") + "/" + movie.getString(
                "id");
    }

    /**
     * As getItemByIdentifier, get an ExternalData by its path
     *
     * @param path
     * @return ExternalData
     * @throws javax.jcr.PathNotFoundException
     */
    @Override public ExternalData getItemByPath(String path) throws PathNotFoundException {
        String[] splitPath = path.split("/");
        try {
            if (path.endsWith("j:acl")) {
                throw new PathNotFoundException(path);
            }
            if (splitPath.length <= 1) {
                return getItemByIdentifier("root");
            } else if (splitPath[1].equals(MOVIES)) {
                switch (splitPath.length) {
                    case 3:
                        return getItemByIdentifier(MOVIES_FOLDERCACHEKEY_PREFIX + splitPath[2]);
                    case 4:
                        return getItemByIdentifier(MOVIES_FOLDERCACHEKEY_PREFIX + splitPath[2] + "/" + splitPath[3]);
                    case 5:
                        return getItemByIdentifier(MOVIE_PREFIX + splitPath[4]);
                    case 6:
                        return getItemByIdentifier(MOVIECREDITS + splitPath[4] + "-" + splitPath[5]);
                    default:
                        return getItemByIdentifier("movies-rootfolder");
                }
            } else if (splitPath[1].equals(PERSONS)) {
                if (splitPath.length == 2) {
                    return getItemByIdentifier("persons-rootfolder");
                } else if (splitPath.length == 3) {
                    return getItemByIdentifier(PERSON_PREFIX + splitPath[2]);
                }
            }
        } catch (ItemNotFoundException e) {
            throw new PathNotFoundException(e);
        }
        throw new PathNotFoundException();
    }

    /**
     * Returns a set of supported node types.
     *
     * @return a set of supported node types
     */
    @Override public Set<String> getSupportedNodeTypes() {
        return Sets.newHashSet(CONTENT_FOLDER, JNT_MOVIE, "jnt:moviesList", JNT_CAST, JNT_CREW);
    }

    /**
     * Indicates if this data source has path-like hierarchical external identifiers, e.g. IDs that are using file system paths.
     *
     * @return <code>true</code> if this data source has path-like hierarchical external identifiers, e.g. IDs that are using file system
     * paths; <code>false</code> otherwise.
     */
    @Override public boolean isSupportsHierarchicalIdentifiers() {
        return false;
    }

    /**
     * Indicates if the data source supports UUIDs.
     *
     * @return <code>true</code> if the data source supports UUIDs
     */
    @Override public boolean isSupportsUuid() {
        return false;
    }

    /**
     * Returns <code>true</code> if an item exists at <code>path</code>; otherwise returns <code>false</code>.
     *
     * @param path item path
     * @return <code>true</code> if an item exists at <code>path</code>; otherwise returns <code>false</code>
     */
    @Override public boolean itemExists(String path) {
        return false;
    }

    private JSONObject queryTMDB(String path, String... params) throws RepositoryException {
        try {
            URIBuilder builder = new URIBuilder().setScheme("http").setHost(API_URL).setPath(path).setParameter(API_KEY, apiKeyValue);

            for (int i = 0; i < params.length; i += 2) {
                builder.setParameter(params[i], params[i + 1]);
            }

            URI uri = builder.build();

            long l = System.currentTimeMillis();
            HttpGet getMethod = new HttpGet(uri);
            getMethod.setHeader("Authorization", "Bearer " + apiKeyValue);
            getMethod.setHeader("Content-Type", "application/json;charset=utf-8");
            CloseableHttpResponse resp = null;

            try {
                resp = (CloseableHttpResponse) httpClient.execute(getMethod);
                return new JSONObject(EntityUtils.toString(resp.getEntity()));

            } finally {
                if (resp != null) {
                    resp.close();
                }
                logger.debug("Request {} executed in {} ms", uri.toString(), (System.currentTimeMillis() - l));
            }
        } catch (Exception e) {
            throw new RepositoryException(e);
        }
    }

    @Override public String[] getPropertyValues(String path, String propertyName) throws PathNotFoundException {
        return getI18nPropertyValues(path, "en", propertyName);
    }

    @Override public String[] getI18nPropertyValues(String path, String lang, String propertyName) throws PathNotFoundException {
        try {
            JSONObject movie;
            if (path.startsWith("/movies")) {
                String movieId = StringUtils.substringAfterLast(path, "/");
                if (cache.get(FULLMOVIE_PREFIX + lang + "-" + movieId) != null) {
                    movie = new JSONObject((String) cache.get(FULLMOVIE_PREFIX + lang + "-" + movieId).getObjectValue());
                } else {
                    movie = queryTMDB(API_MOVIE + movieId, "language", lang);
                    cache.put(new Element(FULLMOVIE_PREFIX + lang + "-" + movieId, movie.toString()));
                }
                if (propertyName.equals(Constants.JCR_TITLE) && movie.has("title")) {
                    return new String[] { movie.getString("title") };
                } else if (propertyName.equals(POSTER_PATH) && movie.has(POSTER_PATH)) {
                    JSONObject configuration = getConfiguration();
                    String baseUrl = configuration.getJSONObject(IMAGES).getString(BASE_URL);
                    return new String[] {
                            baseUrl + configuration.getJSONObject(IMAGES).getJSONArray("poster_sizes").get(1) + movie.getString(
                                    propertyName) };
                } else if (movie.has(propertyName) && !movie.getString(propertyName).equals("null")) {
                    return new String[] { movie.getString(propertyName) };
                } else if (propertyName.equals("runtime")) {
                    return new String[] { "0" };
                }
                return new String[] { "" };
            }
        } catch (JSONException | RepositoryException e) {
            throw new PathNotFoundException(e);
        }
        return new String[0];
    }

    @Override public Binary[] getBinaryPropertyValues(String path, String propertyName) throws PathNotFoundException {
        return new Binary[0];
    }

    public JSONObject getConfiguration() throws JSONException, RepositoryException {
        JSONObject configuration;
        if (cache.get(CONFIGURATION) != null) {
            configuration = new JSONObject((String) cache.get(CONFIGURATION).getObjectValue());
        } else {
            configuration = queryTMDB(API_CONFIGURATION);
            cache.put(new Element(CONFIGURATION, configuration.toString()));
        }

        return configuration;
    }

    @Override public List<String> search(ExternalQuery query) throws RepositoryException {
        List<String> results = new ArrayList<>();
        String nodeType = QueryHelper.getNodeType(query.getSource());

        try {
            if (NodeTypeRegistry.getInstance().getNodeType(JNT_MOVIE).isNodeType(nodeType)) {
                searchMovie(query, results);
            } else if (NodeTypeRegistry.getInstance().getNodeType(JNT_CAST).isNodeType(nodeType)) {
                List<String> results1 = searchCast(query, results);
                if (results1 != null)
                    return results1;
            } else if (NodeTypeRegistry.getInstance().getNodeType(JNT_CREW).isNodeType(nodeType)) {
                List<String> results1 = searchCrew(query, results);
                if (results1 != null)
                    return results1;
            }
        } catch (UnsupportedRepositoryOperationException e) {
            //
        } catch (JSONException e) {
            throw new RepositoryException(e);
        }
        return results;
    }

    private List<String> searchCrew(ExternalQuery query, List<String> results) throws RepositoryException, JSONException {
        Map<String, Value> m = QueryHelper.getSimpleAndConstraints(query.getConstraint());
        if (m.containsKey("id")) {
            final String id = m.get("id").getString();
            JSONObject search;
            if (cache.get(MOVIE_CREDITS_QUERYCACHEKEYPREFIX + id) != null) {
                search = new JSONObject((String) cache.get(MOVIE_CREDITS_QUERYCACHEKEYPREFIX + id).getObjectValue());
            } else {
                search = queryTMDB(API_PERSON + id + "/movie_credits");
                cache.put(new Element(MOVIE_CREDITS_QUERYCACHEKEYPREFIX + id, search.toString()));
            }

            return processCrewResults(results, id, search);
        }
        return null;
    }

    private List<String> processCrewResults(List<String> results, String id, JSONObject search) throws JSONException, RepositoryException {
        JSONArray result = search.getJSONArray("crew");
        for (int i = 0; i < result.length(); i++) {
            JSONObject r = result.getJSONObject(i);
            ExternalData d = getItemByIdentifier(MOVIE_PREFIX + r.getString("id"));
            if (d != null) {
                for (String s : getChildren(d.getPath())) {
                    if (s.endsWith("_" + r.getString("job") + "_" + id)) {
                        results.add(d.getPath() + "/" + s);
                        if (results.size() == 20) {
                            return results;
                        }
                    }
                }
            }
        }
        return null;
    }

    private List<String> searchCast(ExternalQuery query, List<String> results) throws RepositoryException, JSONException {
        Map<String, Value> m = QueryHelper.getSimpleAndConstraints(query.getConstraint());
        if (m.containsKey("id")) {
            final String id = m.get("id").getString();
            JSONObject search;
            if (cache.get(MOVIE_CREDITS_QUERYCACHEKEYPREFIX + id) != null) {
                search = new JSONObject((String) cache.get(MOVIE_CREDITS_QUERYCACHEKEYPREFIX + id).getObjectValue());
            } else {
                search = queryTMDB(API_PERSON + id + "/movie_credits");
                cache.put(new Element(MOVIE_CREDITS_QUERYCACHEKEYPREFIX + id, search.toString()));
            }

            return processCastResults(results, id, search);
        } else {
            long pageNumber = query.getOffset();
            if (pageNumber < 1500) {
                Set<String> processedMovies = new HashSet<>();
                cache.getKeys().stream().filter(o -> o.toString().startsWith(MOVIE_PREFIX) || o.toString().startsWith(FULLMOVIE_PREFIX))
                        .sorted(Comparator.naturalOrder()).skip(query.getOffset()).forEach(o -> {
                            String str = o.toString();
                            String movieID = null;
                            if (str.startsWith(MOVIE_PREFIX)) {
                                movieID = StringUtils.substringAfter(str, MOVIE_PREFIX);
                            } else {
                                movieID = StringUtils.substringAfterLast(str, "-");
                            }
                            if (!processedMovies.contains(movieID) && results.size() < query.getLimit()) {
                                processedMovies.add(movieID);
                                try {
                                    List<String> credits = new ArrayList<>();
                                    String pathForMovie = getPathForMovie(new JSONObject((String) cache.get(o).getObjectValue()));
                                    getMovieCredits(movieID, credits);
                                    credits.forEach(s -> {
                                        if (s.startsWith(CAST) && results.size() < query.getLimit()) {
                                            results.add(pathForMovie + "/" + s);
                                        }
                                    });
                                } catch (JSONException e) {
                                    throw new RuntimeException(e);
                                } catch (RepositoryException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });
            }
        }
        logger.info("returning cast members {}", results.size());
        for (String result : results) {
            logger.info(result);
        }
        return results;
    }

    private List<String> processCastResults(List<String> results, String id, JSONObject search) throws JSONException, RepositoryException {
        JSONArray result = search.getJSONArray("cast");
        for (int i = 0; i < result.length(); i++) {
            JSONObject r = result.getJSONObject(i);
            ExternalData d = getItemByIdentifier(MOVIE_PREFIX + r.getString("id"));
            if (d != null) {
                for (String s : getChildren(d.getPath())) {
                    if (s.endsWith(id)) {
                        results.add(d.getPath() + "/" + s);
                        if (results.size() == 20) {
                            return results;
                        }
                    }
                }
            }
        }
        return null;
    }

    private void searchMovie(ExternalQuery query, List<String> results) throws RepositoryException, JSONException {
        JSONArray tmdbResult = null;

        Map<String, Value> m = QueryHelper.getSimpleOrConstraints(query.getConstraint());
        if (m.containsKey(Constants.JCR_TITLE)) {
            tmdbResult = queryTMDB(API_SEARCH_MOVIE, "query", m.get(Constants.JCR_TITLE).getString()).getJSONArray(RESULTS);
            if (tmdbResult != null) {
                processResultsArray(results, tmdbResult);
            }
        } else {
            getMostPopularMovies(query, results);
        }
    }

    private void getMostPopularMovies(ExternalQuery query, List<String> results) throws RepositoryException, JSONException {
        JSONArray tmdbResult;
        long pageNumber = query.getOffset() / 20;
        if (pageNumber < 100) {
            //Return up to the first 2000 most popular movies
            JSONObject discoverMovies = queryTMDB(API_DISCOVER_MOVIE, "sort_by", "popularity.desc", "page", String.valueOf(pageNumber + 1));
            if (discoverMovies.has("total_pages") && discoverMovies.has(RESULTS)) {
                int totalPages = discoverMovies.getInt("total_pages");
                tmdbResult = discoverMovies.getJSONArray(RESULTS);
                if (tmdbResult != null) {
                    processResultsArray(results, tmdbResult);
                }
                for (long i = pageNumber + 2; i <= totalPages; i++) {
                    processResultsArray(results,
                            queryTMDB(API_DISCOVER_MOVIE, "sort_by", "popularity.desc", "page", String.valueOf(i)).getJSONArray(RESULTS));
                    if (results.size() >= query.getLimit()) {
                        break;
                    }
                }
            }
            logger.info("Found {} results from TMDB", results.size());
        }
    }

    private void processResultsArray(List<String> results, JSONArray tmdbResult) throws JSONException {
        for (int i = 0; i < tmdbResult.length(); i++) {
            JSONObject jsonObject = tmdbResult.getJSONObject(i);
            final String path = getPathForMovie(jsonObject);
            if (path != null) {
                results.add(path);
                cache.put(new Element(INDEXEDFULLMOVIECACHEKEYPREFIX + jsonObject.getString("id"), "indexed"));
            }
        }
    }
}
