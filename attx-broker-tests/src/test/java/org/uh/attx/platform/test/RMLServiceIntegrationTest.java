/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uh.attx.platform.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.uh.hulib.attx.wc.uv.common.RabbitMQClient;
import org.uh.hulib.attx.wc.uv.common.pojos.RMLServiceInput;
import org.uh.hulib.attx.wc.uv.common.pojos.RMLServiceRequest;
import org.uh.hulib.attx.wc.uv.common.pojos.RMLServiceResponse;
import org.uh.hulib.attx.wc.uv.common.pojos.prov.Context;
import org.uh.hulib.attx.wc.uv.common.pojos.prov.Provenance;

/**
 *
 * @author jkesanie
 */
@RunWith(JUnitPlatform.class)
public class RMLServiceIntegrationTest {

    public final String API_VERSION = "0.1";

    public RMLServiceIntegrationTest() {
        Unirest.setObjectMapper(new ObjectMapper() {
            private com.fasterxml.jackson.databind.ObjectMapper jacksonObjectMapper
                    = new com.fasterxml.jackson.databind.ObjectMapper();

            public <T> T readValue(String value, Class<T> valueType) {
                try {
                    return jacksonObjectMapper.readValue(value, valueType);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            public String writeValue(Object value) {
                try {
                    return jacksonObjectMapper.writeValueAsString(value);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Before
    public void checkHealth() {
        try {
            HttpResponse<JsonNode> resp = Unirest.get(TestUtils.getRMLService() + "/health").asJson();
            assertEquals("UP", resp.getBody().getObject().get("status"));
        } catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }

    }

    @Test 
    public void testTransformationAMQP() throws Exception {
        checkHealth();
        RMLServiceRequest req = getWorkingRequest();
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
         String reqStr = mapper.writeValueAsString(req);

        RabbitMQClient c = new RabbitMQClient(TestUtils.getMessageBrokerHost(),"user", "password", "provenance.inbox");
        
        String respStr = c.sendSyncServiceMessage(reqStr, "rmlservice", 10000);        
        assertNotEquals(null, respStr);
        assertNotEquals("", respStr);
        assertNotEquals(null, respStr);
        
        RMLServiceResponse resp = mapper.readValue(respStr, RMLServiceResponse.class);
        
        assertEquals("SUCCESS", resp.getPayload().getStatus());
    }

    @Test
    public void testTranformationRest() throws Exception {
        checkHealth();
        RMLServiceRequest req = getWorkingRequest();
        HttpResponse<JsonNode> resp = Unirest.post(TestUtils.getRMLService() + "/" + API_VERSION + "/transform")
                .header("accept", "application/json")
                .header("content-type", "application/json")
                .body(req)
                .asJson();
        
        assertEquals(200, resp.getStatus());
                
        assertEquals("SUCCESS", resp.getBody().getObject().getJSONObject("payload").get("status"));
        
        // TODO: check that content is ok
    }
    
    private RMLServiceRequest getWorkingRequest() throws Exception {
        String mappingStr = FileUtils.readFileToString(new File(getClass().getResource(
                "/rmlfixtures/etsin-org-map.ttl").toURI()), "UTF-8");

        String inputStr = FileUtils.readFileToString(new File(getClass().getResource(
                "/rmlfixtures/etsin-org-data.json").toURI()), "UTF-8");

        RMLServiceRequest req = new RMLServiceRequest();
        req.setProvenance(getProvenance());

        RMLServiceInput payload = new RMLServiceInput();

        payload.setType("Data");
        payload.setMapping(mappingStr);
        payload.setInput(inputStr);
        req.setPayload(payload);
        
        return req;
    }

    private Provenance getProvenance() {
        Provenance p = new Provenance();
        Context ctx = new Context();
        ctx.setActivityID("act");
        ctx.setStepID("step");
        ctx.setWorkflowID("wf");
        p.setContext(ctx);
        return p;
    }
}
