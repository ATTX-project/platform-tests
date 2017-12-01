package org.uh.attx.platform.test;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;

import java.io.File;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * Created by stefanne on 04/04/17.
 */
public class TestUtils {

    public static String getESSiren() {
        return "http://" + System.getProperty("essiren.host") + ":" + Integer.parseInt(System.getProperty("essiren.port"));
    }

    public static String getES5() {
        return "http://" + System.getProperty("es5.host") + ":" + Integer.parseInt(System.getProperty("es5.port"));
    }

    public static String getMessageBrokerHost() {
        return System.getProperty("messagebroker.host");
    }

    public static String getFuseki() {
        return "http://" + System.getProperty("fuseki.host") + ":" + Integer.parseInt(System.getProperty("fuseki.port"));
    }

    public static String getUV() {
        return "http://" + System.getProperty("frontend.host") + ":" + Integer.parseInt(System.getProperty("frontend.port"));
    }

    public static String getGmService() {
        return "http://" + System.getProperty("graphmanager.host") + ":" + Integer.parseInt(System.getProperty("graphmanager.port"));
    }

    public static String getUVprov() {
        return "http://" + System.getProperty("uvprov.host") + ":" + Integer.parseInt(System.getProperty("uvprov.port"));
    }

    public static String getProvService() {
        return "http://" + System.getProperty("provservice.host") + ":" + Integer.parseInt(System.getProperty("provservice.port"));
    }

    public static String getRMLService() {
        return "http://" + System.getProperty("rmlservice.host") + ":" + Integer.parseInt(System.getProperty("rmlservice.port"));
    }

    public static final String VERSION = "/0.2";

    public static final String API_USERNAME = "master";
    public static final String API_PASSWORD = "commander";
    public static final String ACTIVITY = "{ \"debugging\" : \"false\", \"userExternalId\" : \"admin\" }";

    public static JSONObject getQueryResultField(HttpResponse<JsonNode> response, String field) {
        JSONObject queryObject = response.getBody().getObject().getJSONObject("results");
        return queryObject.getJSONArray("bindings").getJSONObject(0).getJSONObject(field);
    }

    public static HttpResponse<JsonNode> graphQueryResult(String query) {
        String URL = getFuseki() + "/test/query";
        HttpResponse<JsonNode> queryResponse = null;
        try {
            queryResponse = Unirest.post(URL)
                    .header("Content-Type", "application/sparql-query")
                    .header("Accept", "application/sparql-results+json")
                    .body(query)
                    .asJson();
        } catch (UnirestException e) {
            e.printStackTrace();
            fail("Could not query Graph Store at:" + URL);
        }
        return queryResponse;
    }

    public static int importPipeline(URL resource) {
        int pipelineID = 0;
        try {
            HttpResponse<JsonNode> pipelineRequest = Unirest.post(getUV() + "/master/api/1/pipelines/import")
                    .header("accept", "application/json")
                    .basicAuth(API_USERNAME, API_PASSWORD)
                    .field("importUserData", false)
                    .field("importSchedule", false)
                    .field("file", new File(resource.toURI()))
                    .asJson();
            assertEquals(200, pipelineRequest.getStatus());
            JSONObject pipelineObject = pipelineRequest.getBody().getObject();
            System.out.println(pipelineObject);
            pipelineID = pipelineObject.getInt("id");
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Could not import pipeline resource:" + resource);
        }
        return pipelineID;
    }

    public static Callable<String> pollForWorkflowExecution(Integer pipelineID) {
        return new Callable<String>() {
            public String call() throws Exception {
                String URL = String.format(getUV() + "/master/api/1/pipelines/%s/executions/last", pipelineID.intValue());
                HttpResponse<JsonNode> schedulePipelineResponse = Unirest.get(URL)
                        .header("accept", "application/json")
                        .header("Content-Type", "application/json")
                        .basicAuth(API_USERNAME, API_PASSWORD)
                        .asJson();
                if (schedulePipelineResponse.getStatus() == 200) {
                    JSONObject execution = schedulePipelineResponse.getBody().getObject();
                    String status = execution.getString("status");
                    System.out.println(status);
                    return status;
                } else {
                    return "Not yet";
                }
            }
        };
    }

