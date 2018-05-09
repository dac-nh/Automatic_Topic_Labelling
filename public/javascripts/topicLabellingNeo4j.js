var keywordTable = $('#keywordTable').DataTable({
    order: [[1, 'desc']],
    pageLength: 10
});
$('#inputFile').get(0).onchange = function () {
    $('#fileName').val(this.files[0].name);
    $('#progressBar').get(0).style.width = "100%";
};

function topicLabeling() {
    var formData = new FormData();
    formData.append('file', $('#inputFile').get(0).files[0]);
    formData.append('level', 2);
    // show waiting dialog
    waitingDialog.show('Processing');
    $.ajax({
        url: window.location.origin + "/topic-labeling-neo4j-post",
        type: 'POST',
        data: formData,
        processData: false,  // tell jQuery not to process the data
        contentType: false,  // tell jQuery not to set contentType
        success: function (data) {
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

            var topicNameList = "";
            var topicWeightList = "";
            for (var i = 0; i < data[0].length; i++) {
                if (i === 0) {
                    topicNameList += "The highest probability topic: " + data[0][i].label;
                    topicWeightList += round(data[0][i].weight, 5)
                } else {
                    topicNameList += "\n Topic " + i + ": " + data[0][i].label;
                    topicWeightList += "\n" + round(data[0][i].weight, 5)
                }
            }
            $('#topicName').text(topicNameList);
            $('#topicWeight').text(topicWeightList);

            waitingDialog.hide();

        }
    });
}

$(document).ready(function () {
    if ($('#fileName').get(0).value !== "") {
        $('#progressBar').get(0).style.width = "100%";
    }
});