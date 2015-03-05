package cz.incad.kramerius.editor.server;

import java.io.IOException;
import java.nio.charset.Charset;

import org.json.JSONException;
import org.json.JSONObject;

import cz.incad.kramerius.utils.IOUtils;

public class PropertiesFromModelsTest {
    
    
    public void testPropertiesFromModels() throws IOException, JSONException {
        String str = readString("periodical.json");
        JSONObject jsonObject = new JSONObject(str);
        
    }

    private String readString(String param) throws IOException {
        String str = IOUtils.readAsString(PropertiesFromModelsTest.class.getResourceAsStream(param), Charset.forName("UTF-8"),true);
        return str;
    }
}
