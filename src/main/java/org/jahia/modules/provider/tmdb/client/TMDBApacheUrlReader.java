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
package org.jahia.modules.provider.tmdb.client;

import info.movito.themoviedbapi.model.core.responses.TmdbResponseException;
import info.movito.themoviedbapi.tools.RequestType;
import info.movito.themoviedbapi.tools.TmdbUrlReader;
import org.apache.http.HttpHeaders;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URL;

/**
 * Apache Http Client for TMDB API to avoid using okhttp.
 *
 * @author Jerome Blanchard
 */
public class TMDBApacheUrlReader implements TmdbUrlReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(TMDBApacheUrlReader.class);
    public static final int SOCKET_TIMEOUT = 60000;
    public static final int CONNECT_TIMEOUT = 15000;
    public static final int MAX_CONNECTIONS = 10;
    public static final int DEFAULT_MAX_PER_ROUTE = 2;


    private HttpClient httpClient;
    private String apikey;

    public TMDBApacheUrlReader(String apikey) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(SOCKET_TIMEOUT)
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setConnectionRequestTimeout(CONNECT_TIMEOUT).build();

        SSLContext sslcontext = SSLContexts.createSystemDefault();

        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", new SSLConnectionSocketFactory(sslcontext))
                .build();

        PoolingHttpClientConnectionManager httpConnectionManager = new PoolingHttpClientConnectionManager(registry);
        httpConnectionManager.setMaxTotal(MAX_CONNECTIONS);
        httpConnectionManager.setDefaultMaxPerRoute(DEFAULT_MAX_PER_ROUTE);

        httpClient = HttpClients.custom().setConnectionManager(httpConnectionManager).setDefaultRequestConfig(requestConfig)
                .disableCookieManagement().build();
        this.apikey = apikey;
    }

    @Override
    public String readUrl(URL url, String jsonBody, RequestType requestType) throws TmdbResponseException {
        LOGGER.debug(String.format("TMDB API: making request, of type: %s, to: %s", requestType.toString(), url.toString()));

        HttpRequestBase request;
        switch (requestType) {
            case GET :
                request = new HttpGet(url.toExternalForm());
                break;
            case POST :
                StringEntity entity = new StringEntity(jsonBody, ContentType.APPLICATION_JSON);
                request = new HttpPost(url.toExternalForm());
                request.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
                ((HttpPost)request).setEntity(entity);
                break;
            case DELETE :
                request = new HttpDelete(url.toExternalForm());
                break;
            default:
                throw new RuntimeException("Invalid request type: " + requestType);
        }
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apikey);
        request.addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());

        long l = System.currentTimeMillis();
        try (CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(request)) {
            LOGGER.debug("Request {} executed in {} ms", url, (System.currentTimeMillis() - l));
            return EntityUtils.toString(response.getEntity());
        } catch (IOException exception) {
            throw new TmdbResponseException(exception);
        }
    }

}
