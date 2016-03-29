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
var dataM = [];

var meta = {
    "names": ["Server Id", "Server up time (s)", "Server up time (m)","Server up time (h)"],
    "types": ["ordinal", "linear"]
};


var configChart = {
    title: "Server up time distribution",
    type: "bar",
    x: "Server Id",
    colorScale:["#3498db"],
    charts: [{ y: "Server up time (s)"}],
    width: $('body').width(),
    height: $('body').height(),
    padding: { "top": 40, "left": 80, "bottom": 70, "right": 100 },
     tooltip: {"enabled":true, "color":"#e5f2ff", "type":"symbol", "content":["Server Id","Server up time (s)","Server up time (m)", "Server up time (h)"], "label":true}

};


//chart.chartConfig.width = ($('#canvas').width() - 20);
//chart.chartConfig.height = 200;

function initialize() {
    fetch();
}

$(document).ready(function () {
    initialize();
});




function fetch(ch) {
    if (!ch) {
        ch = 0;
    }

    var queryInfo = {
        tableName: "LOGANALYZER_SERVERUPTIME",
        searchParams: {
            query : '(*:*)',
            start : 0, //starting index of the matching record set
            count : 100000 //page size for pagination
        }
    };
    client.search(queryInfo, function (d) {
        if (d["status"] === "success") {
        var temObj =d["message"];
        var startTime="";
        var endTime="";
        var ip;
        var total=0;
        var count=0;

        var obj = $.parseJSON(temObj );


            for (var i = 0; i < obj.length; i++) {
                    dataM.push([obj[i].values.AgentId, Math.round(obj[i].values.AverageServerUpTime/1000 * 100) / 100,Math.round(obj[i].values.AverageServerUpTime/60000 * 100) / 100,Math.round(obj[i].values.AverageServerUpTime/3600000 * 100) / 100]);

}

        drawAnalyticsChart();
        }
    }, function (error) {
        console.log("error occured: " + error);
    });
}





/*function fetch2(ch) {
    if (!ch) {
        ch = 0;
    }

    var queryInfo = {
        tableName: "LOGANALYZER",
        searchParams: {
            query : '(_content: "Starting WSO2 Carbon*" OR _content:"Mgt Console URL*")',
            start : 0, //starting index of the matching record set
            count : 100000 //page size for pagination
        }
    };
    client.search(queryInfo, function (d) {
        if (d["status"] === "success") {
        var temObj =d["message"];
        var startTime="";
        var endTime="";
        var ip;
        var total=0;
        var count=0;

        var obj = $.parseJSON(temObj );

        var temp = {};
        obj.sort(function(a, b) {
            return a.values._timeinstance.localeCompare(b.values._timeinstance);
        });
        // Store each of the elements in an object keyed of of the name field.  If there is a collision (the name already exists) then it is just replaced with the most recent one.
        for (var i = 0; i < obj.length; i++) {
            temp[obj[i].values.logstream] = obj[i].values.logstream;
            console.log(obj[i].values.logstream);
        }
        for (var o in temp) {
            for (var i = 0; i < obj.length; i++) {
                 if(obj[i].values.logstream == o){
                     if(obj[i].values._content.indexOf("Starting WSO2 Carbon") > -1){
                            startTime =  obj[i].values._timeinstance;
                     }
                     if(obj[i].values._content.indexOf("Mgt Console URL") > -1){
                            endTime =  obj[i].values._timeinstance;
                     }
                     ip = obj[i].values.logstream;
                     if(startTime!="" && endTime != "" ){
                         var dt  = startTime.split(/\-|\s/)
                         dat = new Date(dt.slice(0,3).reverse().join('/')+' '+dt[3]);
                         var dt2  = endTime.split(/\-|\s/)
                         dat2 = new Date(dt2.slice(0,3).reverse().join('/')+' '+dt2[3]);
                         var ms = (dat2.getTime() - dat.getTime())/1000;
                         total = total + ms;
                         count++;
                         startTime ="";
                         endTime="";
                         console.log(o);
                     }
                 }

            }
            console.log(total);
            console.log(count);
            dataM.push([ip, total/count]);
            total = 0;
            count = 0;
        }




        drawAnalyticsChart();
        }
    }, function (error) {
        console.log("error occured: " + error);
    });
}*/

function drawAnalyticsChart() {

    chart = new vizg(
        [
            {
                "metadata": this.meta,
                "data": dataM
            }
        ],
        configChart
    );
    //table.draw(div1);
    chart.draw(div2);
}
