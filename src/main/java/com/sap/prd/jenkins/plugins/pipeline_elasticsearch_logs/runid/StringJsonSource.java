package com.sap.prd.jenkins.plugins.pipeline_elasticsearch_logs.runid;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.Descriptor;

public class StringJsonSource extends JsonSource
{
  private String jsonString;

  @DataBoundConstructor
  public StringJsonSource(String jsonString)
  {
    this.jsonString = jsonString;
  }

  public String getJsonString()
  {
    return jsonString;
  }

  @Override
  public String getJson()
  {
    return jsonString;
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
