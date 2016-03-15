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
var client = new AnalyticsClient().init();
var div1 = "#table";
var div2 = "#chart";
var table, chart;
var from = 1460341665000;
var to = 1460484000000;
var async_tasks = gadgetConfig.level.length;
var dataM = [];
var isRedraw =false;
//var height = document.body.clientHeight;
//var width = document.body.clientWidth;


//$( window ).resize(function() {
//    console.log(height);
//    console.log(width);
//    height = document.body.clientHeight;
//    width = document.body.clientWidth;
//    configChart.width=width;
//    chart.draw();
//});

var meta = {
    "names": ["LogLevel", "Frequency"],
    "types": ["ordinal", "linear"]
};

var configTable = {
    key: "LogLevel",
    charts: [{
        type: "table",
        y: "Frequency",
        color: "*",
        columns: ["LogLevel", "Frequency"],
        columnTitles: ["Log Level", "Frequency"]
    }
    ],
    width: 400,
    height: 200
};

var configChart = {
    type: "bar",
    x: "LogLevel",
    charts: [{y: "Frequency"}],
    width: 400,
    height: 200
};


function initialize() {
    fetch();
}

$(document).ready(function () {
    initialize();
});

function fetch(ch) {
    if (!ch) {
        dataM.length = 0;
        ch = 0;
    }
    var queryInfo = {
        tableName: "LOGANALYZER",
        searchParams: {
            query: "_level:" + gadgetConfig.level[ch] + " AND  _eventTimeStamp: [" + from + " TO " + to + "]"
        }
    };

    client.searchCount(queryInfo, function (d) {
        if (d["status"] === "success") {
            dataM.push([gadgetConfig.level[ch], parseInt(d["message"])]);
            async_tasks--;
            if (async_tasks == 0) {
                if(isRedraw){
                    isRedraw = false;
                    redrawChart();
                }else{
                    drawAnalyticsChart();
                }
            } else {
                fetch(++ch);
            }
        }
    }, function (error) {
        console.log("error occured: " + error);
    });
    console.log("ch value: " + ch);
}

function drawAnalyticsChart() {
    table = new vizg(
        [
            {
                "metadata": this.meta,
                "data": dataM
            }
        ],
        configTable
    );
    chart = new vizg(
        [
            {
                "metadata": this.meta,
                "data": dataM
            }
        ],
        configChart
    );
    table.draw(div1);
    chart.draw(div2);
}

function redrawChart(){
    for(var i in dataM){
        table.insert([dataM[i]]);
        chart.insert([dataM[i]]);
    }
}


function subscribe(callback) {
    gadgets.HubSettings.onConnect = function () {
        gadgets.Hub.subscribe("subscriber", function (topic, data, subscriber) {
            callback(topic, data, subscriber)
        });
    };
}

subscribe(function (topic, data, subscriber) {
    //console.log("---subscribe----"+subscriber);
    //console.log("topic: " + topic);
    //console.log("data: " + JSON.stringify(data));
    console.log("From Time : "+parseInt(data["timeFrom"]));
    console.log("To Time : "+parseInt(data["timeTo"]));
    from = parseInt(data["timeFrom"]);
    to = parseInt(data["timeTo"]);
    async_tasks = gadgetConfig.level.length;
    isRedraw =true;
    fetch();
});