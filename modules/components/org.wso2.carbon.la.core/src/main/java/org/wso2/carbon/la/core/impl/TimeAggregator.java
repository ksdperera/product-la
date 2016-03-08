package org.wso2.carbon.la.core.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.analytics.datasource.commons.Record;
import org.wso2.carbon.la.commons.constants.LAConstants;
import org.wso2.carbon.la.commons.domain.RecordBean;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by vithulan on 3/4/16.
 */
public class TimeAggregator {

    private static final Log log = LogFactory.getLog(TimeAggregator.class);
    /**
     * final variables to group entries in a time based way.
     */
    private static final String day = "day";
    private static final String week = "week";
    private static final String month = "month";
    private static final String year = "year";
    private static final String auto = "auto";

    /**
     * Groups the entries as @param groupBy
     * @param iterators list of iterators of search result entry
     * @param columnName column name that result is wanted to be
     * @param groupBy   way of grouping
     * @return returns a map of column entries which are grouped in groupBy method
     */
    public Map<String, Map<String, Integer>> getGrouped(List<Iterator<Record>> iterators,String columnName, String groupBy){
        Map<Long, Map<String, Integer>> sortedDayMap = getBasicGrouped(iterators, columnName);
        Map<Long, Map<String, Integer>> sortedAllDayMap = getAllDayMap(sortedDayMap);
        Map<String, Map<String, Integer>> groupedMap = getGrouping(sortedAllDayMap, groupBy);
        return new TreeMap<>(groupedMap);
    }

    /**
     * Groups the entries by day
     * @param iterators list of iterators of search result entry
     * @param columnName column name that result is wanted to be
     * @return returns a map of entries which are grouped by day
     */
    private Map<Long, Map<String, Integer>> getBasicGrouped(List<Iterator<Record>> iterators, String columnName){
        SimpleDateFormat format = new SimpleDateFormat(LAConstants.TIMESTAMP_PATTERN);
        Map<String, Object> values;
        Map<Long, Map<String, Integer>> stamper = new HashMap<>();
        int count = 0;
        String val ;
        for (Iterator<Record> iterator : iterators) {
            while (iterator.hasNext()) {
                RecordBean recordBean = createRecordBean(iterator.next());
                values = recordBean.getValues();
                long epoch1 = Long.parseLong(values.get(LAConstants.TIMESTAMP_FIELD).toString());
                Date expiry = new Date(epoch1);
                String str_date = format.format(expiry);

                Date date = null;
                try {
                    date = format.parse(str_date);
                } catch (ParseException e) {
                    String msg = String.format("Error occurred while parsing date");
                    log.error(msg,e);
                }
                long epoch = date.getTime();

                if (values.get(columnName) != null) {
                    val = values.get(columnName).toString();

                    if (!stamper.containsKey(epoch)) {
                        Map<String, Integer> counter = new HashMap<String, Integer>();
                        counter.put(val, 1);
                        stamper.put(epoch, counter);
                    } else {
                        Map<String, Integer> counter = stamper.get(epoch);
                        if (!counter.containsKey(val)) {
                            counter.put(val, 1);
                            stamper.put(epoch, counter);
                        } else {
                            count = counter.get(val);
                            count++;
                            counter.put(val, count);
                            stamper.put(epoch, counter);
                        }
                    }

                }
                if (log.isDebugEnabled()) {
                    log.debug("Retrieved -- Record Id: " + recordBean.getId() + " values :" +
                            recordBean.toString());
                }
            }
        }
        return new TreeMap<>(stamper);
    }

    /**
     * Adds missing days to the @param sortedDayMap
     * @param sortedDayMap Map of entries which are grouped sorted in day basis
     * @return Map of entries which are grouped and sorted
     */
    public Map<Long, Map<String, Integer>> getAllDayMap(Map<Long, Map<String, Integer>> sortedDayMap){
        Map<Long, Map<String, Integer>> allDayMap = new HashMap<>();
        int j = 1;
        long lastDay = 0;
        long presentDay = 0;
        long k = 1;

        for (Map.Entry<Long, Map<String, Integer>> entry : sortedDayMap.entrySet()) {

            if (j > 1) {
                presentDay = entry.getKey();
                long dif = presentDay - lastDay;
                k = dif / LAConstants.EPOCH_DAYGAP;

            }
            if (k > 1) {
                long newDate = lastDay;
                while (k > 1) {
                    newDate = newDate + LAConstants.EPOCH_DAYGAP;
                    Map<String, Integer> tempMap = new HashMap<>();
                    tempMap.put("No Entry", 0);
                    allDayMap.put(newDate, tempMap);
                    k--;
                }
                k = 1;
                lastDay = newDate;
            }
            if (k == 1) {
                Map<String, Integer> counter = entry.getValue();
                if (j < sortedDayMap.size()) {
                    j++;
                }
                lastDay = entry.getKey();
                allDayMap.put(lastDay, counter);
            }

        }
        return new TreeMap<>(allDayMap);
    }

