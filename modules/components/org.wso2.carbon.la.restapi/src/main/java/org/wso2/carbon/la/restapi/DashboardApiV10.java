package org.wso2.carbon.la.restapi;

/**
 * Created by vithulan on 2/2/16.
 */

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.apache.http.HttpHeaders;
import org.wso2.carbon.analytics.api.AnalyticsDataAPI;
import org.wso2.carbon.analytics.dataservice.commons.*;
import org.wso2.carbon.analytics.dataservice.core.AnalyticsDataServiceUtils;
import org.wso2.carbon.analytics.datasource.commons.AnalyticsSchema;
import org.wso2.carbon.analytics.datasource.commons.ColumnDefinition;
import org.wso2.carbon.analytics.datasource.commons.Record;
import org.wso2.carbon.analytics.datasource.commons.RecordGroup;
import org.wso2.carbon.analytics.datasource.commons.exception.AnalyticsException;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.la.commons.constants.LAConstants;
import org.wso2.carbon.la.commons.domain.QueryBean;
import org.wso2.carbon.la.commons.domain.RecordBean;
import org.wso2.carbon.la.commons.domain.config.LogFileConf;
import org.wso2.carbon.la.core.impl.DrilldownController;
import org.wso2.carbon.la.core.impl.LogFileProcessor;
import org.wso2.carbon.la.core.impl.TimeAggregator;
import org.wso2.carbon.la.core.utils.LACoreServiceValueHolder;
import org.wso2.carbon.la.restapi.beans.LAErrorBean;
import org.wso2.carbon.la.restapi.util.Util;
import org.wso2.carbon.utils.CarbonUtils;

import javax.activation.DataHandler;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


@Path("/dashboard")
public class DashboardApiV10 {

    private static final Log log = LogFactory.getLog(DashboardApiV10.class);

    /**
     * GET method
     * @return Returns all the column names in DAS, LOGANALYZER Table
     * @throws AnalyticsException
     */
    @GET
    @Path("getFields")
    @Produces("application/json")
    @Consumes("application/json")
    public Response getFields() throws AnalyticsException {
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        int tenantId = carbonContext.getTenantId();
        Map<String, ColumnDefinition> columns;
        List<String> fields = new ArrayList<String>();
        AnalyticsDataAPI analyticsDataAPI;
        AnalyticsSchema analyticsSchema;

        analyticsDataAPI = LACoreServiceValueHolder.getInstance().getAnalyticsDataAPI();
        analyticsSchema = analyticsDataAPI.getTableSchema(tenantId, LAConstants.LOG_ANALYZER_STREAM_NAME.toUpperCase());
        columns = analyticsSchema.getColumns();
        for (ColumnDefinition alpha : columns.values()) {
            fields.add(alpha.getName());
        }
        return Response.ok(fields).build();
    }

    /**
     * POST method
     * @param query QueryBean object from front end
     * @return Returns all the elements in selected column with number of Hits
     */
    @POST
    @Path("/fieldData")
    @Produces("application/json")
    @Consumes("application/json")
    public StreamingOutput fieldData(final QueryBean query) {

        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        int tenantId = carbonContext.getTenantId();
        List<String> column = new ArrayList<String>();
        column.add(query.getQuery());
        AnalyticsDataAPI analyticsDataAPI;
        AnalyticsDataResponse analyticsDataResponse;
        analyticsDataAPI = LACoreServiceValueHolder.getInstance().getAnalyticsDataAPI();
        try {
            analyticsDataResponse = analyticsDataAPI.get(tenantId, LAConstants.LOG_ANALYZER_STREAM_NAME.toUpperCase(), 1,
                    column, query.getTimeFrom(), query.getTimeTo(), query.getStart(), -1);
            List<Iterator<Record>> iterators = Util.getRecordIterators(analyticsDataResponse, analyticsDataAPI);
            final Map<String, Integer> counter = Util.getCountGroup(iterators, query.getQuery());
            return new StreamingOutput() {
                @Override
                public void write(OutputStream outputStream)
                        throws IOException, WebApplicationException {
                    Writer recordWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
                    recordWriter.write("[");
                    int i = 1;
                    for (Map.Entry<String, Integer> entry : counter.entrySet()) {

                        recordWriter.write("[[\"" + entry.getKey() + "\"],[\"" + entry.getValue() + "\"]]");

                        if (i < counter.size()) {
                            recordWriter.write(",");
                            i++;
                        }
                    }
                    recordWriter.write("]");
                    recordWriter.flush();
                }
            };

        } catch (AnalyticsException e) {
            String msg = String.format("Error occurred while retrieving field data");
            log.error(msg, e);
            return new StreamingOutput() {
                @Override
                public void write(OutputStream outputStream) throws IOException, WebApplicationException {
                    Writer recordWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
                    recordWriter.write("Error in reading records");
                    recordWriter.flush();
                }
            };
        }
    }

