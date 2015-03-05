package cz.incad.kramerius.editor.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;

import junit.framework.TestCase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;

import cz.incad.kramerius.utils.IOUtils;

public class GetKrameriusObjectQueryHandlerTest2 extends TestCase {
    
    private String readString(String param) throws IOException {
        InputStream is = PropertiesFromModelsTest.class.getResourceAsStream(param);
        String str = IOUtils.readAsString(is, Charset.forName("UTF-8"),true);
        return str;
    }
    
    
    
    public void testPropertiesToJSON() throws JSONException, IOException {
        String periodical = readString("periodical.json");
        String periodical_children = readString("periodical_children.json");
        
        JSONObject jsonObject = new JSONObject(periodical);
        Map<String, String> map = GetKrameriusObjectQueryHandler.propertiesJSONObject(jsonObject,"nevim");
        System.out.println(map);
        Assert.assertTrue(map.get("pid").equals("uuid:4eac74b0-e92c-11dc-9fa1-000d606f5dc6"));
        Assert.assertTrue(map.get("model").equals("periodical"));
        Assert.assertTrue(map.get("title").equals("Rudé právo"));
        Assert.assertTrue(map.get("rootTitle").equals("Rudé právo"));
        
        JSONArray jsonArr = new JSONArray(periodical_children);
        for (int i = 0,ll=jsonArr.length(); i < ll; i++) {
            JSONObject chJSON = jsonArr.getJSONObject(i);
            Map<String, String> chMap = GetKrameriusObjectQueryHandler.propertiesJSONObject(chJSON, "nevim");
            
            Assert.assertTrue(chMap.containsKey("pid"));
            Assert.assertTrue(chMap.containsKey("model"));
            
            Assert.assertTrue(chMap.get("model").equals("periodicalvolume"));
            Assert.assertTrue(chMap.containsKey("title"));
            Assert.assertTrue(chMap.containsKey("rootTitle"));
            Assert.assertTrue(chMap.containsKey("year"));
            System.out.println(chMap);
        }
        
        
        String periodicalvolume = readString("periodicalvolume.json");
        String periodicalvolume_children = readString("periodicalvolume_children.json");
        
        jsonObject = new JSONObject(periodicalvolume);
        map = GetKrameriusObjectQueryHandler.propertiesJSONObject(jsonObject,"nevim");
        Assert.assertTrue(map.containsKey("pid"));
        Assert.assertTrue(map.get("model").equals("periodicalvolume"));
        Assert.assertTrue(map.containsKey("title"));
        Assert.assertTrue(map.containsKey("rootTitle"));
        Assert.assertTrue(map.containsKey("year"));

        System.out.println("map"+map);
        
        
        jsonArr = new JSONArray(periodicalvolume_children);
        for (int i = 0,ll=jsonArr.length(); i < ll; i++) {
            JSONObject chJSON = jsonArr.getJSONObject(i);
            Map<String, String> chMap = GetKrameriusObjectQueryHandler.propertiesJSONObject(chJSON, "nevim");

            Assert.assertTrue(chMap.containsKey("pid"));
            Assert.assertTrue(chMap.containsKey("model"));

            Assert.assertTrue(chMap.get("model").equals("periodicalitem"));
            Assert.assertTrue(chMap.containsKey("title"));
            Assert.assertTrue(chMap.containsKey("rootTitle"));
            //Assert.assertTrue(chMap.containsKey("issueNumber"));
            Assert.assertTrue(chMap.containsKey("date"));
            Assert.assertTrue(chMap.containsKey("partNumber"));
        }
        
    }
}
