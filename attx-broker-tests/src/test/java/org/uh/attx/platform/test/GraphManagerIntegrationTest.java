package org.uh.attx.platform.test;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.apache.commons.io.FileUtils;
import org.uh.hulib.attx.wc.uv.common.RabbitMQClient;

import java.io.File;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;


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

    @Test
    public void messageTestReplaceDatasetData(){
        try {
            String reqStr = FileUtils.readFileToString(new File(getClass().getResource(
                    "/graphmanagerfixtures/graph_replace_data.json").toURI()), "UTF-8");
            RabbitMQClient client = new RabbitMQClient(TestUtils.getMessageBrokerHost(), "user", "password", "provenance.inbox");

            System.out.println(reqStr);
            String respStr = client.sendSyncServiceMessage(reqStr, "attx.graphManager.inbox", 10000);
            System.out.println("test: " + respStr + "*");
            JSONObject jsonObj = new JSONObject(respStr);
            assertNotEquals("", respStr);
            if(!jsonObj.has("status")){
                HttpResponse<JsonNode> resp = TestUtils.graphQueryResult("SELECT (count(?s) as ?count) \n" +
                        "FROM <http://work/dataset1>\n" +
                        "WHERE{      \n" +
                        "   ?s ?p ?o .\n" +
                        "}\n");
                assertEquals("1", getQueryResult(resp, 0, "count"));
            } else {
                throw new Exception("Something went wrong: " + jsonObj.getString("statusMessage"));
            }

        }catch (Exception e){
            fail(e.getMessage());
        } finally {
            TestUtils.dropGraph("http://work/dataset1");
        }
    }

    @Test
    public void messageTestReplaceDatasetBadContent(){
        try {
            String reqStr = new String("ceva");
            RabbitMQClient client = new RabbitMQClient(TestUtils.getMessageBrokerHost(), "user", "password", "provenance.inbox");

            System.out.println(reqStr);
            String respStr = client.sendSyncServiceMessage(reqStr, "attx.graphManager.inbox", 10000);
            System.out.println("test: " + respStr + "*");
            JSONObject jsonObj = new JSONObject(respStr);
            assertNotEquals("", respStr);
            if(jsonObj.has("status")){
               assertEquals("Error Type: ValueError, with message: No JSON object could be decoded", jsonObj.getString("statusMessage"));
            } else {
                throw new Exception("Failed with different message: " + jsonObj.getString("statusMessage"));
            }


        }catch (Exception e){
            fail(e.getMessage());
        }
    }

    @Test
    public void messageTestReplaceDatasetMissingFile(){
        try {
            String reqStr = FileUtils.readFileToString(new File(getClass().getResource(
                    "/graphmanagerfixtures/graph_replace_uri_missing.json").toURI()), "UTF-8");
            RabbitMQClient client = new RabbitMQClient(TestUtils.getMessageBrokerHost(), "user", "password", "provenance.inbox");

            System.out.println(reqStr);
            String respStr = client.sendSyncServiceMessage(reqStr, "attx.graphManager.inbox", 10000);
            System.out.println("test: " + respStr + "*");
            JSONObject jsonObj = new JSONObject(respStr);
            assertNotEquals("", respStr);
            if(jsonObj.has("status")){

                assertEquals("Error Type: IOError, with message: Something went wrong with retrieving the file: file:///attx-sb-shared/data/triple_missing.ttl. It does not exist!", jsonObj.getString("statusMessage"));
            } else {
                throw new Exception("Failed with different message: " + jsonObj.getString("statusMessage"));
            }

        }catch (Exception e){
            fail(e.getMessage());
        } finally {
            TestUtils.dropGraph("http://work/dataset1");
        }
    }

    @Test
    public void messageTestReplaceDatasetURI(){

        try {
            String reqStr = FileUtils.readFileToString(new File(getClass().getResource(
                        "/graphmanagerfixtures/graph_replace_uri.json").toURI()), "UTF-8");
            RabbitMQClient client = new RabbitMQClient(TestUtils.getMessageBrokerHost(), "user", "password", "provenance.inbox");

            System.out.println(reqStr);
            String respStr = client.sendSyncServiceMessage(reqStr, "attx.graphManager.inbox", 10000);
            JSONObject jsonObj = new JSONObject(respStr);
            System.out.println("test: " + respStr + "*");
            assertNotEquals("", respStr);
            if(!jsonObj.has("status")){
                HttpResponse<JsonNode> resp = TestUtils.graphQueryResult("SELECT (count(?s) as ?count) \n" +
                        "FROM <http://work/dataset1>\n" +
                        "WHERE{      \n" +
                        "   ?s ?p ?o .\n" +
                        "}\n");
                assertEquals("1", getQueryResult(resp, 0, "count"));
            } else {
                throw new Exception("Something went wrong: " + jsonObj.getString("statusMessage"));
            }
        }catch (Exception e){
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            TestUtils.dropGraph("http://work/dataset1");
        }
    }

    @Test
    public void messageTestReplaceDatasetArray(){

        try {
            String reqStr = FileUtils.readFileToString(new File(getClass().getResource(
                        "/graphmanagerfixtures/graph_replace_array.json").toURI()), "UTF-8");
            RabbitMQClient client = new RabbitMQClient(TestUtils.getMessageBrokerHost(), "user", "password", "provenance.inbox");

            System.out.println(reqStr);
            String respStr = client.sendSyncServiceMessage(reqStr, "attx.graphManager.inbox", 10000);
            JSONObject jsonObj = new JSONObject(respStr);
            System.out.println("test: " + respStr + "*");
            assertNotEquals("", respStr);
            if(!jsonObj.has("status")){
                HttpResponse<JsonNode> resp = TestUtils.graphQueryResult("SELECT (count(?s) as ?count) \n" +
                        "FROM <http://work/dataset1>\n" +
                        "WHERE{      \n" +
                        "   ?s ?p ?o .\n" +
                        "}\n");
                assertEquals("2", getQueryResult(resp, 0, "count"));
            } else {
                throw new Exception("Something went wrong: " + jsonObj.getString("statusMessage"));
            }
        }catch (Exception e){
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            TestUtils.dropGraph("http://work/dataset1");
        }
    }

    @Test
    public void messageTestAdd(){
        try {
            String reqStr = FileUtils.readFileToString(new File(getClass().getResource(
                    "/graphmanagerfixtures/graph_add_array.json").toURI()), "UTF-8");
            RabbitMQClient client = new RabbitMQClient(TestUtils.getMessageBrokerHost(), "user", "password", "provenance.inbox");

            System.out.println(reqStr);
            String respStr = client.sendSyncServiceMessage(reqStr, "attx.graphManager.inbox", 10000);
            System.out.println("test: " + respStr + "*");
            assertNotEquals("", respStr);
            HttpResponse<JsonNode> resp = TestUtils.graphQueryResult("SELECT (count(?s) as ?count) \n" +
                    "FROM <http://work/dataset1>\n" +
                    "WHERE{      \n" +
                    "   ?s ?p ?o .\n" +
                    "}\n");
            assertEquals("2", getQueryResult(resp, 0, "count"));

        }catch (Exception e){
            fail(e.getMessage());
        } finally {
            TestUtils.dropGraph("http://work/dataset1");
        }
    }

    @Test
    public void apiTestAddDatasetData(){
        try {
            String reqStr = FileUtils.readFileToString(new File(getClass().getResource(
                    "/graphmanagerfixtures/api_graph_add.json").toURI()), "UTF-8");
            HttpResponse<JsonNode> addRequest = Unirest.post(TestUtils.getGmService() + "/" + API_VERSION + "/graph/update")
                    .header("accept", "application/json")
                    .header("Content-Type", "application/json")
                    .body(reqStr)
                    .asJson();

            System.out.println(reqStr);
            assertEquals(200, addRequest.getStatus());
            HttpResponse<JsonNode> resp = TestUtils.graphQueryResult("SELECT (count(?s) as ?count) \n" +
                    "FROM <http://work/dataset2>\n" +
                    "WHERE{      \n" +
                    "   ?s ?p ?o .\n" +
                    "}\n");
            assertEquals("1", getQueryResult(resp, 0, "count"));

        }catch (Exception e){
            fail(e.getMessage());
        } finally {
            TestUtils.dropGraph("http://work/dataset2");
        }
    }

    @Test
    public void apiTestAddDatasetDataBadRequest(){
        try {
            String reqStr = FileUtils.readFileToString(new File(getClass().getResource(
                    "/graphmanagerfixtures/api_graph_add_badrequest.json").toURI()), "UTF-8");
            HttpResponse<JsonNode> addRequest = Unirest.post(TestUtils.getGmService() + "/" + API_VERSION + "/graph/update")
                    .header("accept", "application/json")
                    .header("Content-Type", "application/json")
                    .body(reqStr)
                    .asJson();

            System.out.println(reqStr);
            assertEquals(400, addRequest.getStatus());

        }catch (Exception e){
            fail(e.getMessage());
        }
    }


    @Test
    public void messageTestQuery(){
        try {
            String reqStr = FileUtils.readFileToString(new File(getClass().getResource(
                    "/graphmanagerfixtures/graph_query.json").toURI()), "UTF-8");
            RabbitMQClient client = new RabbitMQClient(TestUtils.getMessageBrokerHost(), "user", "password", "provenance.inbox");

            System.out.println(reqStr);
            String respStr = client.sendSyncServiceMessage(reqStr, "attx.graphManager.inbox", 10000);
            System.out.println("test: " + respStr + "*");
            assertNotEquals("", respStr);

        }catch (Exception e){
            fail(e.getMessage());
        }
    }

    @Test
    public void apiTestQueryDatasetData(){
        try {
            String reqStr = FileUtils.readFileToString(new File(getClass().getResource(
                    "/graphmanagerfixtures/api_graph_query.json").toURI()), "UTF-8");
            HttpResponse<String> queryRequest = Unirest.post(TestUtils.getGmService() + "/" + API_VERSION + "/graph/query")
                    .header("Content-Type", "application/json")
                    .body(reqStr)
                    .asString();

            System.out.println(reqStr);
            assertEquals(200, queryRequest.getStatus());
            System.out.println(queryRequest.getBody());
            assertNotEquals("", queryRequest.getBody());

        }catch (Exception e){
            fail(e.getMessage());
        }
    }


    @Test
    public void messageTestRetrieve(){
        try {
            String reqStr = FileUtils.readFileToString(new File(getClass().getResource(
                    "/graphmanagerfixtures/graph_retrieve.json").toURI()), "UTF-8");
            RabbitMQClient client = new RabbitMQClient(TestUtils.getMessageBrokerHost(), "user", "password", "provenance.inbox");

            System.out.println(reqStr);
            String respStr = client.sendSyncServiceMessage(reqStr, "attx.graphManager.inbox", 10000);
            System.out.println("test: " + respStr + "*");
            assertNotEquals("", respStr);

        }catch (Exception e){
            fail(e.getMessage());
        }
    }

    @Test
    public void apiTestRetrieveDatasetData(){
        try {
            HttpResponse<String> queryRequest = Unirest.get(TestUtils.getGmService() + "/" + API_VERSION + "/graph?uri=" + "http://data.hulib.helsinki" +
                    ".fi/attx/strategy")
                    .header("Content-Type", "application/json")
                    .asString();

            assertEquals(200, queryRequest.getStatus());
            System.out.println(queryRequest.getBody());
            assertNotEquals("", queryRequest.getBody());

        }catch (Exception e){
            fail(e.getMessage());
        }
    }

}
