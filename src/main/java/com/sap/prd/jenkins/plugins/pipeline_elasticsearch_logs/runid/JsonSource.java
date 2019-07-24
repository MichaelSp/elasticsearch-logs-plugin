package com.sap.prd.jenkins.plugins.pipeline_elasticsearch_logs.runid;

import hudson.model.AbstractDescribableImpl;

public abstract class JsonSource extends AbstractDescribableImpl<JsonSource>
{

  public abstract String getJson();
  
}
