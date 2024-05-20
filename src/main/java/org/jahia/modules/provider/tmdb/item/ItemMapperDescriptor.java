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
package org.jahia.modules.provider.tmdb.item;

import org.jahia.modules.provider.tmdb.helper.Naming;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public enum ItemMapperDescriptor {

    ERROR("error", "^/error$", "", "^error$", "", false,
            ErrorItemMapper.class),
    ROOT("root", "^/?$", "root", "^root$", Naming.NodeType.CONTENT_FOLDER,false,
            RootItemMapper.class),
    MOVIES("movies", "^/movies$", "movies", "^movies$", Naming.NodeType.CONTENT_FOLDER,false,
            MoviesItemMapper.class),
    PERSONS("persons", "^/persons$", "persons",  "^persons$", Naming.NodeType.CONTENT_FOLDER,false,
            PersonsItemMapper.class),
    PERSON("person", "^/persons/\\d+$", "person-",  "^person-\\d+$",  Naming.NodeType.MOVIE_PERSON,false,
            PersonItemMapper.class),
    MOVIE_YEAR("year", "^/movies/\\d{4}$", "",  "^myear-\\d{4}$", Naming.NodeType.CONTENT_FOLDER,false,
            MovieYearItemMapper.class),
    MOVIE_MONTH("month", "^/movies/\\d{4}/\\d{4}-\\d{2}$", "", "^mmonth-\\d{4}-\\d{2}$", Naming.NodeType.CONTENT_FOLDER,false,
            MovieMonthItemMapper.class),
    MOVIE_ID("movie", "^/movies/\\d{4}/\\d{4}-\\d{2}/\\d+$", "mid-", "^mid-\\d+$",  Naming.NodeType.MOVIE, true,
            MovieItemMapper.class),
    MOVIE_CREDITS("credits", "^/movies/\\d{4}/\\d{4}-\\d{2}/\\d+/(cast_|crew_)\\d+$", "mcredits-", "^mcredits-\\d+-\\d+$",
            List.of(Naming.NodeType.CREW, Naming.NodeType.CAST), false, MovieCreditsItemMapper.class),
    MOVIE_REF("lists", "^/lists/[a-z0-9]+/\\d+$", "mref-", "^mref-[a-z0-9]+\\d+$",  Naming.NodeType.CONTENT_REFERENCE, false,
            MovieReferenceItemMapper.class),
    ;

    private final String pathLabel;
    private final Pattern pathPattern;
    private final String idPrefix;
    private final Pattern idPattern;
    private final List<String> types;
    private final boolean lazyProps;
    private final Class<? extends ItemMapper> mapperClass;

    ItemMapperDescriptor(String pathLabel, String pathPattern, String idPrefix, String idPattern,
            String types, boolean lazyProps, Class<? extends ItemMapper> mapperClass) {
        this(pathLabel, pathPattern, idPrefix, idPattern, Arrays.asList(types.split(",")), lazyProps, mapperClass);
    }

    ItemMapperDescriptor(String pathLabel, String pathPattern, String idPrefix, String idPattern,
            List<String> types, boolean lazyProps, Class<? extends ItemMapper> mapperClass) {
        this.pathLabel = pathLabel;
        this.pathPattern = Pattern.compile(pathPattern);
        this.idPrefix = idPrefix;
        this.idPattern = Pattern.compile(idPattern);
        this.mapperClass = mapperClass;
        this.types = types;
        this.lazyProps = lazyProps;
    }

    public String getPathLabel() {
        return pathLabel;
    }

    public Pattern getPathPattern() {
        return pathPattern;
    }

    public String getIdPrefix() {
        return idPrefix;
    }

    public Pattern getIdPattern() {
        return idPattern;
    }

    public List<String> getMappedTypes() {
        return types;
    }

    public boolean containsMappedType(String type) {
        return types.contains(type);
    }

    public boolean supportLazyProps() {
        return lazyProps;
    }

    public Class<? extends ItemMapper> getMapperClass() {
        return mapperClass;
    }

    @Override public String toString() {
        return getPathLabel();
    }

    public static Optional<ItemMapperDescriptor> findByPath(String path) {
        return Arrays.stream(ItemMapperDescriptor.values()).filter(n -> n.getPathPattern() != null && n.getPathPattern().matcher(path).matches()).findFirst();
    }

    public static Optional<ItemMapperDescriptor> findByPathForProps(String path) {
        return Arrays.stream(ItemMapperDescriptor.values()).filter(n -> n.supportLazyProps() && n.getPathPattern() != null && n.getPathPattern().matcher(path).matches()).findFirst();
    }

    public static Optional<ItemMapperDescriptor> findById(String id) {
        return Arrays.stream(ItemMapperDescriptor.values()).filter(n -> n.getIdPattern() != null && n.getIdPattern().matcher(id).matches()).findFirst();
    }

    public static List<ItemMapperDescriptor> findByType(String type) {
        return Arrays.stream(ItemMapperDescriptor.values()).filter(n -> n.containsMappedType(type)).collect(Collectors.toList());
    }
}
