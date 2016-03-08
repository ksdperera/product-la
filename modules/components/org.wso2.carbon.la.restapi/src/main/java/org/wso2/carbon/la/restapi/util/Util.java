package org.wso2.carbon.la.restapi.util;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.analytics.dataservice.commons.AnalyticsDataResponse;
import org.wso2.carbon.analytics.dataservice.commons.SearchResultEntry;
import org.wso2.carbon.analytics.dataservice.core.SecureAnalyticsDataService;
import org.wso2.carbon.analytics.datasource.commons.Record;
import org.wso2.carbon.analytics.datasource.commons.RecordGroup;
import org.wso2.carbon.analytics.datasource.commons.exception.AnalyticsException;
import org.wso2.carbon.la.commons.constants.LAConstants;
import org.wso2.carbon.la.commons.domain.RecordBean;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by vithulan on 2/3/16.
 */
public class Util {
    private static final Log log = LogFactory.getLog(Util.class);

    /**
     * Creates list of iterators of search result entries
     * @param resp analytic data response from analytics data API
     * @param analyticsDataService analytics data API
     * @return returns list of iterators
     * @throws AnalyticsException
     */
    public static List<Iterator<Record>> getRecordIterators(AnalyticsDataResponse resp,
                                                            SecureAnalyticsDataService analyticsDataService)
            throws AnalyticsException {
        List<Iterator<Record>> iterators = new ArrayList<>();
        for (RecordGroup recordGroup : resp.getRecordGroups()) {
            iterators.add(analyticsDataService.readRecords(resp.getRecordStoreName(), recordGroup));
        }
        return iterators;
    }

    /**
     * Gets the record bean of an entry
     * @param record search result entry
     * @return returns RecordBean object
     */
    public static RecordBean createRecordBean(Record record) {
        RecordBean recordBean = new RecordBean();
        recordBean.setId(record.getId());
        recordBean.setTableName(record.getTableName());
        recordBean.setTimestamp(record.getTimestamp());
        recordBean.setValues(record.getValues());
        return recordBean;
    }

    /**
     * Appends timestamp with search query
     * @param query search query
     * @param timeFrom time from where results are required
     * @param timeTo upto the time where results are required
     * @return String search query
     */
    public static String appendTimeStamp(String query, String timeFrom, String timeTo){
        String pattern = "MM/dd/yyyy";
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        Date fromDate =null;
        Date toDate = null;
        try {
            fromDate = format.parse(timeFrom);
            toDate = format.parse(timeTo);
        } catch (ParseException e) {
            log.error(e);
            e.printStackTrace();
        }
        long timeFromEpoch = fromDate.getTime();    //TODO how to avoid null pointer exception?
        long timeToEpoch = toDate.getTime();
        String searchQuery = "";
        if (!"".equals(query)) {
            searchQuery = query + " AND ";
        }
        return searchQuery + LAConstants.TIMESTAMP_FIELD+" :[" + timeFromEpoch + " TO " + timeToEpoch + "]";
    }

    /**
     * Groups all the result entries based on entry name and gets number of Hits
     * @param iterators List of iterators of search result entries
     * @param ColumnName Column name where results are required
     * @return returns a Map
     */
    public static Map<String, Integer> getCountGroup(List<Iterator<Record>> iterators, String ColumnName){
        Map<String, Object> values;
        Map<String, Integer> counter = new HashMap<>();

        int count = 0;
        String val;

        for (Iterator<Record> iterator : iterators) {
            while (iterator.hasNext()) {
                RecordBean recordBean = Util.createRecordBean(iterator.next());
                values = recordBean.getValues();

                if (values.get(ColumnName) == null) {

                    if (!counter.containsKey("NULLVALUE")) {
                        counter.put("NULLVALUE", 1);
                    } else {
                        count = counter.get("NULLVALUE");
                        count++;
                        counter.put("NULLVALUE", count);
                    }

                } else {
                    val = values.get(ColumnName).toString();
                    if (!counter.containsKey(val)) {
                        counter.put(val, 1);
                    } else {
                        count = counter.get(val);
                        count++;
                        counter.put(val, count);
                    }
                }

                if (log.isDebugEnabled()) {
                    log.debug("Retrieved -- Record Id: " + recordBean.getId() + " values :" +
                            recordBean.toString());
                }
            }
        }
        return counter;
    }

}
