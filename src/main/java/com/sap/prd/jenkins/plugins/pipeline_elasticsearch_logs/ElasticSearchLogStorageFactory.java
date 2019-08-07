package com.sap.prd.jenkins.plugins.pipeline_elasticsearch_logs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkinsci.plugins.uniqueid.IdStore;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner.Executable;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.graph.StepNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.log.BrokenLogStorage;
import org.jenkinsci.plugins.workflow.log.LogStorage;
import org.jenkinsci.plugins.workflow.log.LogStorageFactory;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.kohsuke.stapler.framework.io.ByteBuffer;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.console.AnnotatedLargeText;
import hudson.model.BuildListener;
import hudson.model.Queue;
import hudson.model.TaskListener;

@Extension
public class ElasticSearchLogStorageFactory implements LogStorageFactory
{

  private final static Logger LOGGER = Logger.getLogger(ElasticSearchLogStorageFactory.class.getName());

  private transient Map<String, NodeGraphStatus> nodeGraphs = new HashMap<>();

  @Override
  public LogStorage forBuild(FlowExecutionOwner owner)
  {
    ElasticSearchConfiguration config = ElasticSearchGlobalConfiguration.get().getElasticSearch();
    if (config == null)
    {
      return null;
    }

    try
    {
      Queue.Executable exec = owner.getExecutable();
      if (exec instanceof WorkflowRun)
      {
        WorkflowRun run = (WorkflowRun) exec;
        NodeGraphStatus nodeGraphStatus = null;
        if (run.isBuilding())
        {
          String runId  = ElasticSearchConfiguration.getUniqueRunId(run);
          LOGGER.log(Level.FINE, "Getting NodeGraphStatus for RunID: {0}", runId);
          nodeGraphStatus = nodeGraphs.get(runId);
          if (nodeGraphStatus == null)
          {
            LOGGER.log(Level.FINE, "Creating NodeGraphStatus for RunID: {0}", runId);
            nodeGraphStatus = new NodeGraphStatus(run);
            nodeGraphs.put(runId, nodeGraphStatus);
          }
        }

        LOGGER.log(Level.INFO, "Getting LogStorage for: {0}", run.getFullDisplayName());
        return new ElasticSearchLogStorage(config.getRunConfiguration(run), run, nodeGraphStatus);        
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
  
  public void removeNodeGraphStatus(WorkflowRun run)
  {
    String runId  = IdStore.getId(run);
    LOGGER.log(Level.FINE, "Removing NodeGraphStatus for RunID: {0}", runId);
    nodeGraphs.remove(runId);
  }

  static String getStepName(FlowNode node)
  {
    String stepName = null;
    if (node instanceof StepNode)
    {
      StepDescriptor descriptor = ((StepNode) node).getDescriptor();
      if (descriptor != null)
      {
        stepName = descriptor.getFunctionName();
      }
    }
    return stepName;
  }

  static ElasticSearchLogStorageFactory get()
  {
    return ExtensionList.lookupSingleton(ElasticSearchLogStorageFactory.class);
  }

  private static class ElasticSearchLogStorage implements LogStorage
  {
    private ElasticSearchRunConfiguration config;
    private WorkflowRun run;
    private final NodeGraphStatus nodeGraphStatus;
    
    ElasticSearchLogStorage(ElasticSearchRunConfiguration config, WorkflowRun run, NodeGraphStatus nodeGraphStatus)
    {
      this.config = config;
      this.run = run;
      this.nodeGraphStatus = nodeGraphStatus;
    }
    
    @Override
    public BuildListener overallListener() throws IOException, InterruptedException
    {
      ElasticSearchSender sender = new ElasticSearchSender(null, config, run, nodeGraphStatus);
      return sender;
    }

    @Override
    public TaskListener nodeListener(FlowNode node) throws IOException, InterruptedException
    {
      NodeInfo nodeInfo = new NodeInfo(node);
      return new ElasticSearchSender(nodeInfo, config, null, nodeGraphStatus);
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