    /**
     * POST method
     * @param query QueryBean object from front end
     * @return Returns filtered data from a specified column
     */
    @POST
    @Path("/filterData")
    @Produces("application/json")
    @Consumes("application/json")
    public StreamingOutput filterData(final QueryBean query) {
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        String username = carbonContext.getUsername();
        DrilldownController drilldownController = new DrilldownController();
        if (query != null) {
            String q[] = query.getQuery().split(",,");
            String searchQuery = q[0];
            searchQuery = Util.appendTimeStamp(searchQuery, query.getStr_timeFrom(), query.getStr_timeTo());
            final String col = q[1];
            List<String> column = new ArrayList<>();
            column.add(col);

            String facetPath = query.getFacetPath();
            Map<String, List<String>> categoryPath = new HashMap<>();
            List<String> pathList = new ArrayList<>();
            if (facetPath.equals("None")) {
                categoryPath.put(LAConstants.LOGSTREAM, pathList);
            } else {
                String pathArray[] = facetPath.split(",");
                for (String path : pathArray) {
                    pathList.add(path);
                }
                categoryPath.put(LAConstants.LOGSTREAM, pathList);
            }
            List<Iterator<Record>> iterators = drilldownController.getResults(query.getTableName(), searchQuery,
                    query.getLength(), query.getStart(), categoryPath, username, column);
            final Map<String, Integer> counter = Util.getCountGroup(iterators, col);
            return new StreamingOutput() {
                @Override
                public void write(OutputStream outputStream)
                        throws IOException, WebApplicationException {
                    Writer recordWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
                    recordWriter.write("[");
                    int i = 1;
                    for (Map.Entry<String, Integer> entry : counter.entrySet()) {
                        recordWriter.write("[[\"" + entry.getKey() + "\"],[\"" + entry.getValue() + "\"]]");
                        if (i < counter.size()) {
                            recordWriter.write(",");
                            i++;
                        }
                    }
                    recordWriter.write("]");
                    recordWriter.flush();
                }
            };
        } else {
            String msg = String.format("Query cannot be NULL");
            log.error(msg);
            return new StreamingOutput() {
                @Override
                public void write(OutputStream outputStream) throws IOException, WebApplicationException {
                    Writer recordWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
                    recordWriter.write("Query is null");
                    recordWriter.flush();
                }
            };
        }
    }

