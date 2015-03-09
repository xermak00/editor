/*
 * Copyright (C) 2010 Jan Pokorsky
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package cz.incad.kramerius.editor.server;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

import cz.incad.kramerius.FedoraAccess;
import cz.incad.kramerius.KrameriusModels;
import cz.incad.kramerius.editor.server.utils.ApiUtilsHelp;
import cz.incad.kramerius.editor.share.GWTKrameriusObject.Kind;
import cz.incad.kramerius.editor.share.rpc.GetKrameriusObjectQuery;
import cz.incad.kramerius.editor.share.rpc.GetKrameriusObjectResult;
import cz.incad.kramerius.editor.share.rpc.GetKrameriusObjectResult.Descriptor;
import cz.incad.kramerius.relation.Relation;
import cz.incad.kramerius.relation.RelationModel;
import cz.incad.kramerius.relation.RelationService;
import cz.incad.kramerius.utils.StringUtils;
import cz.incad.kramerius.utils.conf.KConfiguration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.LocaleUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.customware.gwt.dispatch.server.ActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.ActionException;
import net.customware.gwt.dispatch.shared.DispatchException;

/**
 *
 * @author Jan Pokorsky
 */
public final class GetKrameriusObjectQueryHandler implements ActionHandler<GetKrameriusObjectQuery, GetKrameriusObjectResult> {

    private RelationService relationsDAO;
    private FedoraAccess fedoraAccess;
    private RemoteServices remotes;
    
    private Provider<HttpServletRequest> requestProvider;
    
    @Inject
    public GetKrameriusObjectQueryHandler(
            RelationService dao,
            RemoteServices remotes,
            @Named("rawFedoraAccess") FedoraAccess fedoraAccess, Provider<HttpServletRequest> reqProvider) {

        this.relationsDAO = dao;
        this.fedoraAccess = fedoraAccess;
        this.remotes = remotes;
        this.requestProvider = reqProvider;
    }

    @Override
    public Class<GetKrameriusObjectQuery> getActionType() {
        return GetKrameriusObjectQuery.class;
    }

    @Override
    public GetKrameriusObjectResult execute(GetKrameriusObjectQuery action, ExecutionContext context) throws DispatchException {
        String pidTxt = action.getPID();
        pidTxt = EditorServerUtils.validatePID(pidTxt);
        RelationModel fetchedRelations;
        try {
            fetchedRelations = fetchRelations(pidTxt);
        } catch (IOException ex) {
            // XXX implement better error handling to notify client what is wrong
            Logger.getLogger(GetKrameriusObjectQueryHandler.class.getName()).log(Level.SEVERE, null, ex);
            throw new ActionException(ex);
        }
        
        String loc = action.getLocale();
        Map<String, Map<String,String>> fetchedProperties = fetchTitles(pidTxt, fetchedRelations, loc);
        GetKrameriusObjectResult result = buildResult(pidTxt, fetchedRelations, fetchedProperties);
        return result;
    }

    @Override
    public void rollback(GetKrameriusObjectQuery action, GetKrameriusObjectResult result, ExecutionContext context) throws DispatchException {
        throw new UnsupportedOperationException("Not supported.");
    }

    private GetKrameriusObjectResult buildResult(
            String pid, RelationModel rels, Map<String, Map<String,String>> properties) {

        Descriptor objDescriptor = new Descriptor(pid, properties.get(pid));
        Kind objKind = EditorServerUtils.resolveKind(rels.getKind());
        RelationResult relResult = buildRelationsResult(rels, properties);

        GetKrameriusObjectResult result = new GetKrameriusObjectResult(
                objDescriptor, objKind, relResult.relKinds, relResult.relDescriptors);
        return result;
    }

