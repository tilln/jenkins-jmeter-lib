package nz.co.breakpoint.jenkins

class JmeterPlotter implements Serializable {

    @NonCPS
    static def generateHtml(inputs, isScatter=true, hoverValue='') {
        def writer = new StringWriter()
        new groovy.xml.MarkupBuilder(writer).html {
            head {
                script('', src: 'https://cdn.plot.ly/plotly-latest.min.js')
                script(type: 'text/javascript') {
                    mkp.yieldUnescaped """
                        function plotJmeterFile(sourceFile, targetDiv, isScatter, hoverValue) {
                            Plotly.d3.csv(sourceFile, function processData(rows) {
                                var traces = {};
                                for (var i=0; i<rows.length; i++) {
                                    var label = rows[i]['label'];
                                    var value = isScatter ? rows[i]['elapsed'] : rows[i]['responseMessage'];
                                    var timestamp = rows[i]['timeStamp'].trim();
                                    if (timestamp.match(/^[0-9]+\$/)) timestamp = new Date(parseInt(timestamp));
                                    traces[label] = traces[label] || (isScatter ?
                                        { x: [], y: [], text: [], type: 'scatter', mode: 'markers', 'hoverinfo': 'x+text', name: label } :
                                        { x: [], y: [], text: [], type: 'line', name: label });
                                    traces[label].x.push(timestamp);
                                    traces[label].y.push(value);
                                    if (hoverValue) traces[label].text.push(""+hoverValue+'='+rows[i][hoverValue]);
                                }
                                var layout = {
                                    xaxis: { title: 'Time' },
                                    yaxis: { title: isScatter ? 'Milliseconds' : 'Measured Value' },
                                    hovermode: 'closest'
                                };
                                traces = Object.keys(traces).map(function(key){return traces[key]}); // convert map to array of values
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
                        sourceSelector.onchange = function(){ plotJmeterFile(sourceSelector.value, 'plot', $isScatter, '$hoverValue'); };
                        sourceSelector.onchange();
                    """
                }
            }
        }
        return writer.toString()
    }
}