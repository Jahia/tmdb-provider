# tmdb-provider

## Prerequisites
* Jahia 8.2.0.0 or superior
* bootstrap 2 modules (bootstrap and bootstrap-component)
* the prepackaged site Digitall must be deployed

## Setup
To setup the tmdb-provider you must:
1. request an API key from TMDB
2. deploy the module tmdb-provider
3. go in the Jahia Tools OSGi configuration tool and configure the TMDB provider by using the API key 
5. deploy the module tmdb-provider
6. add the component requestToken in one your page, then click on the link to login/create an account and click on `I agree`
7. Now you can for example add a content reference and look for a node in the source, the source is under /digitall/contents/tmdb/...