    private RelationResult buildRelationsResult(RelationModel rels, Map<String, Map<String,String>> properties) {
        Kind[] relKinds = new Kind[rels.getRelationKinds().size()];
        Descriptor[][] relDescriptors = new Descriptor[relKinds.length][];
        int kindIdx = 0;
        for (KrameriusModels relationKind : rels.getRelationKinds()) {
            List<Relation> kindRelations = rels.getRelations(relationKind);
            Descriptor[] kindRelationDesriptors = new Descriptor[kindRelations.size()];
            relDescriptors[kindIdx] = kindRelationDesriptors;
            relKinds[kindIdx] = EditorServerUtils.resolveKind(relationKind);
            ++kindIdx;

            int relIdx = 0;
            for (Relation relation : kindRelations) {
                String pid = relation.getPID();
                Map<String, String> relProps = properties.get(pid);
                kindRelationDesriptors[relIdx++] = new Descriptor(
                        relation.getPID(), relProps);
            }
        }
//        result.setRelations(relKinds, relDescriptors);

        return new RelationResult(relKinds, relDescriptors);
    }

    /** helper class to pass complex result */
    private static final class RelationResult {
        Kind[] relKinds;
        Descriptor[][] relDescriptors;

        public RelationResult(Kind[] relKinds, Descriptor[][] relDescriptors) {
            this.relKinds = relKinds;
            this.relDescriptors = relDescriptors;
        }
    }

    private RelationModel fetchRelations(String pid) throws IOException {
        RelationModel rels = relationsDAO.load(pid);
        return rels;
    }

    private Map<String, Map<String,String>> fetchTitles(String pid, RelationModel rels, String locale) throws ActionException {
        try {
            Map<String, Map<String, String>> result = new HashMap<String, Map<String,String>>();

            
            Locale loc = LocaleUtils.toLocale(locale);
            if (loc == null) {
                loc = Locale.getDefault();
            }

            JSONObject jsonObj = ApiUtilsHelp.item(pid);
            String constructedTitle = ApiUtilsHelp.constructTitle(jsonObj, loc);

            Map<String, JSONObject> objects = new HashMap<String, JSONObject>();
            JSONArray jsonArr = ApiUtilsHelp.children(pid);
            for (int i = 0, ll = jsonArr.length(); i < ll; i++) {
                JSONObject chJSON = (JSONObject) jsonArr.get(i);
                objects.put(chJSON.getString("pid"), chJSON);
            }

            result.put(pid, propertiesJSONObject(jsonObj, constructedTitle));
            for (KrameriusModels relationKind : rels.getRelationKinds()) {
                for (Relation relation : rels.getRelations(relationKind)) {
                    JSONObject jsonObject = objects.get(relation.getPID());
                    if (jsonObject != null) {
                        result.put(relation.getPID(), propertiesJSONObject(jsonObject, constructedTitle));
                    }
                }
            }
            return result;
        } catch (JSONException e) {
            throw new ActionException(e);
        }
    }

    
    
    
    public static Map<String, String> propertiesJSONObject(JSONObject jsonObj, String constructedTitle) throws JSONException {
        Map<String, String> map = new HashMap<String, String>();
        map.put("title", jsonObj.getString("title"));
        map.put("pid", jsonObj.getString("pid"));
        map.put("policy", jsonObj.getString("policy"));
        map.put("rootTitle", jsonObj.getString("root_title"));
        map.put("constructedTitle", constructedTitle);
        
        String model = jsonObj.getString("model");
        map.put("model", model);
        if (jsonObj.has("details")) {
            JSONObject detailsJSON = jsonObj.getJSONObject("details");
            PropertiesFromModels detail = PropertiesFromModels.findDetail(model);
            if (detail != null) {
                detail.details(map, detailsJSON);
            }
        }

        if (jsonObj.has("context")) {
            JSONArray jsonArray = jsonObj.getJSONArray("context");
            if (jsonArray.length() > 0) {
                StringBuilder builder = new StringBuilder();
                JSONArray selArr = jsonArray.getJSONArray(0);
                for (int i = 0,ll=selArr.length(); i < ll; i++) {
                    if (i > 0) builder.append(",");
                    JSONObject value = selArr.getJSONObject(i);
                    builder.append(value.getString("pid"));
                }
                map.put("context", builder.toString());
            }
        }
        return map;
    }



