package org.wso2.carbon.la.log.agent;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.wso2.carbon.databridge.agent.exception.DataEndpointAgentConfigurationException;
import org.wso2.carbon.databridge.agent.exception.DataEndpointAuthenticationException;
import org.wso2.carbon.databridge.agent.exception.DataEndpointConfigurationException;
import org.wso2.carbon.databridge.agent.exception.DataEndpointException;
import org.wso2.carbon.databridge.commons.exception.TransportException;
import org.wso2.carbon.la.log.agent.agentConf.ConfigLogAgent;
import org.wso2.carbon.la.log.agent.data.LogPublisher;

import java.io.FileNotFoundException;
import java.io.FileReader;

/**
 * Created by sajithd on 2/18/16.
 */
public class AgentMain {
    public static void main(String[] args) throws FileNotFoundException {
        Gson gson = new Gson();
        LogPublisher logPublisher =null;
        JsonReader jsonReader = new JsonReader(new FileReader("/home/sajithd/WSO2_LogAnalyzer/product-la/modules/components/org.wso2.carbon.la.log.agent/src/main/resources/agentConfig.json"));

        ConfigLogAgent configLogAgent = gson.fromJson(jsonReader , ConfigLogAgent.class);
        try {
            logPublisher = new LogPublisher(configLogAgent.getOutput().getLoganalyzer());
        } catch (DataEndpointAuthenticationException e) {
            e.printStackTrace();
        } catch (DataEndpointAgentConfigurationException e) {
            e.printStackTrace();
        } catch (TransportException e) {
            e.printStackTrace();
        } catch (DataEndpointException e) {
            e.printStackTrace();
        } catch (DataEndpointConfigurationException e) {
            e.printStackTrace();
        }
        try {
            LogReader logReader =  new LogReader(logPublisher, configLogAgent.getGroups()[0]);
            logReader.start();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }
}
