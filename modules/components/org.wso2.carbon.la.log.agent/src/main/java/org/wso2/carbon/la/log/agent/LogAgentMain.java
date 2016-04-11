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
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.carbon.la.log.agent.agentConf.ConfigLogAgent;
import org.wso2.carbon.la.log.agent.data.LogPublisher;
import org.wso2.carbon.la.log.agent.data.LogReader;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;

public class LogAgentMain {
    private static Log log = LogFactory.getLog(LogAgentMain.class);
    private static ConfigLogAgent configLogAgent;
    public static void main(String []args) throws FileNotFoundException {
        Gson gson = new Gson();
        LogPublisher logPublisher;
        setTrustStoreParams();
        JsonReader jsonReader = new JsonReader(new FileReader(loadConfiguration()));
        configLogAgent = gson.fromJson(jsonReader, ConfigLogAgent.class);
        setSchema();
        logPublisher = new LogPublisher(configLogAgent);
        LogReader logReader = new LogReader(logPublisher, configLogAgent.getGroups()[0]);
        logReader.start();
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

    public static void setTrustStoreParams() {
        File file  = new File("../config/client-truststore.jks");
        String trustStorePath = null;
        if(file.exists()){
            trustStorePath = file.getPath();
        }else{
            URL url = LogAgentMain.class.getResource("/client-truststore.jks");
            try {
                trustStorePath = new File(url.toURI()).getAbsolutePath();
            } catch (URISyntaxException e) {
                log.error("Error in loading trust-store key");
            }
        }
        System.setProperty("javax.net.ssl.trustStore",trustStorePath);
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
    }

    public static String loadConfiguration() {
        File file  = new File("../config/agentConfig.json");
        String agentConfigPath = null;
        if(file.exists()){
            agentConfigPath = file.getPath();
        }else{
            URL url = LogAgentMain.class.getResource("/agentConfig.json");
            try {
                agentConfigPath = new File(url.toURI()).getAbsolutePath();
            } catch (URISyntaxException e) {
                log.error("Error in loading config file");
            }
        }
        return agentConfigPath;
    }

    public static void setSchema() {
        try {
            String tableName = configLogAgent.getGroups()[0].getName().toUpperCase();
            JSONObject agentSchema = new JSONObject();
            JSONObject dasSchema = new JSONObject();
            CloseableHttpClient httpClient1 = HttpClientBuilder.create().build();
            try {
                HttpGet request = new HttpGet("https://"+configLogAgent.getOutput().getLoganalyzer().getHost()+":" + configLogAgent.getOutput().getLoganalyzer().getPort() + "/analytics/tables/"+tableName+"/schema");
                request.setHeader("Authorization", "Basic YWRtaW46YWRtaW4=");
                request.setHeader("Content-Type", "application/json");
                HttpResponse response = httpClient1.execute(request);
                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader((response.getEntity().getContent())));
                StringBuffer result = new StringBuffer();
                String line = "";
                while ((line = bufferedReader.readLine()) != null) {
                    result.append(line);
                }
                dasSchema = new JSONObject(result.toString());
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                httpClient1.close();
            }
            String[][] matches = configLogAgent.getGroups()[0].getConfig().getFilter().getRegex().getMatch();

            for (int i = 0; i < matches.length; i++) {
                agentSchema.put("_" + matches[i][0], new JSONObject("{\"type\":\"STRING\",\"isScoreParam\":false,\"isIndex\":true}"));
            }
            agentSchema.put("_trace", new JSONObject("{\"type\":\"STRING\",\"isScoreParam\":false,\"isIndex\":true}"));
            JSONObject mergeSchema = mergeJSONObjects(agentSchema, dasSchema.getJSONObject("columns"));
            dasSchema.put("columns", mergeSchema);

            CloseableHttpClient httpClient2 = HttpClientBuilder.create().build();
            try {
                HttpPost request2 = new HttpPost("https://"+configLogAgent.getOutput().getLoganalyzer().getHost()+":" + configLogAgent.getOutput().getLoganalyzer().getPort() + "/analytics/tables/"+tableName+"/schema");
                StringEntity params = new StringEntity(dasSchema.toString());
                request2.setHeader("Authorization", "Basic YWRtaW46YWRtaW4=");
                request2.setHeader("Content-Type", "application/json");
                request2.setEntity(params);
                HttpResponse response = httpClient2.execute(request2);

                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new RuntimeException(
                            "Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
                }
            } catch (Exception ex) {
            } finally {
                httpClient2.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
