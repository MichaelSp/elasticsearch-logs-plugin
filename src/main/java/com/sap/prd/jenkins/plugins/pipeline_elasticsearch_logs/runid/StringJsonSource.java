package com.sap.prd.jenkins.plugins.pipeline_elasticsearch_logs.runid;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.Descriptor;
import net.sf.json.JSONObject;

public class StringJsonSource extends JsonSource
{
  private JSONObject jsonObject;

  @DataBoundConstructor
  public StringJsonSource(String jsonString)
  {
    this.jsonObject = JSONObject.fromObject(jsonString);
  }

  public String getJsonString()
  {
    return jsonObject.toString();
  }

  @Override
  public JSONObject getJsonObject() {
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
