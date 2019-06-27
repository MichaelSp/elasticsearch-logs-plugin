### Description

A Jenkins plugin to send Pipeline build logs to an [Elasticsearch](https://www.elastic.co/products/elasticsearch) instance.

This is an [implementation of JEP-210](https://github.com/jenkinsci/jep/blob/master/jep/210/README.adoc).


### Requirements

 - [Jenkins](https://jenkins.io/)
 - [Elasticsearch](https://www.elastic.co/products/elasticsearch)

### Download and Installation

In your Jenkins go to **Manage Jenkins » Manage Plugins » Available** check **Pipeline Logging via Elastic Search** and press **Install without restart**

### Configuration

tbd.

### Limitations

Currently the plugin is only able to push the logs to Elasticsearch but the way to read the logs from ElasticSearch and display in Jenkins is not yet implemented.
The initial scope of this plugin was to use it it in a [JenkinsFileRunner](https://github.com/jenkinsci/jenkinsfile-runner) scenario.


### Known Issues

see Jenkins Jira

### How to obtain support

tbd.: 
 - Link to Jira component once the plugin is officially hosted in the [JenkinsCI](https://github.com/jenkinsci) org.
 - Link to Plugin wiki on Jenkins site where once can ask questions and see open issues


### Contributing

Please open a Pull Request in our [Github](https://github.com/jenkinsci/pipeline-elasticsearch-logs-plugin) repository.

### To-Do (upcoming changes)



### License

The following text should appear in the license section.  

Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
This file is licensed under the "Apache Software License, v. 2" except as noted otherwise in the LICENSE file .”

