/*
 * Copyright (c)  2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
var bc = bc || {};
bc.chart = null;
bc.polling_task = null;
bc.data = [];
bc.filter_key = null;
bc.filters_meta = {};
bc.filters = [];
bc.filter_prefix = "g_";
bc.selected_filters = [];
bc.force_fetch = false;
bc.freeze = false;
bc.div = "#chart";
bc.meta = {
    "names": ["id", "name", "count"],
    "types": ["ordinal", "ordinal", "linear"]
};
bc.config = {
    type: "bar",
    x: "id",
    charts: [
        {
            type: "bar",
            y: "count",
            color: "name",
            xTitle: "",
            yTitle: "",
            mode: "group"
        }
    ],
    width: 500,
    height: 250
};

bc.initialize = function () {
    bc.chart = new vizg(
        [
            {
                "metadata": bc.meta,
                "data": bc.data
            }
        ],
        bc.config
    );
    bc.chart.draw("#chart", [
        {
            type: "click",
            callback: bc.onclick
        }
    ]);
    bc.loadFiltersFromURL();
    bc.startPolling();
};

bc.loadFiltersFromURL = function() {
    bc.filter_key = $.ajax({
        url: gadgetConfig.source,
        method: "POST",
        data: JSON.stringify({}),
        async: false
    }).responseJSON.filteredBy;
    var params = getURLParams();
    for (var filter in params) {
        if (params.hasOwnProperty(filter)
            && filter.lastIndexOf(bc.filter_prefix, 0) === 0) {
            var filter_key = filter.substring(bc.filter_prefix.length);
            if (bc.filter_key === filter_key) {
                bc.selected_filters = params[filter];
            } else {
                bc.updateFilters({
                    filter: filter_key,
                    selections: params[filter]
                });
            }
        }
    }
    // TODO : update selected bars (UI)
};

bc.startPolling = function () {
    setTimeout(function () {
        bc.update();
    }, 500);
    this.polling_task = setInterval(function () {
        bc.update();
    }, gadgetConfig.polling_interval);
};

bc.update = function (force) {
    bc.force_fetch = !bc.force_fetch ? force || false : true;
    if (!bc.freeze) {
        bc.fetch(function (data) {
            bc.chart.insert(data);
        });
    }
};

bc.fetch = function (cb) {
    bc.data.length = 0;
    bc.force_fetch = false;
    $.ajax({
        url: gadgetConfig.source,
        method: "POST",
        dataType: 'json',
        contentType: 'application/json',
        data: JSON.stringify(bc.filters),
        success: function (response) {
            bc.filter_key = response["filteredBy"];
            var data = response["data"];
            if (data && data.length > 0) {
                for (var i = 0; i < data.length; i++) {
                    bc.data.push(
                        [data[i]["id"], data[i]["label"], data[i]["count"]]
                    );
                }
                if (bc.force_fetch) {
                    bc.update();
                } else {
                    cb(bc.data);
                }
            }
        },
        complete: function (jqXHR, status) {
            if (status !== "success") console.warn("Error accessing source for : " + gadgetConfig.id);
        }
    });
};

bc.updateURL = function () {
    updateURLParam(bc.filter_prefix + bc.filter_key, bc.selected_filters);
};

bc.subscribe = function (callback) {
    gadgets.HubSettings.onConnect = function () {
        gadgets.Hub.subscribe("subscriber", function (topic, data, subscriber) {
            callback(topic, data)
        });
    };
};

bc.publish = function (data) {
    gadgets.Hub.publish("publisher", data);
};

bc.onclick = function (event, item) {
    if (item != null) {
        var filter = item.datum[bc.config.x];
        var index = bc.selected_filters.indexOf(filter);
        if (index !== -1) {
            bc.selected_filters.splice(index, 1);
        } else {
            bc.selected_filters.push(filter);
        }
        bc.publish({
            "filter": bc.filter_key,
            "selections": bc.selected_filters
        });
        bc.freeze = bc.selected_filters.length > 0;
        bc.updateURL();
        bc.update(true);
    }
};

bc.updateFilters = function (data) {
    var updated = false;
    if (typeof data != "undefined" && data != null) {
        if (typeof data.selections === "undefined"
            || data.selections === null
            || Object.prototype.toString.call(data.selections) !== '[object Array]'
            || data.selections.length === 0) {
            if (bc.filters_meta.hasOwnProperty(data.filter)) {
                delete bc.filters_meta[data.filter];
                updated = true;
            }
        } else {
            if (typeof data.filter != "undefined"
                && data.filter != null
                && typeof data.selections != "undefined"
                && data.selections != null
                && Object.prototype.toString.call(data.selections) === '[object Array]'
                && data.selections.length > 0) {
                bc.filters_meta[data.filter] = data;
                updated = true;
            }
        }
    }
    if (updated) {
        bc.filters.length = 0;
        for (var i in bc.filters_meta) {
            if (bc.filters_meta.hasOwnProperty(i)) {
                bc.filters.push(bc.filters_meta[i]);
            }
        }
        bc.update(true);
    }
};

bc.subscribe(function (topic, data) {
    bc.updateFilters(data);
});

$(document).ready(function () {
    bc.initialize();
});