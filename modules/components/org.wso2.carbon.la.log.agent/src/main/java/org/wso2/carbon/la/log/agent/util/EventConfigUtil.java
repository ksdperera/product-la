/**
 * Copyright (c) 2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.la.log.agent.util;

import org.wso2.carbon.la.log.agent.data.LogEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This is the utility class for fetching event data
 */
public class EventConfigUtil {

    public static List<Object> getEventData(LogEvent logEvent) {
        List<Object> payloadData = new ArrayList<Object>(2);
        payloadData.add(logEvent.getMessage());
        return payloadData;
    }

    public static Map<String, String> getExtractedDataMap(LogEvent logEvent) {
        return logEvent.getExtractedValues();
    }
}
