package org.wso2.carbon.la.log.agent;

import java.io.*;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.wso2.carbon.databridge.commons.utils.DataBridgeCommonsUtils;
import org.wso2.carbon.la.log.agent.agentConf.Filter;
import org.wso2.carbon.la.log.agent.agentConf.GroupElement;
import org.wso2.carbon.la.log.agent.agentConf.Input;
import org.wso2.carbon.la.log.agent.conf.LogGroup;
import org.wso2.carbon.la.log.agent.conf.ServerConfig;
import org.wso2.carbon.la.log.agent.data.LogEvent;
import org.wso2.carbon.la.log.agent.data.LogPublisher;
import org.wso2.carbon.la.log.agent.filters.AbstractFilter;
import org.wso2.carbon.la.log.agent.util.PublisherUtil;

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
    private boolean exFlag =false;
    private String lastLogLine = null;
    private LogEvent persistLog = null;

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
        this.logPublisher=logPublisher;
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
        this.streamId =DataBridgeCommonsUtils.generateStreamId(groupElement.getName(),groupElement.getVersion()); ;
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
                    //System.out.println(getLineNumber() + ": " + getLine());
                    try {
                        String logLine = getLine();
                        if (logLine != null && logLine != "") {
//                            logPublisher.publish(constructLogEvent(logLine), streamId);
                            LogEvent logEvent = constructLogEvent(logLine);
                            if(stringBuffer==null && !logEvent.getExtractedValues().containsValue("ERROR")){
                                logPublisher.publish(logEvent, streamId);
                            }
                            if(logEvent.getExtractedValues().containsValue("ERROR")){
                                persistLog = logEvent;
                            }
                            if(exFlag && stringBuffer!=null){
                                Map<String, String> persistMap = persistLog.getExtractedValues();
                                persistMap.put("trace",stringBuffer.toString());
                                logEvent.setMessage(persistLog.getMessage());
                                logEvent.setExtractedValues(persistMap);
                                logPublisher.publish(logEvent, streamId);
                                stringBuffer = null;
                                exFlag =false;
                                persistLog = null;
                            }
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
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
                logger.info("Tail interrupted");
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
        return logEvent;
    }

    private Map<String, String> applyFilters(Filter filter, String logLine){
        Map<String, String> matchMap = new HashMap<String, String>();
        for (int i=0;i<filter.getRegex().getMatch().length;i++)
        {
            String value = processRegEx(logLine,filter.getRegex().getMatch()[i][1]);
            if(value!=null){
                matchMap.put(filter.getRegex().getMatch()[i][0], value);
                exFlag =true;
            }else{
                stringBuffer = stringBuffer + logLine;
                exFlag =false;
            }
        }
        return matchMap;
    }

    private String processRegEx(String logLine, String regEx){
        String value = null;
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(logLine);
        if (m.find())
        {
            value=m.group(0).toString();
        }
        return  value;
    }
}
