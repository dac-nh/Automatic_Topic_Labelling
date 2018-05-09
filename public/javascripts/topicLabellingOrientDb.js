var keywordTable = $('#keywordTable').DataTable({
    order: [[1, 'desc']],
    pageLength: 5
});

var standardPieData = [{
    name: "Default",
    y: 1
}, {
    name: "Value",
    y: 1,
    sliced: true,
    selected: true
}];

var highChartEuclidean = Highcharts.chart('topic-pie-chart-euclidean', {
    chart: {
        plotBackgroundColor: null,
        plotBorderWidth: null,
        plotShadow: false,
        type: 'pie'
    },
    title: {
        text: "Topic labelling by Euclidean"
    },
    tooltip: {
        pointFormat: '{series.name}: <b>{point.percentage:.1f}%</b>'
    },
    plotOptions: {
        pie: {
            allowPointSelect: true,
            cursor: 'pointer',
            depth: 35,
            dataLabels: {
                enabled: false,
                format: '{point.name}'
            },
            showInLegend: true
        }
    },
    series: [{
        name: 'Brands',
        colorByPoint: true,
        data: standardPieData
    }]
});

var highChartKL = Highcharts.chart('topic-pie-chart-kl', {
    chart: {
        plotBackgroundColor: null,
        plotBorderWidth: null,
        plotShadow: false,
        type: 'pie'
    },
    title: {
        text: "Topic labelling by K-L Divergence"
    },
    tooltip: {
        pointFormat: '{series.name}: <b>{point.percentage:.1f}%</b>'
    },
    plotOptions: {
        pie: {
            allowPointSelect: true,
            cursor: 'pointer',
            depth: 35,
            dataLabels: {
                enabled: false,
                format: '{point.name}'
            },
            showInLegend: true
        }
    },
    series: [{
        name: 'Brands',
        colorByPoint: true,
        data: standardPieData
    }]
});

$('#inputFile').get(0).onchange = function () {
    $('#fileName').val(this.files[0].name);
    $('#progressBar').get(0).style.width = "100%";
};

function topicLabeling() {
    // Check if no file
    if ($('#inputFile').get(0).files[0] === undefined) {
        $('#noFileWarning').show();
        return;
    }

    var formData = new FormData();
    formData.append('file', $('#inputFile').get(0).files[0]);
    // show waiting dialog
    waitingDialog.show('Processing');
    $.ajax({
        url: window.location.origin + "/orient-db/topic-labeling",
        type: 'POST',
        data: formData,
        processData: false,  // tell jQuery not to process the data
        contentType: false,  // tell jQuery not to set contentType
        success: function (data) {
            var euclidean = data[0].euclidean;
            var kl = data[0].kl;

            // Keywords table
            keywordTable.clear();
            keywordTable.draw(false);
            var keywords = data[1];
            var max = keywords[keywords.length - 1].max;
            for (var i = 0; i < keywords.length - 1; i++) {
                keywordTable.row.add([
                    keywords[i].label,
                    '<div class="progress"><div class="progress-bar" role="progressbar" aria-valuenow="'
                    + keywords[i].weight * 100 / max + '" '
                    + 'aria-valuemin="0" aria-valuemax="100" style="width:' + keywords[i].weight * 100 / max + '%"\>'
                    + keywords[i].weight
                    + '</div></div>'
                ]).draw(false);
            }
            // Euclidean processing
            var highChartEuclideanData = [];
            var topicNameListEuclidean = "";
            var topicWeightListEuclidean = "";
            for (var i = 0; i < euclidean.listTopic.length; i++) {
                var topic = {};
                if (i === 0) {
                    // create topic
                    topic = {
                        name: euclidean.listTopic[i].label,
                        y: euclidean.highest - round(euclidean.listTopic[i].weight, 5),
                        sliced: true,
                        selected: true
                    };
                } else {
                    // create topic
                    topic = {
                        name: euclidean.listTopic[i].label,
                        y: euclidean.highest - round(euclidean.listTopic[i].weight, 5)
                    }
                }
                highChartEuclideanData.push(topic); // add to topic array
                // Add keyword and weight to table
                if (euclidean.listTopic[i].label.length > 25) { // cut length of euclidean
                    topicNameListEuclidean += "\n Topic " + i + ": " + euclidean.listTopic[i].label.substr(0, 23) + '..';
                } else {
                    topicNameListEuclidean += "\n Topic " + i + ": " + euclidean.listTopic[i].label
                }
                topicWeightListEuclidean += "\n" + round(euclidean.listTopic[i].weight, 5)
            }
            highChartEuclidean.series[0].setData(highChartEuclideanData, true); // reload high chart
            // Add to table Euclidean
            $('#topicNameEuclidean').text(topicNameListEuclidean);
            $('#topicWeightEuclidean').text(topicWeightListEuclidean);

            // K-L Divergence processing
            var highChartKLData = [];
            var topicNameListKl = "";
            var topicWeightListKl = "";
            for (var i = 0; i < kl.listTopic.length; i++) {
                var topic = {};
                if (i === 0) {
                    // create topic
                    topic = {
                        name: kl.listTopic[i].label,
                        y: kl.highest - round(kl.listTopic[i].weight, 5),
                        sliced: true,
                        selected: true
                    };
                } else {
                    // create topic
                    topic = {
                        name: kl.listTopic[i].label,
                        y: kl.highest - round(kl.listTopic[i].weight, 5)
                    }
                }
                highChartKLData.push(topic); // add to topic array
                // Add keyword and weight to table
                if (kl.listTopic[i].label.length > 25) { // cut length of kl
                    topicNameListKl += "\n Topic " + i + ": " + kl.listTopic[i].label.substr(0, 23) + '..';
                } else {
                    topicNameListKl += "\n Topic " + i + ": " + kl.listTopic[i].label
                }
                topicWeightListKl += "\n" + round(kl.listTopic[i].weight, 5)
            }
            highChartKL.series[0].setData(highChartKLData, true); // reload high chart

            // Add to table Kl
            $('#topicNameKl').text(topicNameListKl);
            $('#topicWeightKl').text(topicWeightListKl);
            waitingDialog.hide(); // high waiting dialog
        }
    });
}

$(document).ready(function () {
    if ($('#fileName').get(0).value !== "") {
        $('#progressBar').get(0).style.width = "100%";
    }
});