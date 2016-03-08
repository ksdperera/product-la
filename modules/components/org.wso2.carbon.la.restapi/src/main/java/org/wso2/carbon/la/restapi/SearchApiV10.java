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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.la.restapi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.analytics.datasource.commons.exception.AnalyticsException;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.la.commons.constants.LAConstants;
import org.wso2.carbon.la.commons.domain.QueryBean;
import org.wso2.carbon.la.commons.domain.RecordBean;
import org.wso2.carbon.la.commons.domain.ResponseBean;
import org.wso2.carbon.la.core.impl.SearchController;
import org.wso2.carbon.la.restapi.beans.LAErrorBean;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/search")
public class SearchApiV10 extends LARestApi{

    private static final Log log = LogFactory.getLog(SearchApiV10.class);

    private SearchController searchController;

    SearchApiV10(){
        searchController = new SearchController();
    }

    /**
     * Search records.
     * @param queryBean the query bean
     * @return the response
     * @throws AnalyticsException
     */
    @POST
    @Consumes({ MediaType.APPLICATION_JSON})
    @Produces({ MediaType.APPLICATION_JSON })
    @Path("/")
    public Response search(QueryBean queryBean) {
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        String username = carbonContext.getUsername();
        queryBean.setTableName(LAConstants.LOG_ANALYZER_STREAM_NAME);
        try {
            List<RecordBean> recordBeans = searchController.search(queryBean, username);
            ResponseBean responseBean = new ResponseBean();
            responseBean.setDraw(queryBean.getDraw());
            responseBean.setRecordsTotal(searchController.getRecordCount(username, queryBean));
            responseBean.setRecordsFiltered(searchController.getRecordCount(username, queryBean));
            responseBean.setData(recordBeans);
            return Response.ok(responseBean).build();
        } catch (AnalyticsException e) {
            String msg = String.format( "Error occurred while searching");
            log.error(msg, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new LAErrorBean(e.getMessage()))
                    .build();
        }
    }
}
