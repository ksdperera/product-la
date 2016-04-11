/**
 * Copyright (c) 2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.la.log.agent.data;

import org.wso2.carbon.databridge.agent.AgentHolder;
import org.wso2.carbon.databridge.agent.DataPublisher;
import org.wso2.carbon.databridge.agent.exception.DataEndpointAgentConfigurationException;
import org.wso2.carbon.databridge.agent.exception.DataEndpointAuthenticationException;
import org.wso2.carbon.databridge.agent.exception.DataEndpointConfigurationException;
import org.wso2.carbon.databridge.agent.exception.DataEndpointException;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.databridge.commons.exception.*;
import org.wso2.carbon.la.log.agent.agentConf.ConfigLogAgent;
import org.wso2.carbon.la.log.agent.util.DataPublisherUtil;
import org.wso2.carbon.la.log.agent.agentConf.Loganalyzer;
import org.wso2.carbon.la.log.agent.util.EventConfigUtil;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.logging.Logger;

public class LogPublisher {
    private static final Logger logger = Logger.getLogger("LogPublisher");
    private Loganalyzer loganalyzer;
    private static DataPublisher dataPublisher;
    private DataPublisherUtil dataPublisherUtil = new DataPublisherUtil();
    private ConfigLogAgent configLogAgent;

    public LogPublisher(ConfigLogAgent configLogAgent){
        this.configLogAgent = configLogAgent;
        this.loganalyzer = configLogAgent.getOutput().getLoganalyzer();
        AgentHolder.setConfigPath(dataPublisherUtil.getDataAgentConfigPath());
        try {
            dataPublisher = new DataPublisher("Thrift", "tcp://" + loganalyzer.getHost() + ":" + loganalyzer.getThrift_port(),
                    "ssl://" + loganalyzer.getHost() + ":" + (loganalyzer.getThrift_port() + 100), loganalyzer.getUser_name(),
                    loganalyzer.getPassword());
        } catch (DataEndpointAgentConfigurationException e) {
            e.printStackTrace();
        } catch (DataEndpointException e) {
            e.printStackTrace();
        } catch (DataEndpointConfigurationException e) {
            e.printStackTrace();
        } catch (DataEndpointAuthenticationException e) {
            e.printStackTrace();
        } catch (TransportException e) {
            e.printStackTrace();
        }
    }

    public void publish(LogEvent logEvent, String streamId) throws FileNotFoundException {
        List<String> payLoadData = Arrays.asList(configLogAgent.getAgentid());
        Map<String, String> arbitraryDataMap = EventConfigUtil.getExtractedDataMap(logEvent);
        arbitraryDataMap.put("message",EventConfigUtil.getEventData(logEvent).toString());
        if (payLoadData != null && arbitraryDataMap != null) {
            Event event = new Event(streamId, System.currentTimeMillis(), null, null, payLoadData.toArray(),
                    arbitraryDataMap);
            dataPublisher.publish(event);
            System.out.println(event.toString());
        }
    }

}
