package com.sap.prd.jenkins.plugins.pipeline_elasticsearch_logs;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionListener;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import hudson.Extension;
import hudson.model.Queue;

@Extension
public class ElasticSearchFlowExecutionListener extends FlowExecutionListener
{

  private static final Logger LOGGER = Logger.getLogger(ElasticSearchFlowExecutionListener.class.getName());

  @Override
  public void onCreated(FlowExecution execution)
  {

    try
    {
      ElasticSearchConfiguration config = ElasticSearchGlobalConfiguration.get().getElasticSearch();

      if (config == null)
      {
        return;
      }

      Queue.Executable exec = execution.getOwner().getExecutable();
      if (exec instanceof WorkflowRun)
      {
        ElasticSearchGraphListener graphListener = new ElasticSearchGraphListener((WorkflowRun) exec, config.getSerializableConfiguration());
        execution.addListener(graphListener);
      }
    }
    catch (IOException e)
    {
      LOGGER.log(Level.SEVERE, "Failed to get Executable of FlowExecution.");
    }
  }


}
