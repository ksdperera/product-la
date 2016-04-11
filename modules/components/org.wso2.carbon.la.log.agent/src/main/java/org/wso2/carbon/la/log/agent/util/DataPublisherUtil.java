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

package org.wso2.carbon.la.log.agent.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

public class DataPublisherUtil {
    private static Log log = LogFactory.getLog(DataPublisherUtil.class);

    public String getDataAgentConfigPath() {
        File file  = new File("../config/data-agent-conf.xml");
        String thriftConfigPath = null;
        if(file.exists()){
            thriftConfigPath = file.getPath();
        }else{
            URL url = DataPublisherUtil.class.getResource("/agentConfig.json");
            try {
                thriftConfigPath = new File(url.toURI()).getAbsolutePath();
            } catch (URISyntaxException e) {
                log.error("Error in loading thrift config file");
            }
        }
        return thriftConfigPath;
    }
}
