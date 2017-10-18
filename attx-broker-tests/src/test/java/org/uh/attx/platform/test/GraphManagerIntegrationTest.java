package org.uh.attx.platform.test;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.apache.commons.io.FileUtils;
import org.uh.hulib.attx.wc.uv.common.RabbitMQClient;

import java.io.File;

import static org.junit.Assert.*;


@RunWith(JUnitPlatform.class)
public class GraphManagerIntegrationTest {

    public final String API_VERSION = "0.2";

    @Before
    public void checkHealth(){
        TestUtils.testGmHealth();
    }


    private String getQueryResult(HttpResponse<JsonNode> resp, int index, String key)  {
        JSONObject item = resp.getBody().getObject().getJSONObject("results").getJSONArray("bindings").getJSONObject(index);
        return item.getJSONObject(key).getString("value");
    }

//    @Test
    public void testReplaceDatasetData(){
        try {
            String reqStr = FileUtils.readFileToString(new File(getClass().getResource(
                    "/graphmanagerfixtures/graph_replace_data.json").toURI()), "UTF-8");
            RabbitMQClient c = new RabbitMQClient("localhost", "user", "password", "provenance.inbox");

            System.out.println(reqStr);
            String respStr = c.sendSyncServiceMessage(reqStr, "attx.graphManager.inbox", 10000);
            System.out.println("test: " + respStr + "*");
            assertNotEquals("", respStr);
            HttpResponse<JsonNode> resp = TestUtils.graphQueryResult("SELECT (count(?s) as ?count) \n" +
                    "FROM <http://work/dataset1>\n" +
                    "WHERE{      \n" +
                    "   ?s ?p ?o .\n" +
                    "}\n");
            assertEquals("1", getQueryResult(resp, 0, "count"));

        }catch (Exception e){
            fail(e.getMessage());
        }
    }

    @Test
    public void testReplaceDatasetURI(){
        File input = null;
        try {
            input = File.createTempFile("file", ".ttl");
            FileUtils.writeStringToFile(input, "<http://example.org/#spiderman> <http://www.perceive.net/schemas/relationship/enemyOf> <http://example" +
                    ".org/#red-goblin> .", "UTF-8");
            String reqStr = FileUtils.readFileToString(new File(getClass().getResource(
                    "/graphmanagerfixtures/graph_replace_uri.json").toURI()), "UTF-8");

//            FileUtils.copyFile(input, new File());
            reqStr = reqStr.replaceFirst("file", "file:///attx-sb-shared/triple.ttl");
            RabbitMQClient c = new RabbitMQClient("localhost", "user", "password", "provenance.inbox");

            System.out.println(reqStr);
            String respStr = c.sendSyncServiceMessage(reqStr, "attx.graphManager.inbox", 10000);
            System.out.println("test: " + respStr + "*");
            assertNotEquals("", respStr);
            HttpResponse<JsonNode> resp = TestUtils.graphQueryResult("SELECT (count(?s) as ?count) \n" +
                    "FROM <http://work/dataset1>\n" +
                    "WHERE{      \n" +
                    "   ?s ?p ?o .\n" +
                    "}\n");
            assertEquals("1", getQueryResult(resp, 0, "count"));

        }catch (Exception e){
            fail(e.getMessage());
        } finally {
//            if(input != null)
//                input.delete();
        }
    }

}
