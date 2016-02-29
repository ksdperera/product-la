package org.wso2.carbon.la.log.agent.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by malith on 11/18/15.
 */
public class LogEvent {

    private String host;
    private String message;
    private String type;
    private Long timeStamp;
    private String className;

    private Map<String, String> extractedValues = new HashMap<String, String>();

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setTimeStamp(Long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getTimeStamp() {
        return timeStamp;
    }

    public String getClassName() {
        return className;
    }

    public String getType() {
        return type;
    }

    public Map<String, String> getExtractedValues() {
        return extractedValues;
    }

    public void setExtractedValues(Map<String, String> extractedValues) {
        this.extractedValues = extractedValues;
    }

}
