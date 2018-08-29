package nz.co.breakpoint.jenkins

class JmeterPlotter implements Serializable {

    @NonCPS
    static def generateHtml(inputs, type='scatter', hoverValue='') {
        def writer = new StringWriter()
        new groovy.xml.MarkupBuilder(writer).html {
            head {
                script('', src: 'https://cdn.plot.ly/plotly-latest.min.js')
                script(type: 'text/javascript') {
                    mkp.yieldUnescaped """
                        function plotJmeterFile(sourceFile, targetDiv, type, hoverValue) {
                            Plotly.d3.csv(sourceFile, function processData(rows) {
                                var traces = {};
                                var millis, ylabel;
                                switch (type.toLowerCase()) {
                                    case 'monitor': ylabel = 'Measured Value';
                                        break;
                                    case 'tps':     ylabel = 'Transactions per Second';
                                                    millis = 1000;
                                        break;
                                    case 'tpm':     ylabel = 'Transactions per Minute';
                                                    millis = 1000*60;
                                        break;
                                    case 'tpm':     ylabel = 'Transactions per Hour';
                                                    millis = 1000*60*60;
                                        break;
                                    default:        ylabel = 'Milliseconds';
                                        break;
                                }
                                if (type.match(/^tp/)) {
                                    var timeslots = {};
                                    var max, min;
                                    for (var i in rows) {
                                        var label = rows[i]['label'];
                                        timeslots[label] = timeslots[label] || {};
                                        var timestamp = rows[i]['timeStamp'].trim();
                                        if (timestamp.match(/^[0-9]+$/)) timestamp = parseInt(timestamp);
                                        var slot = Math.trunc(new Date(timestamp).valueOf()/millis)*millis;
                                        // fill gaps with zeros, i.e. slots where there are no transactions:
                                        for (var t = max+millis; t < slot; t += millis) { timeslots[label][t] = 0; }
                                        for (var t = slot; t < min-millis; t += millis) { timeslots[label][t] = 0; }
                                        timeslots[label][slot] = 1+(timeslots[label][slot] || 0);
                                        max = Math.max(max || slot, slot);
                                        min = Math.min(min || slot, slot);
                                    }
                                    traces = Object.keys(timeslots).map(function(label){ return {
                                        x: Object.keys(timeslots[label]).map(function(slot){return new Date(parseInt(slot));}),
                                        y: Object.values(timeslots[label]), text: [], type: 'line', name: label
                                    };});
                                }
                                else {
                                    for (var i in rows) {
                                        var label = rows[i]['label'];
                                        var value = (type != 'monitor') ? rows[i]['elapsed'] : rows[i]['responseMessage'];
                                        var timestamp = rows[i]['timeStamp'].trim();
                                        if (timestamp.match(/^[0-9]+$/)) timestamp = new Date(parseInt(timestamp));
                                        traces[label] = traces[label] || (type == 'scatter' ?
                                            { x: [], y: [], text: [], type: 'scatter', mode: 'markers', 'hoverinfo': 'x+text', name: label } :
                                            { x: [], y: [], text: [], type: 'line', name: label });
                                        traces[label].x.push(timestamp);
                                        traces[label].y.push(value);
                                        if (hoverValue) traces[label].text.push(""+hoverValue+'='+rows[i][hoverValue]);
                                    }
                                    traces = Object.values(traces);
                                }
                                var layout = { xaxis: { title: 'Time' }, yaxis: { title: ylabel }, hovermode: 'closest' };
                                Plotly.newPlot(targetDiv, traces, layout);
                            });
                        }
                    """
                }
            }
            body(style: 'margin: 30px 0 0 0; padding: 0px') {
                div(style: 'height: 20px; position: absolute; top:5; font: 100% verdana,arial,helvetica;') {
                    label(for: 'source-selector', 'File:')
                    select(id: 'source-selector') {
                        inputs.collect { option(it) }
                    }
                }
                div(id: 'plot', style: 'width:100%; height:100%;') {
                    script """
                        var sourceSelector = document.querySelector('#source-selector');
                        sourceSelector.onchange = function(){ plotJmeterFile(sourceSelector.value, 'plot', '$type', '$hoverValue'); };
                        sourceSelector.onchange();
                    """
                }
            }
        }
        return writer.toString()
    }
}