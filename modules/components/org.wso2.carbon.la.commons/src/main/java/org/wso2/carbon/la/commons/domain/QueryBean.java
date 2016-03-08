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
package org.wso2.carbon.la.commons.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;


public class QueryBean {
    /**
     * The table name.
     */
    private String tableName;

    private String query;

    private int start;

    private long timeFrom;

    private long timeTo;

    private String facetPath;

    private String str_timeFrom;
    private String str_timeTo;

    public String getStr_timeFrom(){ return str_timeFrom; }

    public void setStr_timeFrom(String str_timeFrom) {
        this.str_timeFrom = str_timeFrom;
    }

    public void setStr_timeTo(String str_timeTo){
        this.str_timeTo = str_timeTo;
    }

    public String getStr_timeTo(){ return str_timeTo; }

    public int getLength() {
        return length;
    }

    public String getFacetPath(){return facetPath;}

    public void setFacetPath(String facetPath){this.facetPath=facetPath;}

    public void setLength(int length) {
        this.length = length;
    }

    public int getDraw() {
        return draw;
    }

    public void setDraw(int draw) {
        this.draw = draw;
    }

    public Object getColumns() {
        return columns;
    }

    public void setColumns(Object columns) {
        this.columns = columns;
    }

    public Object getOrder() {
        return order;
    }

    public void setOrder(Object order) {
        this.order = order;
    }

    public Object getSearch() {
        return search;
    }

    public void setSearch(Object search) {
        this.search = search;
    }

    private int length;

    private int draw;

    private Object columns;

    private Object order;

    private Object search;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public long getTimeFrom() {
        return timeFrom;
    }

    public void setTimeFrom(long timeFrom) {
        this.timeFrom = timeFrom;
    }

    public long getTimeTo() {
        return timeTo;
    }

    public void setTimeTo(long timeTo) {
        this.timeTo = timeTo;
    }
}
