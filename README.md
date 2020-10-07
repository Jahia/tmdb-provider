# tmdb-provider

## Prerequisites
* Jahia 7.1.0.0 or superior
* bootstrap 2 modules (bootstrap and bootstrap-component)
* the prepackaged site Digitall must be deployed

## Setup
To setup the tmdb-provider you must:
1. request an API key(API Read Access Token (v4 auth) not the API Key (v3 auth)) from TMDB and add the value in jahia.properties file, like this `com.jahia.tmdb.apiKeyValue=YOUR_API_KEY`
2. start or restart your jahia
3. deploy the module tmdb-provider
5. Now you can for example add a content reference and look for a node in the source, the source is under /digitall/contents/tmdb/...