    private String title(String pid) throws JSONException {
        String url = KConfiguration.getInstance().getConfiguration().getString("api.point")+"/item/"+pid;
        Logger.getLogger(GetKrameriusObjectQueryHandler.class.getName()).log(Level.INFO, "URL + "+url);
        JSONObject jsonObject = ApiUtilsHelp.getJSONasObject(url);
        String rtitle = jsonObject.getString("root_title");
        String obj = jsonObject.getString("title");
        String title = obj;
        if (StringUtils.isAnyString(rtitle)) {
            title = rtitle+" " +obj;
        }
        Logger.getLogger(GetKrameriusObjectQueryHandler.class.getName()).log(Level.INFO, "Title "+title);
        return title;
    }
    

    private String fetchDCName(String pid) throws ActionException {
        try {
            return remotes.fetchDCName(pid);
        } catch (IOException ex) {
            Logger.getLogger(GetKrameriusObjectQueryHandler.class.getName()).log(Level.SEVERE, "pid: " + pid, ex);
            throw new ActionException("The server is out of order.");
        }
    }
    
    private static enum PropertiesFromModels {

        page {
            @Override
            public void details(Map<String, String> map, JSONObject jsonObject) throws JSONException {
                List<String> alist = new ArrayList<String>();
                String title = map.get("title");
                if (jsonObject.has("pagenumber")) {
                    String jsonObjTitle = jsonObject.getString("pagenumber");
                    if (jsonObjTitle != null) {
                        jsonObjTitle = jsonObjTitle.trim();
                        if ((title != null) && (!jsonObjTitle.equals(title))) {
                            alist.add(jsonObjTitle);
                        }
                    }
                }

                if (jsonObject.has("type")) {
                    String type = jsonObject.getString("type");
                    type = type.trim();
                    alist.add(type);
                }
                
                if (!alist.isEmpty()) {
                    map.put("details", alist.toString());
                }
            }
        },

        periodicalitem {

            @Override
            public void details(Map<String, String> map, JSONObject jsonObject) throws JSONException {

                List<String> alist = new ArrayList<String>();

                if (jsonObject.has("issueNumber")){
                    String issueNumber = jsonObject.getString("issueNumber");
                    if (issueNumber != null) {
                        issueNumber = issueNumber.trim();
                        if (!issueNumber.equals("")) {
                            alist.add(issueNumber);
                            //map.put("issueNumber", issueNumber);
                        }
                    }
                }

                if (jsonObject.has("date")){
                    String date = jsonObject.getString("date");
                    if (date != null) {
                        date = date.trim();
                        if (!date.equals("")) {
                            alist.add(date);
                            //map.put("date", date);
                        }
                    }
                }

                if (jsonObject.has("partNumber")){
                    String partNumber = jsonObject.getString("partNumber");
                    if (partNumber != null) {
                        partNumber = partNumber.trim();
                        if (!partNumber.equals("")) {
                            alist.add(partNumber);
                            //map.put("partNumber", partNumber);
                        }
                    }
                }

                if (!alist.isEmpty()) {
                    map.put("details", alist.toString());
                }

            }

        },
        
        periodicalvolume {
            @Override
            public void details(Map<String, String> map, JSONObject jsonObject) throws JSONException {
                if (jsonObject.has("year")){
                    String year = jsonObject.getString("year");
                    if (year != null) {
                        year = year.trim();
                        if (!year.equals("")) {
                            map.put("details", Arrays.asList(year).toString());
                        }
                    }
                }
            }

            
        };

        public abstract void details(Map<String, String> map, JSONObject jsonObject) throws JSONException;

        
        public static PropertiesFromModels findDetail(String model) {
            PropertiesFromModels[] values = PropertiesFromModels.values();
            for (PropertiesFromModels detail : values) {
                if (detail.name().equals(model)) {
                    return detail;
                }
            }
            return null;
        }
    }


}