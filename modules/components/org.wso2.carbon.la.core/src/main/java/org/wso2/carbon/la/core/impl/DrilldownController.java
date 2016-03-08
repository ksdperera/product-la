package org.wso2.carbon.la.core.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.analytics.api.AnalyticsDataAPI;
import org.wso2.carbon.analytics.dataservice.commons.AnalyticsDataResponse;
import org.wso2.carbon.analytics.dataservice.commons.AnalyticsDrillDownRequest;
import org.wso2.carbon.analytics.dataservice.commons.SearchResultEntry;
import org.wso2.carbon.analytics.dataservice.commons.exception.AnalyticsIndexException;
import org.wso2.carbon.analytics.datasource.commons.Record;
import org.wso2.carbon.analytics.datasource.commons.RecordGroup;
import org.wso2.carbon.analytics.datasource.commons.exception.AnalyticsException;
import org.wso2.carbon.la.core.utils.LACoreServiceValueHolder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by vithulan on 3/4/16.
 */
public class DrilldownController {
    private static final Log log = LogFactory.getLog(DrilldownController.class);
    private AnalyticsDrillDownRequest analyticsDrillDownRequest;
    private AnalyticsDataAPI analyticsDataAPI;
    private AnalyticsDataResponse analyticsDataResponse;
    public DrilldownController(){
        analyticsDrillDownRequest = new AnalyticsDrillDownRequest();
        analyticsDataAPI = LACoreServiceValueHolder.getInstance().getAnalyticsDataAPI();
    }

    /**
     * @param tableName Table name of LogAnalyzer
     * @param searchQuery Search query appended with timestamp
     * @param length number of elements to be retrieved
     * @param start starting point
     * @param categoryPath facet path of logstream
     * @param username username of user
     * @param columnList List of columns that data has to be retrieved from
     * @return list of iterators of search result.
     */
    public List<Iterator<Record>> getResults(String tableName, String searchQuery, int length, int start,
                                             Map<String, List<String>> categoryPath, String username, List<String> columnList){
        List<SearchResultEntry> searchResultEntries = null;
        try {
            searchResultEntries = drillDownSearch(tableName,searchQuery,length,start,categoryPath,username);
        } catch (AnalyticsIndexException e) {
            String msg = String.format("Error occurred while drilldown searching");
            log.error(msg,e);
        }
        List<String> ids = getRecordIds(searchResultEntries);
        try {
            analyticsDataResponse = analyticsDataAPI.get(username, tableName, 1, columnList, ids);
        } catch (AnalyticsException e) {
            String msg = String.format("Error occurred while filtering using column list");
            log.error(msg,e);
        }
        List<Iterator<Record>> iterators = new ArrayList<>();
        for (RecordGroup recordGroup : analyticsDataResponse.getRecordGroups()) {
            try {
                iterators.add(analyticsDataAPI.readRecords(analyticsDataResponse.getRecordStoreName(), recordGroup));
            } catch (AnalyticsException e) {
                String msg = String.format("Error occurred while getting record lists");
                log.error(msg,e);
            }
        }

        return iterators;
    }

    /**
     *
     * @param tableName Table name of LogAnalyzer
     * @param searchQuery Search query with timestamp appended
     * @param length    number of entries to be returned
     * @param start     starting entry
     * @param categoryPath  facet path
     * @param username  username of user
     * @return  List of ids of search result entry
     * @throws AnalyticsIndexException
     */
    private List<SearchResultEntry> drillDownSearch(String tableName, String searchQuery, int length, int start,
                                                    Map<String, List<String>> categoryPath,
                                                    String username) throws AnalyticsIndexException {
        analyticsDrillDownRequest.setTableName(tableName);
        analyticsDrillDownRequest.setQuery(searchQuery);
        analyticsDrillDownRequest.setRecordCount(length);
        analyticsDrillDownRequest.setRecordStartIndex(start);
        analyticsDrillDownRequest.setCategoryPaths(categoryPath);
        return analyticsDataAPI.drillDownSearch(username, analyticsDrillDownRequest);

    }

    private List<String> getRecordIds(List<SearchResultEntry> searchResultEntries){
        List<String> ids = new ArrayList<>();
        for (SearchResultEntry searchResult : searchResultEntries) {
            ids.add(searchResult.getId());
        }
        return ids;
    }
}
