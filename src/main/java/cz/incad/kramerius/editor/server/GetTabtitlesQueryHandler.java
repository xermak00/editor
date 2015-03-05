package cz.incad.kramerius.editor.server;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.customware.gwt.dispatch.server.ActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import cz.incad.kramerius.FedoraAccess;
import cz.incad.kramerius.editor.server.utils.ApiUtilsHelp;
import cz.incad.kramerius.editor.share.rpc.GetTabstitleQuery;
import cz.incad.kramerius.editor.share.rpc.GetTabstitleResult;
import cz.incad.kramerius.relation.RelationService;
import cz.incad.kramerius.utils.StringUtils;

public class GetTabtitlesQueryHandler  implements ActionHandler<GetTabstitleQuery, GetTabstitleResult> {

    public static final Logger LOGGER = Logger.getLogger(GetTabtitlesQueryHandler.class.getName());
    
    private RelationService relationsDAO;
    private FedoraAccess fedoraAccess;
    private RemoteServices remotes;

    @Inject
    public GetTabtitlesQueryHandler(
            RelationService dao,
            RemoteServices remotes,
            @Named("rawFedoraAccess") FedoraAccess fedoraAccess) {

        this.relationsDAO = dao;
        this.fedoraAccess = fedoraAccess;
        this.remotes = remotes;
    }

    @Override
    public GetTabstitleResult execute(GetTabstitleQuery query, ExecutionContext ctx) throws DispatchException {
        String pid = query.getPID();
        String title = ApiUtilsHelp.constructTitle(pid);
        GetTabstitleResult res = new GetTabstitleResult(title);
        return res;
    }


    private JSONArray select(JSONArray ctxArray) throws JSONException {
        if (ctxArray.length() > 0) {
            return ctxArray.getJSONArray(0);
        } else return null;
    }

    @Override
    public Class<GetTabstitleQuery> getActionType() {
        return GetTabstitleQuery.class;
    }

    @Override
    public void rollback(GetTabstitleQuery query, GetTabstitleResult result, ExecutionContext ctx) throws DispatchException {
        // read only
    }

}
