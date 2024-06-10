# tmdb-provider

## Prerequisites
* Jahia 8.2.0.0 or superior
* bootstrap 3 modules (bootstrap3 and bootstrap3-component)

## Setup
To setup the tmdb-provider you must:
1. request an API key(API Read Access Token (v4 auth) not the API Key (v3 auth)) from TMDB.
2. start or restart your jahia
3. deploy the module tmdb-provider
4. go in the Jahia Tools OSGi configuration tool and configure the TMDB provider by using the key and a mount point.
5. Now you can for example add a content reference and look for a node in the source, the source is under /yoursite/contents/tmdb/...
