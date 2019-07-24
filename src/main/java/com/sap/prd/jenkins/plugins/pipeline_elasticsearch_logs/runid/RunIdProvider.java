package com.sap.prd.jenkins.plugins.pipeline_elasticsearch_logs.runid;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Run;
import jenkins.model.Jenkins;

/**
 * Each run needs to be uniquely identifiable in Elastic search.
 * While in a classical Jenkins with UI it this can be achieved with  full project name, build number and instance id, there might be use cases when working with JenkinsFileRunner,
 * where additional information should be added or a simple guid is sufficient.
 * This extension point allows to provide different implementations how the runid looks like.
 *
 */
public abstract class RunIdProvider extends AbstractDescribableImpl<RunIdProvider> implements ExtensionPoint
{
  
  public abstract String getRunId(Run<?, ?> run, String instanceId);
  
  public static ExtensionList<RunIdProvider> all()
  {
    return Jenkins.get().getExtensionList(RunIdProvider.class);
  }

  public static abstract class RunIdProviderDescriptor extends Descriptor<RunIdProvider>
  {
    protected RunIdProviderDescriptor()
    {
      
    }
  }
}
