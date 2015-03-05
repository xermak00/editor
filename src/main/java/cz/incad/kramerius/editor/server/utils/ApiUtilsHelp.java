package cz.incad.kramerius.editor.server.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.json.JSONConfiguration;

import cz.incad.kramerius.editor.server.GetKrameriusObjectQueryHandler;
import cz.incad.kramerius.utils.StringUtils;
import cz.incad.kramerius.utils.conf.KConfiguration;

public class ApiUtilsHelp {

    public static Logger LOGGER = Logger.getLogger(ApiUtilsHelp.class.getName());
    

    private static JSONArray select(JSONArray ctxArray) throws JSONException {
        if (ctxArray.length() > 0) {
            return ctxArray.getJSONArray(0);
        } else return null;
    }

    public static String constructTitle(String pid) {
        try {
            JSONObject jsonObject = item(pid);
            return constructTitle(jsonObject);
        } catch (JSONException e) {
            LOGGER.log(Level.SEVERE,e.getMessage(),e);
            return "";
        }
        
    }
    public static String constructTitle(JSONObject jsonObject) {
        try {
            ResourceBundle tabs = ResourceBundle.getBundle("cz/incad/kramerius/editor/server/tabs");
            Map<String, JSONObject> map = new HashMap<String, JSONObject>();

            map.put(jsonObject.getString("pid"), jsonObject);
            JSONArray ctxArray = jsonObject.getJSONArray("context");
            for (int i = 0,ll=ctxArray.length(); i < ll; i++) {
                JSONArray subArr = ctxArray.getJSONArray(i);
                for (int j = 0,lj=subArr.length(); j < lj; j++) {
                    JSONObject subJSON = subArr.getJSONObject(j);
                    String subPid = subJSON.getString("pid");
                    if (!map.containsKey(subPid)) {
                        map.put(subPid, ApiUtilsHelp.item(subPid));
                    }
                }
            }

            StringBuilder builder = new StringBuilder();
            
            JSONArray selected = select(ctxArray);
            if (selected != null) {
                for (int i = selected.length()-1;i>=0; i--) {
                    String chPid = selected.getJSONObject(i).getString("pid");
                    String chModel = selected.getJSONObject(i).getString("model");
                    if (tabs.containsKey(chModel)) {
                        chModel = tabs.getString(chModel);
                    }
                    
                    String chTitle = map.get(chPid).getString("title");
                    if (!StringUtils.isAnyString(chTitle)) {
                        Titles detail = Titles.findDetail(chModel);
                        if (detail != null) {
                            JSONObject chJSON = map.get(chPid);
                            chModel = detail.details(chTitle, chJSON);
                        }
                    }
                    if (i < selected.length()-1) builder.append(" ‣ ");
                    builder.append(chModel).append(" (").append(chTitle+")");
                }
            }
            return builder.toString();
        } catch (JSONException e) {
            LOGGER.log(Level.SEVERE,e.getMessage(),e);
            return "";
        }

    }
    
    public static JSONObject getJSONasObject(String url) throws JSONException {
        JSONObject obj = new JSONObject(getJSON(url));
        return obj;
    }

    public static JSONArray getJSONasArray(String url) throws JSONException {
        JSONArray obj = new JSONArray(getJSON(url));
        return obj;
    }
    
    public static String getJSON(String url)
            throws JSONException {
        Client c = Client.create();
        WebResource r = c.resource(url);
        Builder builder = r.accept(MediaType.APPLICATION_JSON);
        return builder.get(String.class);
    }

    public static  JSONObject item(String pid) throws JSONException {
        String url = KConfiguration.getInstance().getConfiguration().getString("api.point")+"/item/"+pid;
        Logger.getLogger(GetKrameriusObjectQueryHandler.class.getName()).log(Level.INFO, "URL + "+url);
        JSONObject jsonObject = getJSONasObject(url);
        return jsonObject;
    }

    public static JSONArray children(String pid) throws JSONException {
        String url = KConfiguration.getInstance().getConfiguration().getString("api.point")+"/item/"+pid+"/children";
        Logger.getLogger(GetKrameriusObjectQueryHandler.class.getName()).log(Level.INFO, "URL + "+url);
        JSONArray jsonArr = getJSONasArray(url);
        return jsonArr;
    }
    
    
    private static enum Titles {

        page {
            @Override
            public String details(String title, JSONObject jsonObject) throws JSONException {
                List<String> alist = new ArrayList<String>();
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
                    return concat(alist);
                } else {
                    return "";
                }
            }
        },

        periodicalitem {

            @Override
            public String details(String titlte, JSONObject jsonObject) throws JSONException {
                List<String> alist = new ArrayList<String>();
                if (jsonObject.has("issueNumber")){
                    String issueNumber = jsonObject.getString("issueNumber");
                    if (StringUtils.isAnyString(issueNumber)) {
                        alist.add(issueNumber);
                    }
                }

                if (jsonObject.has("date")){
                    String date = jsonObject.getString("date");
                    if (StringUtils.isAnyString(date)) {
                        alist.add(date);
                    }
                }

                if (jsonObject.has("partNumber")){
                    String partNumber = jsonObject.getString("partNumber");
                    if (StringUtils.isAnyString(partNumber)) {
                        alist.add(partNumber);
                    }
                 }
                
                return concat(alist);
            }

        },
        
        periodicalvolume {
            @Override
            public String details(String titlte, JSONObject jsonObject) throws JSONException {
                if (jsonObject.has("year")){
                    String year = jsonObject.getString("year");
                    if (StringUtils.isAnyString(year)) {
                        return year;
                    } else return "";
                } else return "";
            }
        };

        public abstract String details(String titlte, JSONObject jsonObject) throws JSONException;

        
        public static Titles findDetail(String model) {
            Titles[] values = Titles.values();
            for (Titles detail : values) {
                if (detail.name().equals(model)) {
                    return detail;
                }
            }
            return null;
        }
        
        public static String concat(List<String> alist) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0,ll=alist.size(); i < ll; i++) {
                if (i > 0) builder.append(",");
                builder.append(alist.get(i));
            }
            return builder.toString();
        }
    }



}
