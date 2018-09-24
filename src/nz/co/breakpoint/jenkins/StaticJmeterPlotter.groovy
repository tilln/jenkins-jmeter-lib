package nz.co.breakpoint.jenkins

@Grab('com.cloudbees:groovy-cps:1.22')  
@Grab('org.knowm.xchart:xchart:3.5.2')
@Grab('com.xlson.groovycsv:groovycsv:1.3')
import static com.xlson.groovycsv.CsvParser.parseCsv
import static org.knowm.xchart.style.markers.SeriesMarkers.CIRCLE
import static org.knowm.xchart.style.markers.SeriesMarkers.NONE
import static org.knowm.xchart.XYSeries.XYSeriesRenderStyle.Scatter
import static org.knowm.xchart.XYSeries.XYSeriesRenderStyle.Line
import static org.knowm.xchart.BitmapEncoder.BitmapFormat.PNG
import static org.knowm.xchart.BitmapEncoder.getBitmapBytes
import com.cloudbees.groovy.cps.NonCPS

/* Plots a JMeter response time CSV input file to HTML with embedded PNG charts.
 * Generates:
 *  - Response times over time scatter plot
 *  - Response time percentiles
 *  - Transactions per minute over time
 *  - Aggregates: Response time averages, 90th percentiles, transaction count
 * CSV must have at least the columns (and a header specifying their order):
 *   timeStamp,label,elapsed
 * With timeStamp column format as below or epoch milliseconds.
 */
class StaticJmeterPlotter implements Serializable {

    static def DTFORMAT = 'yyyy-MM-dd HH:mm:ss.SSS'
    static def TPMINTERVAL = 60000 // 1 minute

    @NonCPS
    static def generateHtml(inputs, parameters) {

        def data = inputs.collectMany { csv ->
            parseCsv(new FileReader(csv)).collect { row ->
                [timeStamp: row.timeStamp ==~ /\d\d\d\d-\d\d-\d\d \d\d:\d\d:\d\d\.\d+/ ? new Date().parse(DTFORMAT, row.timeStamp) : new Date(row.timeStamp.toLong()), 
                elapsed:    row.elapsed.toLong(), 
                label:      row.label,
                success:    row.success]
        }   }

        if (!data) throw new RuntimeException('No input data')

        def startTime = (data.timeStamp*.getTime().min()/TPMINTERVAL).toLong()
        def endTime = ((data.timeStamp*.getTime().max()+TPMINTERVAL)/TPMINTERVAL).toLong()
        def timeRange = (0..<(endTime-startTime)).collectEntries { [((startTime+it)*TPMINTERVAL): 0] } // empty histogram for TPM calculations

        def charts =  new org.knowm.xchart.XYChartBuilder().width(1200).height(600).with { [
            responsetimes : title("Response Times")           .xAxisTitle("Time")      .yAxisTitle("Milliseconds")           .build(),
            percentiles   : title("Percentile Response Times").xAxisTitle("Percentile").yAxisTitle("Milliseconds")           .build(),
            throughput    : title("Throughput")               .xAxisTitle("Time")      .yAxisTitle("Transactions per Minute").build(),
        ] }

        def aggregates = [
            averages        : [:],
            percentiles     : [:],
            transactions    : [:],
        ]

        charts.responsetimes.styler
            .setDefaultSeriesRenderStyle(Scatter)
            .setMarkerSize(parameters.markersize ?: 3)
            .setYAxisMax(parameters.yaxismax ?: 2000)

        charts.percentiles.styler
            .setDefaultSeriesRenderStyle(Line)
            .setYAxisMax(parameters.yaxismax ?: 2000)

        charts.throughput.styler
            .setDefaultSeriesRenderStyle(Line)
            .setMarkerSize(0)

        data.groupBy { it.label }.each { label, series ->
            def timeStamp = series.timeStamp
            def elapsed = series.elapsed
            charts.responsetimes.addSeries(label, timeStamp, elapsed).setMarker(CIRCLE)
            
            def range = (0..99)*.multiply(series.size()/100.0)*.toLong()
            def percentiles = elapsed.toSorted()[range]
            charts.percentiles.addSeries(label, (0..99), percentiles).setMarker(NONE)
            
            def transactions = timeStamp
                .collect { it.getTime()-it.getTime()%TPMINTERVAL } // truncate to full minute
                .inject(timeRange.clone().withDefault { 0 }) { tpm, minute -> tpm[minute] += 1; tpm } // count numbers of requests for each minute
                .collectMany { minute, count -> [[t:new Date(minute), tpm:count]] } // separate into 2 columns
            charts.throughput.addSeries(label, transactions.t, transactions.tpm)
            
            aggregates.averages[label] = elapsed.with{sum()/size()}
            aggregates.percentiles[label] = percentiles[parameters.percentile ?: 90]
            aggregates.transactions[label] = transactions.tpm.sum()
        }

        def writer = new StringWriter()
        new groovy.xml.MarkupBuilder(writer).html {
            head { title("Response Times") } 
            body { table { 
                tr {
                    td { img(src: "data:image/png;base64,${getBitmapBytes(charts.responsetimes, PNG).encodeBase64()}") }
                    td {
                        table { 
                            tr { th("Request"); th("Average Response Times (milliseconds)") }
                            aggregates.averages.collect { label, value -> tr { td(label); td(sprintf('%.0f', value)) } }
                }    }    }
                tr {
                    td { img(src: "data:image/png;base64,${getBitmapBytes(charts.percentiles, PNG).encodeBase64()}") }
                    td {
                        table { 
                            tr { th("Request"); th("${parameters.percentile ?: 90}th Percentile Response Times (milliseconds)") }
                            aggregates.percentiles.collect { label, value -> tr { td(label); td(value) } }
                }    }    }
                tr {
                    td { img(src: "data:image/png;base64,${getBitmapBytes(charts.throughput, PNG).encodeBase64()}") }
                    td { 
                        table {
                            tr { th("Request"); th("Transaction Count") }
                            aggregates.transactions.collect { label, value -> tr { td(label); td(value) } }
        }   }   }   }   }   }
        return writer.toString()
    }
    
    static def main(args) {
        def cli = new CliBuilder(usage: "${this.simpleName}.groovy [options] <files>")
        def defaults = [
            percentile: 90,
            markersize: 3,
            yaxismax:   2000,
        ]
        cli.with {
            h longOpt: 'help', 'Show usage information'
            p longOpt: 'percentile', args: 1, argName: 'integer', "Percentile to print (default = ${defaults.percentile})"
            m longOpt: 'markersize', args: 1, argName: 'integer', "Size of the scatter chart markers (default = ${defaults.markersize})"
            y longOpt: 'yaxismax',   args: 1, argName: 'milliseconds', "Y axis limit, i.e. maximum response times (default = ${defaults.yaxismax})"
        }
        def options = cli.parse(args)
        if (options.h) {
            cli.usage()
        } else {
            println generateHtml(options.arguments(), [
                percentile: options.p ? options.p.toInteger() : defaults.percentile, 
                markersize: options.m ? options.m.toInteger() : defaults.markersize, 
                yaxismax:   options.y ? options.y.toInteger() : defaults.yaxismax, 
            ])
        }
    }
}