package com.sap.prd.jenkins.plugins.pipeline_elasticsearch_logs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkinsci.plugins.workflow.actions.LabelAction;
import org.jenkinsci.plugins.workflow.actions.WorkspaceAction;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner.Executable;
import org.jenkinsci.plugins.workflow.graph.BlockStartNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.graph.StepNode;
import org.jenkinsci.plugins.workflow.log.BrokenLogStorage;
import org.jenkinsci.plugins.workflow.log.LogStorage;
import org.jenkinsci.plugins.workflow.log.LogStorageFactory;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.support.steps.ExecutorStep;
import org.jenkinsci.plugins.workflow.support.steps.StageStep;
import org.kohsuke.stapler.framework.io.ByteBuffer;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.console.AnnotatedLargeText;
import hudson.model.BuildListener;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.TaskListener;

@Extension
public class ElasticSearchLogStorageFactory implements LogStorageFactory
{

  private final static Logger LOGGER = Logger.getLogger(ElasticSearchLogStorageFactory.class.getName());

  @Override
  public LogStorage forBuild(FlowExecutionOwner owner)
  {
    ElasticSearchConfiguration config = ElasticSearchGlobalConfiguration.get().getElasticSearch();
    if (config == null)
    {
      return null;
    }

    final String fullName;
    final String buildId;
    try
    {
      Queue.Executable exec = owner.getExecutable();
      if (exec instanceof Run)
      {
        Run<?, ?> b = (Run<?, ?>)exec;
        // TODO escape [:*@%] in job names using %XX URL encoding
        fullName = b.getParent().getFullName();
        buildId = b.getId();
        return new ElasticSearchLogStorage(fullName, buildId, config.getSerializableConfiguration(), b);
      }
      else
      {
        return null;
      }
    }
    catch (IOException x)
    {
      return new BrokenLogStorage(x);
    }
  }

  static ElasticSearchLogStorageFactory get()
  {
    return ExtensionList.lookupSingleton(ElasticSearchLogStorageFactory.class);
  }

  private static class ElasticSearchLogStorage implements LogStorage
  {
    private final String fullName;
    private final String buildId;
    private ElasticSearchSerializableConfiguration config;
    private Run<?, ?> run;

    ElasticSearchLogStorage(String fullName, String buildId, ElasticSearchSerializableConfiguration config, Run<?, ?> run)
    {
      this.fullName = fullName;
      this.buildId = buildId;
      this.config = config;
      this.run = run;
    }

    @Override
    public BuildListener overallListener() throws IOException, InterruptedException
    {
      ElasticSearchSender sender = new ElasticSearchSender(fullName, buildId, null, null, null, null, config);
      sender.setRun(run);
      return sender;
    }

    @Override
    public TaskListener nodeListener(FlowNode node) throws IOException, InterruptedException
    {
      String stepName = null;
      String stageName = getStageName(node);
      String agentName = getAgentName(node);
      if (node instanceof StepNode)
      {
        StepDescriptor descriptor = ((StepNode)node).getDescriptor();
        if (descriptor != null)
        {
          stepName = descriptor.getFunctionName();
        }
      }
      LOGGER.log(Level.FINEST, "Node: {0}, Step: {1}, Stage: {2}, Agent: {3}", new Object[] { node.getId(), stepName, stageName, agentName });
      return new ElasticSearchSender(fullName, buildId, node.getId(), stepName, stageName, agentName, config);
    }

    private String getStageName(FlowNode node)
    {
      for (BlockStartNode bsn : node.getEnclosingBlocks())
      {
        if (bsn instanceof StepNode)
        {
          StepDescriptor descriptor = ((StepNode)bsn).getDescriptor();
          if (descriptor instanceof StageStep.DescriptorImpl)
          {
            LabelAction labelAction = bsn.getAction(LabelAction.class);
            if (labelAction != null)
            {
              return labelAction.getDisplayName();
            }
          }
        }
      }

      return null;
    }

    private String getAgentName(FlowNode node)
    {
      for (BlockStartNode bsn : node.getEnclosingBlocks())
      {
        if (bsn instanceof StepNode)
        {
          StepDescriptor descriptor = ((StepNode)bsn).getDescriptor();
          if (descriptor instanceof ExecutorStep.DescriptorImpl)
          {
            WorkspaceAction workspaceAction = bsn.getAction(WorkspaceAction.class);
            if (workspaceAction != null)
            {
              return workspaceAction.getNode();
            }
          }
        }
      }

      return null;
    }

    @Override
    public AnnotatedLargeText<Executable> overallLog(Executable build, boolean complete)
    {
      ByteBuffer buf = new ByteBuffer();
      return new AnnotatedLargeText<>(buf, StandardCharsets.UTF_8, true, build);
    }

    @Override
    public AnnotatedLargeText<FlowNode> stepLog(FlowNode node, boolean complete)
    {
      ByteBuffer buf = new ByteBuffer();
      return new AnnotatedLargeText<>(buf, StandardCharsets.UTF_8, true, node);
    }
  }
}
