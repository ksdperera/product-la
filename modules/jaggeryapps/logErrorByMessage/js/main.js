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
var div = "#chartErrorMessage";
var from = new Date(moment().subtract(1, 'year')).getTime();
var to = new Date(moment()).getTime();
var dataM = [];
var names = ["day", "count", "message", "shortMessage", "ID"];
var types = ["ordinal", "linear", "ordinal", "ordinal", "linear"];
var mS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'June', 'July', 'Aug', 'Sept', 'Oct', 'Nov', 'Dec'];
var msgMap = new Map();
var msgCount=0;

function initialize() {
    fetch();
    //$("#chartErrorMessage").html(getDefaultText());
}

function getDefaultText() {
    return '<div class="status-message">'+
        '<div class="message message-info">'+
        '<h4><i class="icon fw fw-info"></i>No content to display</h4>'+
        '<p>Please select a date range to view stats.</p>'+
        '</div>'+
        '</div>';
};

function getEmptyRecordsText() {
    return '<div class="status-message">'+
        '<div class="message message-info">'+
        '<h4><i class="icon fw fw-info"></i>No records found</h4>'+
        '<p>Please select a date range to view stats.</p>'+
        '</div>'+
        '</div>';
}

$(document).ready(function () {
    initialize();
});

function fetch() {
    msgMap.length = 0;
    msgCount = 0;
    dataM.length = 0;
    var queryInfo;
    var timeFrame;
    var newFrom;
    var newTo;
    var tomorrow;
    console.log("sajith");
    var diffDays = daysBetween(new Date(from), new Date(to));
    if(diffDays>30){
        timeFrame = "monthly";
        queryInfo = {
            tableName: "LOGANALYZER_MESSAGE_LEVEL_ERROR_MONTHLY",
            searchParams: {
                query: "_timestamp: [" + from + " TO " + to + "]",
                start : 0, //starting index of the matching record set
                count : 100 //page size for pagination
            }
        };
    }else if (diffDays>7){
        timeFrame = "weekly";
        queryInfo = {
            tableName: "LOGANALYZER_MESSAGE_LEVEL_ERROR_WEEKLY",
            searchParams: {
                query: "_timestamp: [" + from + " TO " + to + "]",
                start : 0, //starting index of the matching record set
                count : 100 //page size for pagination
            }
        };
    }else{
        timeFrame = "daily";
        queryInfo = {
            tableName: "LOGANALYZER_MESSAGE_LEVEL_ERROR_DAILY",
            searchParams: {
                query: "_timestamp: [" + from + " TO " + to + "]",
                start : 0, //starting index of the matching record set
                count : 100 //page size for pagination
            }
        };
    }

    console.log(queryInfo);

    client.search(queryInfo, function (d) {

        newFrom = new Date(from);
        newTo = new Date(to);
        var msgHash;
        var obj = JSON.parse(d["message"]);
        if (d["status"] === "success") {
            tomorrow = new Date(from);
            if(timeFrame==="daily"){
                newFrom.setHours(0);
                newFrom.setMinutes(0);
                newFrom.setSeconds(0);
                newTo.setHours(0);
                newTo.setMinutes(0);
                newTo.setSeconds(0);
                while(!(newFrom.getTime() >= newTo.getTime())){
                    dataM.push([newFrom.toDateString(),0,"No Entry","No Entry",0]);
                    newFrom.setHours(newFrom.getHours()+24);
                }
                for (var i =0; i < obj.length ;i++){
                    msgHash  =hashCode(obj[i].values.message);
                    if(!msgMap.hasOwnProperty(msgHash)){
                        msgCount++;
                        msgMap.set(msgHash,msgCount);
                    }
                    var tempDay = new Date(obj[i].timestamp);
                    dataM.push([tempDay.toDateString(),obj[i].values.classCount,obj[i].values.message,"ID :"+msgMap.get(msgHash)+"  - "+obj[i].values.message.substring(1,60)+"..."],msgMap.get(msgHash));
                }
            }else if(timeFrame === "monthly"){
                newFrom.setDate(1);
                newTo.setDate(1);
                while(!(newFrom.getTime() >= newTo.getTime())){
                    dataM.push([mS[newFrom.getMonth()]+" - "+newFrom.getFullYear(),0,"No Entry","No Entry",0]);
                    newFrom.setMonth(newFrom.getMonth()+1);
                }
                for (var i =0; i < obj.length ;i++){
                    msgHash  =hashCode(obj[i].values.message);
                    if(!msgMap.hasOwnProperty(msgHash)){
                        msgCount++;
                        msgMap.set(msgHash,msgCount);
                    }
                    var tempDay = new Date(obj[i].timestamp);
                    dataM.push([mS[tempDay.getMonth()]+" - "+tempDay.getFullYear(),obj[i].values.classCount,obj[i].values.message,"ID :"+msgMap.get(msgHash)+"  - "+obj[i].values.message.substring(1,60)+"...",msgMap.get(msgHash)]);
                }
            }else if(timeFrame === "weekly"){
                var weekNo =0;
                while(!(newFrom.getTime() > newTo.getTime())){
                    dataM.push(["W"+(++weekNo)+" "+mS[newFrom.getMonth()]+" - "+newFrom.getFullYear(),0,"No Entry","No Entry",0]);
                    newFrom.setHours(newFrom.getHours()+(24*7));
                }
                for (var i =0; i < obj.length ;i++){
                    msgHash  =hashCode(obj[i].values.message);
                    if(!msgMap.hasOwnProperty(msgHash)){
                        msgCount++;
                        msgMap.set(msgHash,msgCount);
                    }
                    var tempDay = new Date(obj[i].timestamp);
                    dataM.push(["W"+obj[i].values.week+" "+mS[tempDay.getMonth()]+" - "+tempDay.getFullYear(),obj[i].values.classCount,obj[i].values.message,"ID :"+msgMap.get(msgHash)+"  - "+obj[i].values.message.substring(1,60)+"...",msgMap.get(msgHash)]);
                }
            }
            drawChartByClass();
        }
    }, function (error) {
        console.log("error occured: " + error);
    });
}

