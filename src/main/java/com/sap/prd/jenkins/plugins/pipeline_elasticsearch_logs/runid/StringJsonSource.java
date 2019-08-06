package com.sap.prd.jenkins.plugins.pipeline_elasticsearch_logs.runid;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.Descriptor;
import net.sf.json.JSONObject;

public class StringJsonSource extends JsonSource
{
  private transient JSONObject jsonObject;
  
  private final String jsonString;

  @DataBoundConstructor
  public StringJsonSource(String jsonString)
  {
    this.jsonObject = JSONObject.fromObject(jsonString);
    this.jsonString = jsonObject.toString();
  }

  public String getJsonString()
  {
    return jsonString;
  }

  @Override
  public JSONObject getJsonObject() {
    if(jsonObject == null) jsonObject = JSONObject.fromObject(jsonString);
    return jsonObject;
  }

  @Extension
  public static class DescriptorImpl extends Descriptor<JsonSource>
  {

    @Override
    public String getDisplayName()
    {
      return "JSON String";
    }
  }

}
