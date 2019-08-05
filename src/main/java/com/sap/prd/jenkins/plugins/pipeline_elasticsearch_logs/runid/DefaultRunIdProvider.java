package com.sap.prd.jenkins.plugins.pipeline_elasticsearch_logs.runid;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.Run;
import net.sf.json.JSONObject;

/**
 * The default runid provider. It uses full project name, build number and instance id.
 *
 */
public class DefaultRunIdProvider extends RunIdProvider
{
  
  @DataBoundConstructor
  public DefaultRunIdProvider()
  {
  }

  @Override
  public JSONObject getRunId(Run<?, ?> run, String instanceId)
  {
    JSONObject data = new JSONObject();
    data.element("project", run.getParent().getFullName());
    data.element("build", run.getId());
    data.element("instance", instanceId);
    return data;
  }
  
  @Extension
  @Symbol("classic")
  public static class DescriptorImpl extends RunIdProviderDescriptor
  {

    @Override
    public String getDisplayName()
    {
      return "Default";
    }
    
  }

}


