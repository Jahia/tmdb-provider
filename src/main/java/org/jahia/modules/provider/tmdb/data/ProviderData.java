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

import org.jahia.modules.external.ExternalData;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Jerome Blanchard
 */
public class ProviderData implements Serializable {

    private String id;
    private String type;
    private String name;
    private Map<String,String[]> properties;
    private Map<String, Map<String,String[]>> i18nProperties;

    public ProviderData() {
        properties = new HashMap<>();
        i18nProperties = new HashMap<>();
    }

    public ProviderData(String id, String type, String name, Map<String, String[]> properties) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.properties = properties;
        this.i18nProperties = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public ProviderData withId(String id) {
        this.id = id;
        return this;
    }

    public String getType() {
        return type;
    }

    public ProviderData withType(String type) {
        this.type = type;
        return this;
    }

    public String getName() {
        return name;
    }

    public ProviderData withName(String name) {
        this.name = name;
        return this;
    }

    public Map<String, String[]> getProperties() {
        return properties;
    }

    public ProviderData withProperties(Map<String, String[]> properties) {
        this.properties = properties;
        return this;
    }

    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    public String[] getProperty(String key) {
        return properties.get(key);
    }

    public ProviderData withProperty(String key, String[] value) {
        this.properties.put(key, value);
        return this;
    }

    public Map<String, String[]> getProperties(String language) {
        if (!i18nProperties.containsKey(language)) {
            i18nProperties.put(language, new HashMap<>());
        }
        return i18nProperties.get(language);
    }

    public ProviderData withProperties(String language, Map<String, String[]> properties) {
        i18nProperties.put(language, properties);
        return this;
    }

    public boolean hasProperty(String language, String key) {
        return i18nProperties.containsKey(language) && i18nProperties.get(language).containsKey(key);
    }

    public String[] getProperty(String language, String key) {
        if (i18nProperties.containsKey(language)) {
            return i18nProperties.get(language).get(key);
        }
        return null;
    }

    public ProviderData withProperty(String language, String key, String[] value) {
        if (!i18nProperties.containsKey(language)) {
            i18nProperties.put(language, new HashMap<>());
        }
        this.i18nProperties.get(language).put(key, value);
        return this;
    }

    public boolean hasLanguage(String language) {
        return i18nProperties.containsKey(language);
    }

    public Map<String, Map<String, String[]>> getI18nProperties() {
        return i18nProperties;
    }

    public ExternalData toExternalData(String path) {
        return new ExternalData(id, path, type, properties);
    }

    public ExternalData toExternalData(String path, Set<String> lazyProps, Set<String> lazyI18nProps) {
        ExternalData data = new ExternalData(id, path, type, properties);
        data.setLazyProperties(new HashSet<>(lazyProps));
        data.setLazyI18nProperties(Map.of("en", new HashSet<>(lazyI18nProps), "fr", new HashSet<>(lazyI18nProps)));
        return data;
    }
}
