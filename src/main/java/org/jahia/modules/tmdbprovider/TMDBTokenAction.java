package org.jahia.modules.tmdbprovider;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

public class TMDBTokenAction extends Action {

    private TMDBDataSource provider;

    public void setDatasource(TMDBDataSource provider) {
        this.provider = provider;
    }

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource, JCRSessionWrapper session, Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {
        String token = provider.createToken();
        JSONObject r = new JSONObject();
        r.put("token",token);
        r.put("url","http://www.themoviedb.org/authenticate/"+token);
        return new ActionResult(200, null, r);

    }
}
