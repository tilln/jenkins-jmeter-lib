# JMeter Jenkins Pipeline Library

Usage
-----

### jmeterPlot

Generate a JMeter response times scatter plot for response times or a line chart for monitoring data.

```groovy
@Library('jenkins-jmeter-lib') _

node() {
    //...
    jmeterPlot(inputs: '**/jmeter.csv', output: 'ResponseTimes.html', hover: 'AccountNo')
    
    // Useful in conjunction with HTML Publisher:
    publishHTML reportFiles: 'ResponseTimes.html', reportDir: '.' // archive the entire folder incl. CSV files
}
```

Parameters:
- inputs: Ant-style Glob pattern defining input CSV files to load. Those need to be present in the Workspace or html archive. (default: **/*.csv)
- output: Name of html file to generate in the Workspace. (default: index.html)
- type: Two types are currently supported, scatter and monitor. (default: scatter)
- hover: Additional text to display when hovering over data points. Useful for analysing outliers. (default: '')

Limitations:
- Only the JMeter CSV format is supported (not JTL/XML format).
- Only the JMeter Plugins CSV format for monitoring is supported (measured value in the `responseMessage` column).
- CSV files need to be present in the Workspace or HTML report archive, at a URL relative to the HTML file.
- Requires access to https://cdn.plot.ly from the browser displaying the Build. 


Setup
-----

1. Add [pipeline library](https://jenkins.io/doc/book/pipeline/shared-libraries/#global-shared-libraries) via *Manage Jenkins » Configure System » Global Pipeline Libraries*
![Global library](https://jenkins.io/doc/book/resources/pipeline/add-global-pipeline-libraries.png)

2. Set the following System property to properly display the contents of the generated results files:
`"hudson.model.DirectoryBrowserSupport.CSP"="default-src 'self'; script-src 'self' https://cdn.plot.ly 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data:;"`

3. Make sure the [Pipeline Utility Steps](https://plugins.jenkins.io/pipeline-utility-steps) plugin is installed.