function drawChartByClass() {
    $("#chartErrorMessage").empty();
    $("#tableErrorMessage").empty();
    var configChart = {
        type: "bar",
        x : "day",
        colorScale:["#ecf0f1","#1abc9c", "#3498db", "#9b59b6", "#f1c40f","#e67e22","#e74c3c","#95a5a6","#2c3e50"],
        xAxisAngle: "true",
        color:"shortMessage",
        charts : [{type: "bar",  y : "count", mode:"stack"}],
        width: $('body').width()+100,
        height: $('body').height(),
        padding: { "top": 10, "left": 80, "bottom": 70, "right": 600 },
        tooltip: {"enabled":true, "color":"#e5f2ff", "type":"symbol", "content":["message","count","ID"], "label":true}
    };

    var configTable = {
        key: "ID",
        title:"LogErrorMessage",
        charts: [{
            type: "table",
            columns: ["ID", "message","shortMessage","count","day" ],
            columnTitles: ["Message ID", "Long Message", "Shorted Message", "Count", "Day"]
        }
        ],
        width: $(window).width()* 0.95,
        height: $(window).width() * 0.65 > $(window).height() ? $(window).height() : $(window).width() * 0.65,
        padding: { "top": 100, "left": 30, "bottom": 22, "right": 70 }
    };

    var meta = {
        "names": names,
        "types": types
    };

    if(dataM.length > 9){
        sort2(dataM);
        var mapOther = [];
        var newDataM = dataM.slice(0,9);
        var newDataOther = dataM.slice(10,dataM.length-1);
        for (var i=10;i<dataM.length;i++){
            if(isNaN(mapOther[dataM[i][0]])){
                mapOther[dataM[i][0]] = dataM[i][1];
            }else{
                mapOther[dataM[i][0]] = mapOther[dataM[i][0]] + dataM[i][1];
            }
        }
        for (var key in mapOther) {
            var value = mapOther[key];
            newDataM.push([key,value,"Other","Other","Other"]);
        }
    }

    var chart = new vizg(
        [
            {
                "metadata": meta,
                "data": newDataM
            }
        ],
        configChart
    );

    var table = new vizg(
        [
            {
                "metadata": meta,
                "data": newDataOther
            }
        ],
        configTable
    );

    chart.draw(div,[
        {
            type: "click",
            callback: onclick
        }
    ]);

    //if(newDataOther.length>0){
    //    table.draw("#tableErrorMessage");
    //    //var table2 = $('#LogErrorMessage').DataTable();
    //    //$('#body').css( 'display', 'block' );
    //    //table2.columns.adjust().draw();
    //}
}

function publish (data) {
    gadgets.Hub.publish("publisher", data);
};

 var onclick = function(event, item) {
    if (item != null) {
        console.log(JSON.stringify(item.datum.message));
        publish(
            {
                "filter": gadgetConfig.id,
                "selected": item.datum.message
            }
        );
    }
};

function subscribe(callback) {
    gadgets.HubSettings.onConnect = function () {
        gadgets.Hub.subscribe("subscriber", function (topic, data, subscriber) {
            callback(topic, data, subscriber)
        });
    };
}

subscribe(function (topic, data, subscriber) {
    console.log("From Time : "+parseInt(data["timeFrom"]));
    console.log("To Time : "+parseInt(data["timeTo"]));
    from = parseInt(data["timeFrom"]);
    to = parseInt(data["timeTo"]);
    isRedraw = true;
    fetch();
});

function daysBetween( date1, date2 ) {
    //Get 1 day in milliseconds
    var one_day=1000*60*60*24;

    // Convert both dates to milliseconds
    var date1_ms = date1.getTime();
    var date2_ms = date2.getTime();

    // Calculate the difference in milliseconds
    var difference_ms = Math.abs(date2_ms - date1_ms);

    // Convert back to days and return
    return Math.round(difference_ms/one_day);
}


function hashCode(str){
    var hash = 0;
    if (str.length == 0) return hash;
    for (i = 0; i < str.length; i++) {
        char = str.charCodeAt(i);
        hash = ((hash<<5)-hash)+char;
        hash = hash & hash; // Convert to 32bit integer
    }
    return zeroPad(Math.abs(hash),13);
}

function zeroPad(num, places) {
    var zero = places - num.toString().length + 1;
    return Array(+(zero > 0 && zero)).join("0") + num;
}


function sortByKey(array, key) {
    return array.sort(function(a, b) {
        var x = a[key]; var y = b[key];
        return ((x < y) ? -1 : ((x > y) ? 1 : 0));
    });
}

function sort2(array) {
    var len = array.length;
    if(len < 2) {
        return array;
    }
    var pivot = Math.ceil(len/2);
    return merge(sort2(array.slice(0,pivot)), sort2(array.slice(pivot)));
}

function merge(left, right) {
    var result = [];
    while((left.length > 0) && (right.length > 0)) {
        if(left[1]> right[1]) {
            result.push(left.shift());
        }
        else {
            result.push(right.shift());
        }
    }

    result = result.concat(left, right);
    return result;
}
