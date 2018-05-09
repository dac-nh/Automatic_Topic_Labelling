var canvasDefaultData = [];
//        var keywordTable = $('#keywordTable').DataTable({
//            order: [[1, 'desc']],
//            pageLength: 8
//        });
var documentTable = $('#documentTable').DataTable({
    order: [[1, 'asc']],
    "columnDefs": [
        {
            "targets": [0],
            "visible": false
        }
    ],
    pageLength: 9
});
var myCanvas = $('#myNetwork').get(0),
    canvasData, network, nodes, edges;
// Canvas Initiation
nodes = new vis.DataSet();
edges = new vis.DataSet();
canvasData = {nodes: nodes, edges: edges};
var options = {
    nodes: {
        shape: "dot",
        size: 15,
        font: {
            size: 17,
            color: '#ffffff'
        },
        borderWidth: 2
    },
    edges: {
        width: 2
    }
};
network = new vis.Network(myCanvas, canvasData, options);

// Canvas event
// Loading bar
network.on("stabilizationProgress", function (params) {
    var maxWidth = 496;
    var minWidth = 20;
    var widthFactor = params.iterations / params.total;
    var width = Math.max(minWidth, maxWidth * widthFactor);

    document.getElementById('bar').style.width = width + 'px';
    document.getElementById('text').innerHTML = Math.round(widthFactor * 100) + '%';
});
network.on("stabilizationIterationsDone", function () {
    document.getElementById('text').innerHTML = '100%';
    document.getElementById('bar').style.width = '100%';
    document.getElementById('loadingBar').style.opacity = 0;
    // really clean the dom element
    setTimeout(function () {
        document.getElementById('loadingBar').style.display = 'none';
    }, 500);
});

// Load sub-topic
network.on("oncontext", function (params) {
    params.event = "[original event]";
//            keywordTable.clear();
    // Loop all list topic
    var listTopic = nodes.get(params.nodes[0])['child'][0];
    for (var i = 0; i < listTopic.length; i++) {
        nodes.add(listTopic[i]);

    }
    // Loop all list edge
    var listEdge = nodes.get(params.nodes[0])['child'][1];
    for (var j = 0; j < listEdge.length; j++) {
        edges.add(listEdge[j])
    }
});
network.on("click", function (params) {
});
// load keyword and document
network.on("doubleClick", function (params) {
    params.event = "[original event]";
//            keywordTable.clear();
//            keywordTable.draw(false);
    documentTable.clear();
    documentTable.draw(false);
    // Title of 2 tables
    $('#keywordTopic').text("Keywords of " + nodes.get(params.nodes[0])['label']);
    $('#documentTopic').text("Papers of " + nodes.get(params.nodes[0])['label']);

    var data = {
        topicId: params.nodes[0]
    };
    $.post(window.location.origin + "/orient-db/get-list-keyword", data,
        function (bean, status) {
            var keywords = bean;
            var jqcloudData = []; // Data for jqcloud
            var max = keywords[keywords.length - 1].max;
            // No data
            if (keywords.length === 1) {
                jqcloudData.push({
                    text: 'No data found',
                    weight: 1
                });
            }
            for (var i = 0; i < keywords.length - 1; i++) {
                // update jqcloud
                jqcloudData.push({
                    text: keywords[i].label,
                    weight: keywords[i].weight
                });

//                            keywordTable.row.add([
//                                keywords[i].label,
//                                '<div class="progress"><div class="progress-bar" role="progressbar" aria-valuenow="'
//                                + keywords[i].weight * 100 / max + '" '
//                                + 'aria-valuemin="0" aria-valuemax="100" style="width:' + keywords[i].weight * 100 / max + '%"\>'
//                                + keywords[i].weight
//                                + '</div></div>'
//                            ]).draw(false);
            }
            $('#keywordCloud').jQCloud('update', jqcloudData);
        }
    );
    $.post(window.location.origin + "/orient-db/get-list-document", data,
        function (bean, status) {
            var document = bean;
            for (var i = 0; i < document.length; i++) {
                var text = document[i].label;
                if (text.length > 30) {
                    text = text.substr(0, 28) + '..';
                }
                documentTable.row.add([
                    document[i].id,
                    text,
                    '<button type="button" class="btn btn-labeled btn-primary" id="open-document"">' +
                    '<span class="btn-label">' +
                    '<i class="glyphicon glyphicon-open-file"></i>' +
                    '</span>Abstract' +
                    '</button>'
                ]).draw(false);
            }
        }
    )
});

// Data Table event for paper abstract
documentTable.on('click', 'button', function () {
    var row_data = documentTable.row($(this).parents('tr')).data();
    var id = row_data[0]; // get paper id
    var label = row_data[1]; // get label
    var data = {
        id: id
    };
    $.post(window.location.origin + "/orient-db/get-paper-abstract", data, function (bean, status) {
        BootstrapDialog.show({
            type: BootstrapDialog.TYPE_INFO,
            size: BootstrapDialog.SIZE_NORMAL,
            title: 'Abstract of ' + label,
            message: bean.content,
            buttons: [{
                label: 'Close',
                cssClass: 'btn-default',
                action: function (dialog) {
                    dialog.close();
                }
            }]
        });
    });
});

// Search topic
function searchTopic() {
    var topicLabel = $('#search-topic-input').val();
    if (topicLabel === "") {
        canvasDefaultData = bean[0]; // update to canvas default data
        nodes.clear();
        nodes.add(canvasDefaultData);
        network.stabilize();
        return;
    }
    var data = {
        topicLabel: topicLabel
    };

    $.post(window.location.origin + "/orient-db/search-topic-by-label", data, function (bean, status) {
        nodes.clear();
        nodes.add(bean[0]);
        network.stabilize();
    });
}

$(function () {
    $("#tabs").tabs();
});

$(document).ready(function () {
    $.get(window.location.origin + "/orient-db/get-list-topic", function (bean, status) {
        canvasDefaultData = bean[0]; // update to canvas default data
        nodes.clear();
        nodes.add(canvasDefaultData);
        network.stabilize();

        // Constructs the suggestion engine
        $.get(window.location.origin + "/orient-db/get-all-topic-label", function (bean, status) {
            var topics = new Bloodhound({
                datumTokenizer: Bloodhound.tokenizers.whitespace,
                queryTokenizer: Bloodhound.tokenizers.whitespace,
                local: bean // use for local call
//                    prefetch: bean // use for json file
            });
            // Initializing the typeahead with remote dataset
            $('.typeahead').typeahead(null, {
                name: 'topics',
                source: topics,
                limit: 10 /* Specify maximum number of suggestions to be displayed */
            });
        });
    });
    // Initiation keyword cloud
    var words = [
        {
            text: "keywords", weight: 13, handlers: {
            click: function () {
                alert('You clicked the word !');
            }
        }
        },
        {text: "of", weight: 10.5},
        {text: "Selected", weight: 9.4},
        {text: "Topic", weight: 7}
    ];
    $('#keywordCloud').jQCloud(words, {
        autoResize: true,
        delay: 50
    });
});