    public static Callable<Integer> pollForWorkflowStart(Integer pipelineID) {
        return new Callable<Integer>() {
            public Integer call() throws Exception {
                String URL = String.format(getUV() + "/master/api/1/pipelines/%s/executions", pipelineID);
                HttpResponse<JsonNode> workflowStart = Unirest.post(URL)
                        .header("accept", "application/json")
                        .header("Content-Type", "application/json")
                        .basicAuth(API_USERNAME, API_PASSWORD)
                        .body(ACTIVITY)
                        .asJson();
                JSONObject execution = workflowStart.getBody().getObject();
                String status = execution.getString("status");
                System.out.println(status);
                return workflowStart.getStatus();
            }
        };
    }


    public static Callable<Boolean> askQueryAnswer(String query) {
        return new Callable<Boolean>() {
            public Boolean call() throws Exception {
                boolean queryResult = false;
                try {
                    HttpResponse<JsonNode> queryWork = graphQueryResult(query);
                    System.out.println(queryWork.getBody().getObject().getBoolean("boolean"));
                    queryResult = queryWork.getBody().getObject().getBoolean("boolean");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    fail("Query not true.\n" + query);
                }
                return queryResult;
            }
        };
    }

    public static void askGraphStoreIfTrue(String query) throws Exception {
        await().atMost(120, TimeUnit.SECONDS).until(askQueryAnswer(query), equalTo(true));
    }

    public static void askGraphStoreIfTrue(String query, int timeout) throws Exception {
        await().atMost(timeout, TimeUnit.SECONDS).until(askQueryAnswer(query), equalTo(true));
    }

    public static Callable<String> pollForIndexStatus(Integer createdID) {
        return new Callable<String>() {
            public String call() throws Exception {
                String URL = String.format(getGmService() + VERSION + "/index/%s", createdID);
                GetRequest get = Unirest.get(URL);
                HttpResponse<JsonNode> response1 = get.asJson();
                JSONObject myObj = response1.getBody().getObject();
                String status = myObj.getString("status");
                System.out.println(status);
                return status;
            }
        };
    }

    public static Callable<Integer> waitForESResults(String esEndpoint, String esIndex) {
        return new Callable<Integer>() {
            public Integer call() throws Exception {
                int totalHits = 0;
                Unirest.post(esEndpoint + "/"+ esIndex +"/_refresh");
                HttpResponse<com.mashape.unirest.http.JsonNode> jsonResponse = Unirest.get(esEndpoint + "/"+ esIndex +"/_search?q=*")
                        .asJson();

                JSONObject esObj = jsonResponse.getBody().getObject();
                if(esObj.has("hits")) {
                    totalHits = esObj.getJSONObject("hits").getInt("total");
                }
                System.out.println(esEndpoint + ": " + totalHits);
                return totalHits;
            }
        };
    }   
    
    public static void clearProvData() {
        dropGraph("http://data.hulib.helsinki.fi/attx/prov");
    }
    
    public static void dropGraph(String graph) {
        try {
            // drop prov graph
            HttpResponse<String> deleteGraph = Unirest.post(TestUtils.getFuseki() + "/test/update")
                    .header("Content-Type", "application/sparql-update")
                    .body("drop graph <" + graph + ">")
                    .asString();
            
            assertEquals(204, deleteGraph.getStatus());
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Drop graph failed. "+ ex.getMessage());
        }        
    }
    

    public static void testUVprovHealth() {
        TestUtils.testHealth(TestUtils.getUVprov() + "/health");
    }
    
    public static void testGmHealth() {
        TestUtils.testHealth(TestUtils.getGmService() + "/health");
    }

    public static void testProvServiceHealth() {
        TestUtils.testHealth(TestUtils.getProvService() + "/health");
    }
    
    private static void testHealth(String endpoint) {
        try {
            HttpResponse<JsonNode> health = Unirest.get(endpoint).asJson();
            assertEquals(200, health.getStatus());
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Health check failed. "+ ex.getMessage());
        }
    }
}
