var serverUrl = window.location.origin;
var facetPath ="None" ;
function openDashboard() {
    //window.location= baseUrl +'/loganalyzer/site/visualize.jag';
    // retrieveColum();
    window.open(serverUrl + '/loganalyzer/site/dashboard/visualize.jag');
}
//var baseUrl = getBaseUrl(window.location.href);
var resultTable =  $('#results-table').DataTable( {
    "processing": false,
    "serverSide": true,
    "ajax" : {
        "url": serverUrl + "/api/search",
        "type": "POST",
        "dataType": "json",
        "contentType": "application/json; charset=utf-8",
        "data": function (payload) {
            payload.query = $("#search-field").val();
           // payload.timeFrom = parseInt($("#timestamp-from").val());
           // payload.timeTo = parseInt($("#timestamp-to").val());
            //document.getElementById("logpath").innerHTML = facetPath;
            payload.facetPath =$("#facetPath").val();
            payload.timeFrom = 0;
            payload.timeTo = 8640000000000000;
            //payload.tableName="LOGANALYZER";
            //payload.length = 100;
            payload.start =0;
            return JSON.stringify(payload)
        }
    },
    "columns": [
        {"data": "values._message"}
    ],
    "searching": false
});

/* Formatting a table to insert when a row is clicked */
function format(data) {

    var tableStr = '<table cellpadding="5" cellspacing="0" border="0" style="padding-left:50px;">';
    for(var key in data.values) {
        tableStr = tableStr +
        '<tr>' +
        '<td>' + capitalizeFirstLetter(key.replace("_","")) + '</td>' +
        '<td>' + data.values[key] + '</td>' +
        '</tr>';
    }
    tableStr = tableStr + '</table>';
    return tableStr;
}

/* Capitalize first letter of a given string*/
function capitalizeFirstLetter(string) {
    return string.charAt(0).toUpperCase() + string.slice(1);
}

$(document).ready(function () {
    addLogstream1();
    var showPopover = $.fn.popover.Constructor.prototype.show;
    $.fn.popover.Constructor.prototype.show = function () {
        showPopover.call(this);
        if (this.options.showCallback) {
            this.options.showCallback.call(this);
        }
    };

    $("#date-time-select").popover({
        html: true,
        content: function() {
            return $('#timeListContent').html();
        },
        showCallback: function () {
            $('.datepicker').datepicker();
            $('.timepicker').timepicker(
                {
                    'step': '20',
                    'minTime': '9:00am',
                    'maxTime': '12:00pm',
                    'timeFormat': 'H:i:s'
                }
            );
        }
    });

    $('#results-table').find('tbody').on( 'click', 'tr', function () {
        var tr = $(this).closest('tr');
        var row = resultTable.row( tr );

        if ( row.child.isShown() ) {
            // This row is already open - close it
            row.child.hide();
            tr.removeClass('shown');
        }
        else {
            // Open this row
            row.child( format(row.data()) ).show();v
            tr.addClass('shown');
        }
    } );

    $('#save-options').click(function () {
        if ("pdf" == $("#save-options").val()) {
            var doc = new jsPDF();
            doc.fromHTML($('#tab-preview').html(), 15, 15, {
                'width': 170
            });
            var string = doc.output('datauristring');
            var x = window.open();
            x.document.open();
            x.document.location = string;
        } else if ("csv" == $("#save-options").val()) {
            tableToCSV($('#results-table').dataTable(), 'table.table');
        }
    });

});

function searchActivities2() {
    var payload = {};
    payload.query = $("#search-field").val()
    payload.start = 0
    payload.count = 100
    console.log(payload)
    jQuery.ajax({
        type: "POST",
        data : JSON.stringify(payload),
        dataType : "json",
        contentType : "application/json; charset=utf-8",
        url: serverUrl + "/api/search",
        success: function(res) {
            appendDataToTable(res);
        },
        error: function(res) {
            alert(res.responseText);
        }
    });
}

function changeTime(value, timestampFrom, timestampTo) {
    var buttonHeight = $("#date-time-select").outerHeight();
    var buttonWidth = $("#date-time-select").outerWidth();
    $("#date-time-select").css({ 'height': buttonHeight});
    $("#date-time-select").css({ 'width': buttonWidth});
    $("#timestamp-from").val(timestampFrom);
    $("#timestamp-to").val(timestampTo);
    $("#date-time-select").text(value);
}

function assignDateRange() {
    var dateFrom = $("#dateRangeDatePickerFrom").val();
    var dateTo = $("#dateRangeDatePickerTo").val();
    changeTime(dateFrom + "-" + dateTo, new Date(dateFrom).getTime(), new Date(dateTo).getTime());
}

function assignDateTimeRange() {
    var dateFrom = $("#dateTimeRangeDatePickerFrom").val();
    var timeFrom = $("#dateTimeRangeTimePickerFrom").val();
    var dateTo = $("#dateTimeRangeDatePickerTo").val();
    var timeTo = $("#dateTimeRangeTimePickerTo").val();
    changeTime(dateFrom + ":" + timeFrom + "-" + dateTo + ":" + timeTo, new Date(dateFrom + " " + timeFrom).getTime(),
        new Date(dateTo + " " + timeTo).getTime());
}

