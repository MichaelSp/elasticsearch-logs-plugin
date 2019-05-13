# pipeline-elasticsearch-logs-plugin
A Jenkins plugin, that will write the logs of pipeline builds to elastic search.
Currently this plugin is intended for JenkinsFileRunner as it is not able to display the logs in Jenkins.

# Usage

After installation go to **Manage Jenkins » Configure System** and configure the section **Logging to Elastic Search for Pipelines**

# Events
Besides the log lines the plugin will sent additional events with flow node information.
The following table describes the different event types.

| event type | description |
| ----- | ----- |
| buildStart | Sent once at the start of the build |
| buildEnd | Sent once at the end of the build |
| nodeStart | Sent once at the start of the a flow node |
| nodeEnd | Sent once at the end of the a flow node |
| buildMessage | A log line sent from the pipeline execution engine (In Jenkins these are the lines displayed in light grey) |
| nodeMessage | A log output line sent from the execution of a flow node |

### Event types
The following table lists the fields that are sent for each event and for which type of event

| Field | description | buildStart | buildEnd | nodeStart | nodeEnd | buildMessage | nodeMessage | 
|-------|-------------|:----------:|:--------:|:----------:|:-------:| :----------: | :---------: |
| eventType | The type of event  | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| timestamp | UTC timestamp string | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| timestampMillis | tmestamp in ms since 1970-01-01 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| project | The project that is built | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| build | The build number  | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| instance | The Jenkins instance  | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| message | The log line  | | | | | ✓ | ✓ |
| node | The id of the flow node | |  |  ✓ | ✓ |  | ✓ |
| step | The name of the step | |  |  ✓ | ✓ |  | ✓ |
| stage | The name of the enclosing stage | |  |  ✓ | ✓ |  | ✓ |
| stageId | The id of the enclosing stage | |  |  ✓ | ✓ | | ✓ |
| agent | The name of the enclosing agent (node step) | |  |  ✓ | ✓ |  | ✓ |
| nodes | The list of flow nodes and their status |  | ✓ |  ✓ | ✓ |  | |

#### Flow node list
The list of flow nodes contains the following fields:

| Field | description |
| ----- | -----|
| displayName | The desription of the step as shown in the pipeline steps UI | 
| errorMessage | The message of the Exception if the step failed | 
| step | The name of the step | 
| id | The id of this flow node |
| enclosingId | The id of the enclosing block flow node | 
| status | The status of the execution of the node |
| duration | The duration of the step | 