    /**
     * POST method
     * @param query QueryBean object from front end
     * @return Returns data in specific column by grouping it by time
     */
    @POST
    @Path("/epochTimeDataFinal")
    @Produces("application/json")
    @Consumes("application/json")
    public StreamingOutput EpochtimeDataFinal(final QueryBean query) {

        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        String username = carbonContext.getUsername();
        DrilldownController drilldownController = new DrilldownController();
        TimeAggregator timeAggregator = new TimeAggregator();
        if (query != null) {
            String q[] = query.getQuery().split(",,");
            String searchQuery = q[0];
            searchQuery = Util.appendTimeStamp(searchQuery, query.getStr_timeFrom(), query.getStr_timeTo());
            final String columnName = q[1];
            final String groupBy = q[2];
            List<String> column = new ArrayList<>();
            column.add(columnName);
            column.add(LAConstants.TIMESTAMP_FIELD);

            String facetPath = query.getFacetPath();
            Map<String, List<String>> categoryPath = new HashMap<>();
            List<String> pathList = new ArrayList<>();
            if (facetPath.equals("None")) {
                categoryPath.put(LAConstants.LOGSTREAM, pathList);
            } else {
                String pathArray[] = facetPath.split(",");
                for (String path : pathArray) {
                    pathList.add(path);
                }
                categoryPath.put(LAConstants.LOGSTREAM, pathList);
            }

            final List<Iterator<Record>> iterators = drilldownController.getResults(query.getTableName(), searchQuery,
                    query.getLength(), query.getStart(), categoryPath, username, column);
            final Map<String, Map<String, Integer>> aggregatedMap = timeAggregator.getGrouped(iterators, columnName, groupBy);
            return new StreamingOutput() {
                @Override
                public void write(OutputStream outputStream)
                        throws IOException, WebApplicationException {
                    Writer recordWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
                    int i = 1;
                    int l = 1;
                    recordWriter.write("[");
                    for (Map.Entry<String, Map<String, Integer>> entry : aggregatedMap.entrySet()) {
                        Map<String, Integer> counter = entry.getValue();
                        for (Map.Entry<String, Integer> infom : counter.entrySet()) {
                            recordWriter.write("[[\"" + entry.getKey() + "\"],[\"" + infom.getKey()
                                    + "\"],[\"" + infom.getValue() + "\"]]");
                            if (i < counter.size()) {
                                recordWriter.write(",");
                                i++;
                            }
                        }
                        i = 1;
                        if (l < aggregatedMap.size()) {
                            recordWriter.write(",");
                            l++;
                        }
                    }
                    recordWriter.write("]");
                    recordWriter.flush();

                }
            };
        } else {
            String msg = String.format("Query cannot be NULL");
            log.error(msg);
            return new StreamingOutput() {
                @Override
                public void write(OutputStream outputStream) throws IOException, WebApplicationException {
                    Writer recordWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
                    recordWriter.write("Query is null");
                    recordWriter.flush();
                }
            };
        }
    }

    /**
     * POST method
     * @param query QueryBean object from front end
     * @return returns the facet child of given facet parent.
     */
    @POST
    @Path("/logStreamData")
    @Produces("application/json")
    @Consumes("application/json")
    public StreamingOutput logStreamData(final QueryBean query) {

        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        int tenantId = carbonContext.getTenantId();
        AnalyticsDataAPI analyticsDataAPI;
        String q[] = query.getQuery().split(",,");
        String path[] = null;
        if (!q[1].equals(" ")) {
            String pathName = q[1];
            path = pathName.split(",");
        }
        String fieldName = q[0];

        CategoryDrillDownRequest categoryDrillDownRequest = new CategoryDrillDownRequest();
        categoryDrillDownRequest.setTableName(query.getTableName());
        categoryDrillDownRequest.setFieldName(fieldName);
        categoryDrillDownRequest.setPath(path);
        final List<CategorySearchResultEntry> list;
        analyticsDataAPI = LACoreServiceValueHolder.getInstance().getAnalyticsDataAPI();
        try {
            SubCategories subCategories = analyticsDataAPI.drillDownCategories(tenantId, categoryDrillDownRequest);
            list = subCategories.getCategories();

            return new StreamingOutput() {
                @Override
                public void write(OutputStream outputStream)
                        throws IOException, WebApplicationException {
                    Writer recordWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
                    recordWriter.write("[");
                    int i = 1;
                    for (CategorySearchResultEntry lister : list) {
                        recordWriter.write("\"" + lister.getCategoryValue() + "\"");
                        if (i < list.size()) {
                            recordWriter.write(",");
                            i++;
                        }
                    }
                    recordWriter.write("]");
                    recordWriter.flush();
                }
            };

        } catch (AnalyticsException e) {
            String msg = String.format("Error occurred while drilldowning facet data");
            log.error(msg, e);
            return new StreamingOutput() {
                @Override
                public void write(OutputStream outputStream) throws IOException, WebApplicationException {
                    Writer recordWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
                    recordWriter.write("Error in reading records");
                    recordWriter.flush();
                }
            };
        }

    }
}


