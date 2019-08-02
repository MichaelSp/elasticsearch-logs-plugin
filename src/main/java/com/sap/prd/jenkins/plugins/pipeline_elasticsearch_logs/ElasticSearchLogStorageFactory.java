package com.sap.prd.jenkins.plugins.pipeline_elasticsearch_logs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import org.jenkinsci.plugins.uniqueid.IdStore;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner.Executable;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.log.BrokenLogStorage;
import org.jenkinsci.plugins.workflow.log.LogStorage;
import org.jenkinsci.plugins.workflow.log.LogStorageFactory;
import org.kohsuke.stapler.framework.io.ByteBuffer;

import com.sap.prd.jenkins.plugins.pipeline_elasticsearch_logs.runid.RunIdProvider;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.console.AnnotatedLargeText;
import hudson.model.BuildListener;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.TaskListener;
import net.sf.json.JSONObject;

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
      if (exec instanceof WorkflowRun)
      {
        WorkflowRun run = (WorkflowRun) exec;
        // TODO escape [:*@%] in job names using %XX URL encoding
        fullName = run.getParent().getFullName();
        buildId = run.getId();

        return new ElasticSearchLogStorage(fullName, buildId, config.getRunConfiguration(), run, config.getRunIdProvider());
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
  
  public static String getUniqueRunId(Run<?, ?> run)
  {
    String runId = IdStore.getId(run);
    if (runId == null)
    {
      IdStore.makeId(run);
      runId = IdStore.getId(run);
    }

    return runId;
  }

  static ElasticSearchLogStorageFactory get()
  {
    return ExtensionList.lookupSingleton(ElasticSearchLogStorageFactory.class);
  }

  private static class ElasticSearchLogStorage implements LogStorage
  {
    private final String fullName;
    private final String buildId;
    private ElasticSearchRunConfiguration config;
    private final JSONObject runId;
    
    ElasticSearchLogStorage(String fullName, String buildId, ElasticSearchRunConfiguration config,
          WorkflowRun run, RunIdProvider runIdProvider)
    {
      this.fullName = fullName;
      this.buildId = buildId;
      this.config = config;
      this.runId = runIdProvider.getRunId(run);
    }

    @Override
    public BuildListener overallListener() throws IOException, InterruptedException
    {
      ElasticSearchSender sender = new ElasticSearchSender(fullName, buildId, null, config, runId);
      return sender;
    }

    @Override
    public TaskListener nodeListener(FlowNode node) throws IOException, InterruptedException
    {
      NodeInfo nodeInfo = new NodeInfo(node);
      return new ElasticSearchSender(fullName, buildId, nodeInfo, config, runId);
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
