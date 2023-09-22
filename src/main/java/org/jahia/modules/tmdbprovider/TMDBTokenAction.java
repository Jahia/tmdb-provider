package org.jahia.modules.tmdbprovider;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Component(service=Action.class, immediate = true)
public class TMDBTokenAction extends Action {

    private TMDBDataSource provider;

    @Reference
    public void setDatasource(TMDBDataSource provider) {
        this.provider = provider;
    }

    @Activate
    public void activate() {
        setName("tmdbtoken");
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