    /**
     * Gets the record bean of an entry
     * @param record search result entry
     * @return returns RecordBean object
     */
    private RecordBean createRecordBean(Record record) {
        RecordBean recordBean = new RecordBean();
        recordBean.setId(record.getId());
        recordBean.setTableName(record.getTableName());
        recordBean.setTimestamp(record.getTimestamp());
        recordBean.setValues(record.getValues());
        return recordBean;
    }

    /**
     * Invokes functions group search entries as in @param method
     * @param allDayMap sorted and grouped map in day basis
     * @param method way of grouping
     * @return returns a map which grouped as in @param method
     */
    private Map<String, Map<String, Integer>> getGrouping(Map<Long, Map<String, Integer>> allDayMap, String method) {
        Map<String, Map<String, Integer>> grouped = new HashMap<>();
        switch (method) {
            case auto:
                grouped = groupbyAuto(allDayMap);
                break;
            case day:
                grouped = groupbyDay(allDayMap);
                break;
            case week:
                grouped = groupbyWeek(allDayMap);
                break;
            case month:
                grouped = groupbyMonth(allDayMap);
                break;
            case year:
                grouped = groupbyYear(allDayMap);
                break;
        }
        return grouped;
    }

    /**
     * Groups the search entries in "Auto" method
     * @param allDayMap sorted and grouped map in day basis
     * @return returns grouped map
     */
    private Map<String, Map<String, Integer>> groupbyAuto(Map<Long, Map<String, Integer>> allDayMap) {
        Map<String, Map<String, Integer>> grouped = new HashMap<>();
        int days = allDayMap.size();
        if (days <= 10) {
            grouped = groupbyDay(allDayMap);
        } else if (days > 10 && days <= 70) {
            grouped = groupbyWeek(allDayMap);
        } else if (days > 70 && days <= 365 * 3) {
            grouped = groupbyMonth(allDayMap);
        } else {
            grouped = groupbyYear(allDayMap);
        }
        return grouped;
    }

    /**
     * Groups search result entries in Day basis
     * @param allDayMap sorted and grouped map in day basis
     * @return returns a grouped map with string key value
     */
    private Map<String, Map<String, Integer>> groupbyDay(Map<Long, Map<String, Integer>> allDayMap) {
        Map<String, Map<String, Integer>> grouped = new HashMap<>();
        String pattern = LAConstants.DAY_PATTERN;
        SimpleDateFormat format = new SimpleDateFormat(pattern);

        for (Map.Entry<Long, Map<String, Integer>> entry : allDayMap.entrySet()) {
            Date expiry = new Date(entry.getKey());
            String str_day = format.format(expiry);
            grouped.put(str_day, entry.getValue());
        }
        return grouped;
    }

    /**
     * Groups search result entries in week basis
     * @param allDayMap sorted and grouped map in day basis
     * @return returns a grouped map with string key value
     */
    private Map<String, Map<String, Integer>> groupbyWeek(Map<Long, Map<String, Integer>> allDayMap) {
        String pattern = LAConstants.WEEK_PATTERN;
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        Map<String, List<Map<String, Integer>>> grouped = new HashMap<>();
        for (Map.Entry<Long, Map<String, Integer>> entry : allDayMap.entrySet()) {
            Date expiry = new Date(entry.getKey());
            String str_week = format.format(expiry);
            if (!grouped.containsKey(str_week)) {
                List<Map<String, Integer>> list = new ArrayList<>();
                list.add(entry.getValue());
                grouped.put(str_week, list);
            } else {
                List<Map<String, Integer>> list = grouped.get(str_week);
                list.add(entry.getValue());
                grouped.put(str_week, list);
            }
        }
        Map<String, Map<String, Integer>> Fgrouped = new HashMap<>();
        for (Map.Entry<String, List<Map<String, Integer>>> entry : grouped.entrySet()) {
            List<Map<String, Integer>> list = entry.getValue();
            Map<String, Integer> data = new HashMap<>();
            for (Map<String, Integer> dataMap : list) {
                for (Map.Entry<String, Integer> entrr : dataMap.entrySet()) {
                    if (!data.containsKey(entrr.getKey())) {
                        data.put(entrr.getKey(), entrr.getValue());
                    } else {
                        int k = data.get(entrr.getKey());
                        data.put(entrr.getKey(), k + entrr.getValue());
                    }
                }
            }
            Fgrouped.put(entry.getKey(), data);
        }
        return Fgrouped;
    }

