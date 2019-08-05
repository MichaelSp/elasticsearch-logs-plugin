package com.sap.prd.jenkins.plugins.pipeline_elasticsearch_logs.runid;

import hudson.model.AbstractDescribableImpl;
import net.sf.json.JSONObject;

public abstract class JsonSource extends AbstractDescribableImpl<JsonSource>
{

  public abstract String getJsonString();

  public abstract JSONObject getJsonObject();
  
}
