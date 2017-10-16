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
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.uh.hulib.attx.dev.TestUtils;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.uh.hulib.attx.wc.uv.common.RabbitMQClient;
import org.uh.hulib.attx.wc.uv.common.pojos.RMLServiceInput;
import org.uh.hulib.attx.wc.uv.common.pojos.RMLServiceRequest;
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
        /*
        RabbitMQClient c = new RabbitMQClient("localhost","user", "password", "provenance.inbox");        
       
        System.out.println(reqStr);
        String respStr = c.sendSyncServiceMessage(reqStr, "rmlservice", 10000);
        System.out.println("test: " + respStr + "*");
        assertNotEquals("", respStr);
*/
        
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("user");
        factory.setPassword("password");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        
        channel.queueDeclare("test2", true, true, false, null);
        String rq = "test2";
        final BlockingQueue<String> response = new ArrayBlockingQueue<String>(1);

        channel.basicConsume(rq, true, new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                
//                if (properties.getCorrelationId().equals(corrId)) {
                    response.offer(new String(body, "UTF-8"));
//                }
            }
        });

        String res = response.poll(5000, TimeUnit.MILLISECONDS);             
        System.out.println(res);
    
        res = response.poll(5000, TimeUnit.MILLISECONDS);             
        System.out.println(res);
          
    }
    @Test
    @Ignore
    public void testTranformationRest() throws Exception {
        checkHealth();
        RMLServiceRequest req = getWorkingRequest();
        HttpResponse<JsonNode> resp = Unirest.post(TestUtils.getRMLService() + "/" + API_VERSION + "/transform")
                .header("accept", "application/json")
                .header("content-type", "application/json")
                .body(req)
                .asJson();
        
        assertEquals(200, resp.getStatus());
        
        System.out.println(resp.getBody().getObject().toString(2));
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
