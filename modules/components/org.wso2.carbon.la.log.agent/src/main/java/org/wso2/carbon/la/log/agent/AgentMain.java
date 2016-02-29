/*
 *
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */

package org.wso2.carbon.la.log.agent;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHeaders;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.carbon.databridge.agent.exception.DataEndpointAgentConfigurationException;
import org.wso2.carbon.databridge.agent.exception.DataEndpointAuthenticationException;
import org.wso2.carbon.databridge.agent.exception.DataEndpointConfigurationException;
import org.wso2.carbon.databridge.agent.exception.DataEndpointException;
import org.wso2.carbon.databridge.commons.exception.TransportException;
import org.wso2.carbon.la.log.agent.agentConf.ConfigLogAgent;
import org.wso2.carbon.la.log.agent.data.LogPublisher;
import org.wso2.carbon.la.log.agent.data.LogReader;
import org.wso2.carbon.la.log.agent.util.DataPublisherUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URISyntaxException;
import java.net.URL;

public class AgentMain {
    public static void main(String[] args) throws FileNotFoundException {

        Log log = LogFactory.getLog(AgentMain.class);
        File jarPath = null;


        final Class<?> referenceClass = AgentMain.class;
        final URL url =
                referenceClass.getProtectionDomain().getCodeSource().getLocation();
        try{
            jarPath = new File(url.toURI()).getParentFile();
        } catch(final URISyntaxException e){
        }

        Gson gson = new Gson();
        LogPublisher logPublisher =null;
//        JsonReader jsonReader = new JsonReader(new FileReader(jarPath+File.separator+ args[0]));
        JsonReader jsonReader = new JsonReader(new FileReader("/home/sajithd/WSO2_LogAnalyzer/product-la/modules/components/org.wso2.carbon.la.log.agent/src/main/resources/agentConfig.json"));

        ConfigLogAgent configLogAgent = gson.fromJson(jsonReader , ConfigLogAgent.class);

        try {

            File jarPath2 = null;

            final Class<?> referenceClass2 = DataPublisherUtil.class;
            final URL url2 =
                    referenceClass2.getProtectionDomain().getCodeSource().getLocation();
            try{
                jarPath2 = new File(url2.toURI()).getParentFile();
            } catch(final URISyntaxException e){
            }
//            System.setProperty("javax.net.ssl.trustStore", jarPath2 + File.separator +"client-truststore.jks");
            System.setProperty("javax.net.ssl.trustStore", "/home/sajithd/WSO2_LogAnalyzer/product-la/modules/components/org.wso2.carbon.la.log.agent/src/main/resources/client-truststore.jks");
            System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");

            WebResource webResource = Client.create(new DefaultClientConfig()).resource("https://localhost:9443/analytics/tables/LOGANALYZER/schema");
            ClientResponse responseDasGet = webResource.type("application/json").header(HttpHeaders.AUTHORIZATION, "Basic " + "YWRtaW46YWRtaW4=").get(ClientResponse.class);
            String dasSchemaJson = responseDasGet.getEntity(String.class);
            JSONObject dasSchema = new JSONObject(dasSchemaJson);
            responseDasGet.close();

            JSONObject agentSchema = new JSONObject();
            agentSchema.put("_eventTimeStamp",new JSONObject("{\"type\":\"STRING\",\"isScoreParam\":false,\"isIndex\":true}"));
            agentSchema.put("_level",new JSONObject("{\"type\":\"STRING\",\"isScoreParam\":false,\"isIndex\":true}"));
            agentSchema.put("_class",new JSONObject("{\"type\":\"STRING\",\"isScoreParam\":false,\"isIndex\":true}"));
            agentSchema.put("_trace",new JSONObject("{\"type\":\"STRING\",\"isScoreParam\":false,\"isIndex\":true}"));
            JSONObject mergeSchema = mergeJSONObjects(agentSchema, dasSchema.getJSONObject("columns"));
            dasSchema.put("columns",mergeSchema);

            ClientResponse response2 = webResource.type("application/json").header(HttpHeaders.AUTHORIZATION, "Basic " + "YWRtaW46YWRtaW4=").post(ClientResponse.class, dasSchema.toString());

            if (response2.getStatus() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + response2.getStatus());
            }

            System.out.println("Output from Server .... \n");
            String output = response2.getEntity(String.class);
            System.out.println(output);
            response2.close();
        } catch (Exception e) {

            e.printStackTrace();

        }

        try {
            logPublisher = new LogPublisher(configLogAgent);
        } catch (DataEndpointAuthenticationException ex) {
            log.error("Error in log Publisher initiating", ex);
        } catch (DataEndpointAgentConfigurationException ex) {
            log.error("Error in log Publisher initiating", ex);
        } catch (TransportException ex) {
            log.error("Error in log Publisher initiating", ex);
        } catch (DataEndpointException ex) {
            log.error("Error in log Publisher initiating", ex);
        } catch (DataEndpointConfigurationException ex) {
            log.error("Error in log Publisher initiating", ex);
        }
        try {
            LogReader logReader =  new LogReader(logPublisher, configLogAgent.getGroups()[0]);
            logReader.start();
        } catch (FileNotFoundException ex) {
            log.error("Error in log Reader initiating", ex);
        }

    }

    public static JSONObject mergeJSONObjects(JSONObject json1, JSONObject json2) {
        JSONObject mergedJSON;
        try {
            mergedJSON = new JSONObject(json2, JSONObject.getNames(json2));
            for (String key : JSONObject.getNames(json1)) {
                mergedJSON.put(key, json1.get(key));
            }

        } catch (JSONException e) {
            throw new RuntimeException("JSON Exception" + e);
        }
        return mergedJSON;
    }
}
