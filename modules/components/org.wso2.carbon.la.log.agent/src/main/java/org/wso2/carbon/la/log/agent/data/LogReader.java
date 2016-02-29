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

package org.wso2.carbon.la.log.agent.data;

import java.io.*;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.wso2.carbon.databridge.commons.utils.DataBridgeCommonsUtils;
import org.wso2.carbon.la.log.agent.agentConf.Filter;
import org.wso2.carbon.la.log.agent.agentConf.GroupElement;

/**
 * Reads log line by line
 */
public class LogReader {

    private static final Logger logger = Logger.getLogger("LogReader");
    private final File file;
    private long offset = 0;
    private int lineCount = 0;
    private boolean ended = false;
    private WatchService watchService = null;
    ArrayDeque<String> lines = new ArrayDeque<String>();
    private boolean readFromTop = true;
    private LogPublisher logPublisher;
    private String streamId;
    private GroupElement groupElement;
    private String stringBuffer;
    private boolean exFlag = false;
    private LogEvent oldPersistLog = null;
    private Long timestamp;

    /**
     * Allows output of a file that is being updated by another process.
     *
     * @param groupElement
     * @param logPublisher
     */
    public LogReader(LogPublisher logPublisher, GroupElement groupElement) throws FileNotFoundException {
        this.groupElement = groupElement;
        this.file = new File(this.groupElement.getConfig().getInput().getFile().getPath());
        setStreamId();
        this.logPublisher = logPublisher;
    }

    /**
     * Start watch.
     */
    public void start() {
        updateOffset();
        // listens for FS events
        new Thread(new LogWatcher()).start();

        new Thread(new TailLog()).start();

    }

    /**
     * Stop watch.
     */
    public void stop() {
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException ex) {
                logger.info("Error closing watch service");
            }
            watchService = null;
        }
    }

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId() {
        this.streamId = DataBridgeCommonsUtils.generateStreamId(groupElement.getName(), groupElement.getVersion());
        ;
    }

    private synchronized void updateOffset() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            br.skip(offset);
            while (true) {
                String line = br.readLine();
                if (line != null) {
                    if (isReadFromTop()) {
                        lines.push(line);
                    }
                    // this may need tweaking if >1 line terminator char
                    offset += line.length() + 1;
                } else {
                    break;
                }
            }
            br.close();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error reading", ex);
        }
    }

    /**
     * @return true if lines are available to read
     */
    public boolean linesAvailable() {
        return !lines.isEmpty();
    }

    /**
     * @return next unread line
     */
    public synchronized String getLine() {
        if (lines.isEmpty()) {
            return null;
        } else {
            lineCount++;
            return lines.removeLast();
        }
    }

    /**
     * @return true if no more lines will ever be available,
     * because stop() has been called or the timeout has expired
     */
    public boolean hasEnded() {
        return ended;
    }

    /**
     * @return next line that will be returned; zero-based
     */
    public int getLineNumber() {
        return lineCount;
    }

    private class TailLog implements Runnable {
        public void run() {
            while (!hasEnded()) {
                while (linesAvailable()) {
                    try {
                        String logLine = getLine();
                        if (logLine != null && logLine != "") {
                            LogEvent logEvent = constructLogEvent(logLine);
                            if (logEvent.getExtractedValues().containsValue("ERROR")) {
                                if (oldPersistLog == null) {
                                    oldPersistLog = logEvent;
                                } else {
                                    LogEvent logEvent1 = new LogEvent();
                                    logEvent1.setMessage(oldPersistLog.getMessage());
                                    logEvent1.setExtractedValues(oldPersistLog.getExtractedValues());
                                    logPublisher.publish(logEvent1, streamId);
                                    oldPersistLog = logEvent;
                                }
                            }
                            if (exFlag && stringBuffer != null) {
                                Map<String, String> persistMap = oldPersistLog.getExtractedValues();
                                persistMap.put("trace", stringBuffer.toString());
                                LogEvent logEvent2 = new LogEvent();
                                logEvent2.setMessage(oldPersistLog.getMessage());
                                logEvent2.setExtractedValues(persistMap);
                                logPublisher.publish(logEvent2, streamId);
                                stringBuffer = null;
                                exFlag = false;
                            }
                            if (stringBuffer == null && logEvent != null && !logEvent.getExtractedValues()
                                    .containsValue("ERROR")) {
                                if (oldPersistLog != null) {
                                    LogEvent logEvent3 = new LogEvent();
                                    logEvent3.setMessage(oldPersistLog.getMessage());
                                    logEvent3.setExtractedValues(oldPersistLog.getExtractedValues());
                                    logPublisher.publish(logEvent3, streamId);
                                    oldPersistLog = null;
                                }
                                logPublisher.publish(logEvent, streamId);
                            }
                        }
                    } catch (FileNotFoundException ex) {
                        logger.log(Level.SEVERE, "Error reading", ex);
                    }
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    logger.log(Level.SEVERE, "Tail interrupted", ex);
                }
            }
        }
    }

    private class LogWatcher implements Runnable {
        private final Path path = file.toPath().getParent();

        public void run() {
            try {
                setReadFromTop(true);
                watchService = path.getFileSystem().newWatchService();
                path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                while (true) {
                    WatchKey watchKey = watchService.take();
                    if (!watchKey.reset()) {
                        stop();
                        break;
                    } else if (!watchKey.pollEvents().isEmpty()) {
                        updateOffset();
                    }
                    Thread.sleep(500);
                }
            } catch (InterruptedException ex) {
                logger.log(Level.SEVERE, "Tail interrupted", ex);
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Tail failed", ex);
            } catch (ClosedWatchServiceException ex) {
                // no warning required - this was a call to stop()
            }
            ended = true;
        }
    }

    public boolean isReadFromTop() {
        return readFromTop;
    }

    public void setReadFromTop(boolean readFromTop) {
        this.readFromTop = readFromTop;
    }

    private LogEvent constructLogEvent(String logLine) {
        LogEvent logEvent = new LogEvent();
        logEvent.setMessage(logLine);
        logEvent.setExtractedValues(applyFilters(groupElement.getConfig().getFilter(), logLine));
        logEvent.setTimeStamp(timestamp);
        return logEvent;
    }

    private Map<String, String> applyFilters(Filter filter, String logLine) {
        Map<String, String> matchMap = new HashMap<String, String>();
        for (int i = 0; i < filter.getRegex().getMatch().length; i++) {
            String value = processRegEx(logLine, filter.getRegex().getMatch()[i][1]);
            if (value != null) {
                if(filter.getRegex().getMatch()[i][0].contains("eventTimeStamp")){
                    SimpleDateFormat sdf  = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                    Date date = null;
                    try {
                        date = sdf.parse(value);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    this.timestamp = date.getTime();
                    matchMap.put(filter.getRegex().getMatch()[i][0], String.valueOf(timestamp));
                }else{
                    matchMap.put(filter.getRegex().getMatch()[i][0], value);
                }
                exFlag = true;
            } else {
                stringBuffer = stringBuffer + logLine;
                exFlag = false;
            }
        }
        return matchMap;
    }

    private String processRegEx(String logLine, String regEx) {
        String value = null;
        Pattern pattern = Pattern.compile(regEx);
        Matcher matcher = pattern.matcher(logLine);
        if (matcher.find()) {
            value = matcher.group(0).toString();
        }
        return value;
    }
}