    /**
     * Groups search result entries in month basis
     * @param allDayMap sorted and grouped map in day basis
     * @return returns a grouped map with string key value
     */
    private Map<String, Map<String, Integer>> groupbyMonth(Map<Long, Map<String, Integer>> allDayMap) {

        String pattern = LAConstants.MONTH_PATTERN;
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        Map<String, List<Map<String, Integer>>> grouped = new HashMap<>();
        for (Map.Entry<Long, Map<String, Integer>> entry : allDayMap.entrySet()) {
            Date expiry = new Date(entry.getKey());
            String str_month = format.format(expiry);
            if (!grouped.containsKey(str_month)) {
                List<Map<String, Integer>> list = new ArrayList<>();
                list.add(entry.getValue());
                grouped.put(str_month, list);
            } else {
                List<Map<String, Integer>> list = grouped.get(str_month);
                list.add(entry.getValue());
                grouped.put(str_month, list);
            }
        }
        Map<String, Map<String, Integer>> Fgrouped = new HashMap<>();
        for (Map.Entry<String, List<Map<String, Integer>>> entry : grouped.entrySet()) {
            List<Map<String, Integer>> list = entry.getValue();
            Map<String, Integer> data = new HashMap<>();
            for (Map<String, Integer> dataMap : list) {
                for (Map.Entry<String, Integer> entrr : dataMap.entrySet()) {
                    if (!data.containsKey(entrr.getKey())) {
                        data.put(entrr.getKey(), entrr.getValue());
                    } else {
                        int k = data.get(entrr.getKey());
                        data.put(entrr.getKey(), k + entrr.getValue());
                    }
                }
            }
            Fgrouped.put(entry.getKey(), data);
        }
        return Fgrouped;
    }

    /**
     * Groups search result entries in year basis
     * @param allDayMap sorted and grouped map in day basis
     * @return returns a grouped map with string key value
     */
    private Map<String, Map<String, Integer>> groupbyYear(Map<Long, Map<String, Integer>> allDayMap) {
        String pattern = LAConstants.YEAR_PATTERN;
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        Map<String, List<Map<String, Integer>>> grouped = new HashMap<>();
        for (Map.Entry<Long, Map<String, Integer>> entry : allDayMap.entrySet()) {
            Date expiry = new Date(entry.getKey());
            String str_year = format.format(expiry);
            if (!grouped.containsKey(str_year)) {
                List<Map<String, Integer>> list = new ArrayList<>();
                list.add(entry.getValue());
                grouped.put(str_year, list);
            } else {
                List<Map<String, Integer>> list = grouped.get(str_year);
                list.add(entry.getValue());
                grouped.put(str_year, list);
            }
        }
        Map<String, Map<String, Integer>> Fgrouped = new HashMap<>();
        for (Map.Entry<String, List<Map<String, Integer>>> entry : grouped.entrySet()) {
            List<Map<String, Integer>> list = entry.getValue();
            Map<String, Integer> data = new HashMap<>();
            for (Map<String, Integer> dataMap : list) {
                for (Map.Entry<String, Integer> entrr : dataMap.entrySet()) {
                    if (!data.containsKey(entrr.getKey())) {
                        data.put(entrr.getKey(), entrr.getValue());
                    } else {
                        int k = data.get(entrr.getKey());
                        data.put(entrr.getKey(), k + entrr.getValue());
                    }
                }
            }
            Fgrouped.put(entry.getKey(), data);
        }
        return Fgrouped;
    }
}