function getLastWeek(){
    var today = new Date();
    return lastWeek = new Date(today.getFullYear(), today.getMonth(), today.getDate() - 7);
}

function getLastMonth(){
    var today = new Date();
    return lastWeek = new Date(today.getFullYear(), today.getMonth() -1, today.getDate());
}

function searchActivities(data){

  resultTable.ajax.reload();
}

function tableToCSV(table, tableElm) {
    var csv = [];

    // Get header names
    $(tableElm+' thead').find('th').each(function() {
        var $th = $(this);
        var text = $th.text();
        if(text != "") csv.push(text);
    });

    // get table data
    var total = table.fnSettings().fnRecordsTotal();
    for(i = 0; i < total; i++) {
        var row = table.fnGetData(i).values['_message'];
        csv.push(row);
    }

    var csvContent = "data:text/csv;charset=utf-8,";
    csv.forEach(function(infoArray, index){

        var dataString = infoArray;
        csvContent += index < csv.length ? dataString+ "\n" : dataString;

    });

    var encodedUri = encodeURI(csvContent);
    var link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute("download", "search_result.csv");
    link.click();
}


function addLogstream1() {
    var payload = {};
    var logstream = "logstream";
    var seperator = ",,";
    payload.query = logstream + seperator + " ";
    payload.start = 0;
    payload.length = 10000;
    payload.timeFrom = 0;
    payload.tableName = "LOGANALYZER";
    payload.timeTo = 8640000000000000;
    var jsonnn = JSON.stringify(payload);


    jQuery.ajax({
        url: serverUrl + "/api/dashboard/logStreamData",
        type: "POST",
        contentType: "application/json",
        dataType: "json",
        data: jsonnn,
        success: function (res) {
            //console.log(response);
            //document.getElementById("json_string3").innerHTML=res.length;
            var select = document.getElementById("0");
            for (var i = 0; i < res.length; i++) {
                var option = document.createElement('option');
                option.text = option.value = res[i];
                option.id=-1;
                select.add(option, 0);
            }

        },
        error: function (res) {
            var response = JSON.stringify(res);
            alert(res.error);
        }
    });
}
var facetCount =0;

var facetObj = {};
var testObj ={};

function addChildLogStream1(val,idVal) {

        var idInt = parseInt(idVal);

        //if(testObj.hasOwnProperty(idVal)){
        for (var key in testObj) {
            if (key > idVal) {
                $("#" + key).remove();
                delete testObj[key];
            }
        }
    if (val != "None") {
        // }
        if (!facetObj.hasOwnProperty(idVal)) {
            facetObj[idVal] = val;


            //facetData.push(val);

        }
        else {
            for (var key in facetObj) {
                if (key >= idInt) {
                    delete facetObj[key];
                    //delete facetObj;
                    if (key != idInt) {

                        $("#" + key).remove();
                    }
                }
            }

            facetCount = idInt;
            facetObj[idInt] = val;
            //facetData.push(val);

        }
        //document.getElementById("logTest").innerHTML = "Val "+val+"  idVal"+idVal+"  facetObj "+JSON.stringify(facetObj)+"  testObj "+JSON.stringify(testObj);

        var facetData = [];
        for (var key in facetObj) {
            facetData.push(facetObj[key]);
        }
        facetPath = facetData;
        //document.getElementById("json_string3").innerHTML=facetpath +"  "+JSON.stringify(facetObj);
        $("#facetPath").val(facetPath);
        var payload = {};
        var logstream = "logstream";
        var seperator = ",,";
        payload.query = logstream + seperator + facetPath;
        payload.start = 0;
        payload.length = 10000;
        payload.timeFrom = 0;
        payload.tableName = "LOGANALYZER";
        payload.timeTo = 8640000000000000;
        var jsonnn = JSON.stringify(payload);


        jQuery.ajax({
            url: serverUrl + "/api/dashboard/logStreamData",
            type: "POST",
            contentType: "application/json",
            dataType: "json",
            data: jsonnn,
            success: function (res) {
                //console.log(response);
                //document.getElementById("json_string3").innerHTML=res.length;
                //var select = document.getElementById("logstreamSelect");

                facetCount++;
                if (!testObj.hasOwnProperty(facetCount)) {
                    var streamDiv = document.getElementById("logStreamData");
                    var selectList = document.createElement("select");
                    selectList.id = facetCount;
                    testObj[facetCount] = "test";
                    //document.getElementById("json_string3").innerHTML = JSON.stringify(facetObj);
                    //selectList.onchange = addChildLogStream(this.value,this.id);
                    selectList.setAttribute("onchange", "addChildLogStream1(this.value,this.id)");
                    streamDiv.appendChild(selectList);
                    var option1 = document.createElement('option');
                     option1.value = "None";
                    option1.text ="Select a category"
                    selectList.add(option1);
                    for (var i = 0; i < res.length; i++) {
                        var option = document.createElement('option');
                        option.text = option.value = res[i];
                        selectList.add(option, 0);
                    }
                }
            },
            error: function (res) {
                var response = JSON.stringify(res);
                alert(res.error);
            }
        });

        //document.getElementById("logTest").innerHTML = facetpath;
    }

}