# JMeter Jenkins Pipeline Library

Usage
-----

### jmeterPlot

Generate a JMeter response times scatter plot

```groovy
@Library('jenkins-jmeter-lib') _

node() {
    //...
    jmeterPlot(inputs: '**/jmeter.csv', output: 'ResponseTimes.html', hover: 'AccountNo')
    //...
}
```

Setup
-----

1. Add [pipeline library](https://jenkins.io/doc/book/pipeline/shared-libraries/#global-shared-libraries) via *Manage Jenkins » Configure System » Global Pipeline Libraries*
![Global library](https://jenkins.io/doc/book/resources/pipeline/add-global-pipeline-libraries.png)

2. Set the following System property to properly display the contents of the generated results files:
`"hudson.model.DirectoryBrowserSupport.CSP"="default-src 'self'; script-src 'self' https://cdn.plot.ly 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data:;"`

3. Make sure the [Pipeline Utility Steps](https://plugins.jenkins.io/pipeline-utility-steps) plugin is installed.
