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

import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.modules.provider.tmdb.helper.Naming;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Jerome Blanchard
 */
@Component(service = { MonthsCollection.class, ProviderDataCollection.class}, scope = ServiceScope.SINGLETON, immediate = true)
public class MonthsCollection implements ProviderDataCollection {

    public static final String ID_PREFIX = "month-";

    private static final Map<String, ProviderData> MONTHS = new HashMap<>();
    static {
        List<String> years =  IntStream.rangeClosed(Calendar.getInstance().get(Calendar.YEAR) - 5,
                        Calendar.getInstance().get(Calendar.YEAR)).mapToObj(Integer::toString).collect(Collectors.toList());
        Calendar calendar = Calendar.getInstance();
        years.stream().map(year -> IntStream.rangeClosed(1, (calendar.get(Calendar.YEAR) == Integer.parseInt(year) ? calendar.get(Calendar.MONTH) : 12))
                .mapToObj(i -> year.concat("-").concat(StringUtils.leftPad(Integer.toString(i), 2, "0")))
                .collect(Collectors.toList()))
                .flatMap(List::stream)
                .forEach(month -> MONTHS.put(ID_PREFIX + month, map(month)));
        MONTHS.put(ID_PREFIX + "0000-00", map("0000-00"));
    }

    @Override
    public ProviderData getData(String identifier) {
        return MONTHS.get(identifier);
    }

    public List<ProviderData> list(String year) {
        return MONTHS.entrySet().stream().filter(e -> e.getKey().startsWith(ID_PREFIX + year)).map(Map.Entry::getValue).collect(Collectors.toList());
    }

    protected static ProviderData map(String month) {
        Map<String, String[]> properties = new HashMap<>();
        properties.put(Constants.JCR_TITLE, new String[] { month });
        return new ProviderData(ID_PREFIX + month, Naming.NodeType.CONTENT_FOLDER, month, properties);
    }


}
