import nz.co.breakpoint.jenkins.JmeterPlotter
import nz.co.breakpoint.jenkins.StaticJmeterPlotter

def call(parameters = [:]) {
    def inputs = findFiles(glob: parameters.inputs ?: '**/*.csv')
    def output = parameters.output ?: 'index.html'
    def type = parameters.type ?: 'scatter'
    def html = type =~ /(?i)static/ ? StaticJmeterPlotter.generateHtml(parameters.path, inputs, parameters)
        : JmeterPlotter.generateHtml(inputs, type.toLowerCase(), parameters.hover ?: '')
    writeFile text: html, file: output
}
