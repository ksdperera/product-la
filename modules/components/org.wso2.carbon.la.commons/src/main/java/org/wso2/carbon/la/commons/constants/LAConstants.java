/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.la.commons.constants;

/**
 * A class to keep LA related shared constants.
 */
public class LAConstants {

    private LAConstants() {
        
    }
    public static final String LOG_ANALYZER_XML = "repository/conf/log-analyzer.xml";
    public static final String LOG_ANALYZER_STREAM_NAME = "loganalyzer";
    public static final String LOG_ANALYZER_STREAM_VERSION = "1.0.0";
    public static final String LOG_ANALYZER_STREAM_ID = LOG_ANALYZER_STREAM_NAME + ":" + LOG_ANALYZER_STREAM_VERSION;

    public static final String ARBITRARY_FIELD_PREFIX = "_";
    public static final String LOGSTASH_TAGS = "tags";

    public static final String LOG_GROUP = "loggroup";
    public static final String LOG_STREAM = "@logstream";
    public static final String DEFAULT_STREAM = "default";
    public static final String LOG_TIMESTAMP = "@timestamp";
    public static final String LOG_TIMESTAMP_LONG = "@timestamp_long";
    public static final String DAS_TIMESTAMP_FIELD = "_timestamp";
    public static final String LOGSTREAM = "logstream";
    public static final String TIMESTAMP_FIELD = "_eventTimeStamp";

    public static final long EPOCH_DAYGAP = 86400000;

    public static final String EVENT_STREAM_PERSIST_ADMINSERVICE = "EventStreamPersistenceAdminService";

    //Date format constants
    public static final String TIMESTAMP_PATTERN = "yyyy-MM-dd";
    public static final String DAY_PATTERN = "yyyy-MM-dd";
    public static final String WEEK_PATTERN = "Y/MM:W";
    public static final String MONTH_PATTERN = "Y-MM";
    public static final String YEAR_PATTERN = "YYYY";

    // Data-set upload configurations    
    public static final String UPLOAD_SETTINGS = "dataUploadSettings";
    public static final String UPLOAD_LOCATION = "uploadLocation";
    public static final String IN_MEMORY_THRESHOLD = "inMemoryThreshold";
    public static final String UPLOAD_LIMIT = "uploadLimit";
    
    // Summary statistic calculation configurations  
    public static final String SUMMARY_STATISTICS_SETTINGS = "summaryStatisticsSettings";
    public static final String HISTOGRAM_BINS = "histogramBins";
    public static final String CATEGORICAL_THRESHOLD = "categoricalThreshold";
    public static final String SAMPLE_SIZE = "sampleSize";
    
    public static final String PROPERTIES = "properties";
    
    // System property names
    public static final String HOME = "user.home";
    public static final String FILE_SEPARATOR = "file.separator";
    public static final String MIN_PERCENTILE_CONF = "minPercentile";
    public static final String MAX_PERCENTILE_CONF = "maxPercentile";
    
    public static final String USER_HOME= "USER_HOME";
    public static final String DATABASE= "database";

    // Character Encodings
    public static final String UTF_8= "UTF-8";
    public static final String ISO_8859_1= "ISO-8859-1";
    
    public static final String TARGET_HOME_PROP = "target.home";

    // I/O Adapter configs
    public static final String BAM_SERVER_URL= "DASServerURL";
    public static final String BAM_DATA_VALUES = "values";
    
    // file formats
    public static final String CSV = ".csv";
    public static final String TSV = ".tsv";
    public static final String IN_SUFFIX = ".in";
    public static final String OUT_SUFFIX = ".out";

    public static final String LA_DB = "jdbc/WSO2LA_DB";

    //delimiter constants
    public static final String DELIMITER_SPACE = "\\s+";
    public static final String DELIMITER_COMMA = "\\s*,\\s*";
    public static final String DELIMITER_TAB = "\\t";
    public static final String DELIMITER_PIPE = "\\|";
}
