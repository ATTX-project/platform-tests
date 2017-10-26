/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uh.attx.platform.test.uv;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import java.net.URL;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import junit.framework.TestCase;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.IsEqual.equalTo;
import org.json.JSONArray;
import org.json.JSONObject;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.uh.attx.platform.test.TestUtils;

/**
 *
 * @author jkesanie
 */
@RunWith(JUnitPlatform.class)
public class IngestionPipelineTest {

    private static int pipelineID = -1;
    private String graphURI = null;
    
    
    @BeforeClass
    public static void importPipeline() {
        try {
            // chekc if pipeline exists
            pipelineID = getExistingPipelineID("ingestionpipeline1");
            if (pipelineID < 0) {
                URL pipelinePackage = IngestionPipelineTest.class.getResource("/uvfixtures/ingestionpipeline1.zip");
                pipelineID = TestUtils.importPipeline(pipelinePackage);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Could not import required pipelines");
        }
    }

    @Before
    public void cleanupData() {
        // clear prov data 
        TestUtils.dropGraph("http://data.hulib.helsinki.fi/attx/prov");
        // clear working data
        
        HttpResponse<JsonNode> resp = TestUtils.graphQueryResult("SELECT ?g \n" +
"WHERE {\n" +
"    graph ?g {        \n" +
"    }\n" +
"    filter(strStarts(str(?g), \"http://data.hulib.helsinki.fi/attx/work/wf_\"))\n" +
"}");
        JSONArray results = resp.getBody().getObject().getJSONObject("results").getJSONArray("bindings");        
        for(int i = 0; i < results.length(); i++) {
            this.graphURI = results.getJSONObject(i).getJSONObject("g").getString("value");            
            TestUtils.dropGraph(this.graphURI);
        }
        
        
    }

    @Test
    public void testRunPipeline() {

        try {
            // schedule executions
            await().atMost(20, TimeUnit.SECONDS).until(TestUtils.pollForWorkflowStart(pipelineID), equalTo(200));
            // wait for success
            await().atMost(240, TimeUnit.SECONDS).until(TestUtils.pollForWorkflowExecution(pipelineID), equalTo("FINISHED_SUCCESS"));

            // some working data was generated
            TestUtils.askGraphStoreIfTrue("ASK\n" +
"WHERE {\n" +
"  graph <" + this.graphURI + "> {\n" +
"    ?s ?p ?o\n" +
"  }\n" +
"} ");
                       
            // There are 92 subjects in the working data
            HttpResponse<JsonNode> resp = TestUtils.graphQueryResult("SELECT (count(?s) as ?count) \n" +
                        "FROM <" + this.graphURI +">\n" +
                        "WHERE{      \n" +
                        "   ?s ?p ?o .\n" +
                        "}\n");
                assertEquals("92", getQueryResult(resp, 0, "count"));            
            
                
            // some provenance data was generated    
            TestUtils.askGraphStoreIfTrue("ASK\n" +
"WHERE {\n" +
"  graph <http://data.hulib.helsinki.fi/attx/prov> {\n" +
"    ?s ?p ?o\n" +
"  }\n" +
"} ");
            
            // trigger uvprov 
            
            //resp = Unirest.get(TestUtils.getUVprov() + "/provjob").asJson();
            
            
        } catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
    }

    private static int getExistingPipelineID(String name) throws Exception {
        // using the UV rest API and trying to add the plugin again. Should response with an error.
        HttpResponse<JsonNode> response = Unirest.get(TestUtils.getUV() + "/master/api/1/pipelines/visible?userExternalId=admin")
                .header("accept", "application/json")
                .basicAuth(TestUtils.API_USERNAME, TestUtils.API_PASSWORD)
                .asJson();
        TestCase.assertEquals(200, response.getStatus());
        System.out.println("**" + response.getBody().toString());
        Iterator<Object> i = response.getBody().getArray().iterator();
        while (i.hasNext()) {
            JSONObject o = (JSONObject) i.next();
            if (o.getString("name").equals(name)) {
                return o.getInt("id");
            }
        }
        return -1;
    }

    private String getQueryResult(HttpResponse<JsonNode> resp, int index, String key) {
        JSONObject item = resp.getBody().getObject().getJSONObject("results").getJSONArray("bindings").getJSONObject(index);
        return item.getJSONObject(key).getString("value");
    }
}
