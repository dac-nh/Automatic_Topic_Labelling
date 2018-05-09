var keywordTable = $('#keywordTable').DataTable({
    order: [[1, 'desc']],
    pageLength: 8
});
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

//Canvas event
network.on("stabilizationProgress", function(params) {
    var maxWidth = 496;
    var minWidth = 20;
    var widthFactor = params.iterations/params.total;
    var width = Math.max(minWidth,maxWidth * widthFactor);

    document.getElementById('bar').style.width = width + 'px';
    document.getElementById('text').innerHTML = Math.round(widthFactor*100) + '%';
});
network.once("stabilizationIterationsDone", function() {
    document.getElementById('text').innerHTML = '100%';
    document.getElementById('bar').style.width = '496px';
    document.getElementById('loadingBar').style.opacity = 0;
    // really clean the dom element
    setTimeout(function () {document.getElementById('loadingBar').style.display = 'none';}, 500);
});
network.on("click", function (params) {
});
network.on("doubleClick", function (params) {
    params.event = "[original event]";
    keywordTable.clear();
    keywordTable.draw(false);
    documentTable.clear();
    documentTable.draw(false);
    // Title of 2 tables
    $('#keywordTopic').text("Keywords of " + nodes.get(params.nodes[0])['label']);
    $('#documentTopic').text("Papers of " + nodes.get(params.nodes[0])['label']);

    var data = {
        topicLabel: nodes.get(params.nodes[0])['label']
    };
    $.post(window.location.origin + "/neo4j/get-list-keyword", data,
        function (bean, status) {
            var keywords = bean;
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
        }
    );
});

// Data Table event for paper abstract
documentTable.on('click', 'button', function () {
    var row_data = documentTable.row($(this).parents('tr')).data();
    var id = row_data[0]; // get paper id
    var label = row_data[1]; // get label
    var data = {
        id: id
    };
    $.post(window.location.origin + "/get-paper-abstract", data, function (bean, status) {
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

$(function () {
    $("#tabs").tabs();
});

$(document).ready(function () {
    $.get(window.location.origin + "/neo4j/get-list-topic", function (bean, status) {
        nodes.clear();
        nodes.add(bean[0]);
        edges.add(bean[1]);
        network.stabilize();
    });
});