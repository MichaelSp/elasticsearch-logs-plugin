package com.sap.prd.jenkins.plugins.pipeline_elasticsearch_logs.runid;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.Run;
import net.sf.json.JSONObject;

public class JsonRunIdProvider extends RunIdProvider
{
  
  private JsonSource jsonSource;

  @DataBoundConstructor
  public JsonRunIdProvider(JsonSource jsonSource)
  {
    this.jsonSource = jsonSource;
  }
  
  public JsonSource getJsonSource()
  {
    return jsonSource;
  }

  @Override
  public JSONObject getRunId(Run<?, ?> run, String instanceId)
  {
    return jsonSource.getJsonObject();
  }

  @Extension
  @Symbol("json")
  public static class DescriptorImpl extends RunIdProviderDescriptor
  {
    @Override
    public String getDisplayName()
    {
      return "JSON File";
    }
  }

}

