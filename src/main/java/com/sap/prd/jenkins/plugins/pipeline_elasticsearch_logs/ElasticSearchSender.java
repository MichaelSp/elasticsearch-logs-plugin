package com.sap.prd.jenkins.plugins.pipeline_elasticsearch_logs;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.jenkinsci.plugins.workflow.actions.ErrorAction;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException;
import org.jenkinsci.plugins.workflow.support.visualization.table.FlowGraphTable;
import org.jenkinsci.plugins.workflow.support.visualization.table.FlowGraphTable.Row;

import hudson.console.ConsoleNote;
import hudson.console.LineTransformationOutputStream;
import hudson.model.BuildListener;
import hudson.model.Result;
import net.sf.json.JSONObject;

public class ElasticSearchSender implements BuildListener, Closeable
{
  //private static final Logger LOGGER = Logger.getLogger(ElasticSearchSender.class.getName());

  private static final DateTimeFormatter UTC_MILLIS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

  private static final long serialVersionUID = 1;

  private transient @CheckForNull PrintStream logger;
  protected final String fullName;
  protected final String buildId;
  protected final NodeInfo nodeInfo;

  protected transient ElasticSearchWriter writer;
  protected final ElasticSearchSerializableConfiguration config;
  protected transient WorkflowRun run;
  protected String eventPrefix;


  public ElasticSearchSender(@Nonnull String fullName, @Nonnull String buildId, @CheckForNull NodeInfo nodeInfo,
        @Nonnull ElasticSearchSerializableConfiguration config, @Nonnull WorkflowRun run) throws IOException
  {
    this.fullName = fullName;
    this.buildId = buildId;
    this.nodeInfo = nodeInfo;
    this.config = config;
    this.run = run;
    if (nodeInfo != null)
    {
      eventPrefix = "node";
    }
    else
    {
      eventPrefix = "build";
    }
    sendNodeUpdate(true);
  }

  @Override
  public PrintStream getLogger()
  {
    if (logger == null)
    {
      try
      {
        logger = new PrintStream(new ElasticSearchOutputStream(), false, "UTF-8");
      }
      catch (UnsupportedEncodingException x)
      {
        throw new AssertionError(x);
      }
    }
    return logger;
  }

  private Map<String, Object> createData()
  {
    Map<String, Object> data = new LinkedHashMap<>();
    Date date = new Date();
    data.put("timestamp", ZonedDateTime.now(ZoneOffset.UTC).format(UTC_MILLIS));
    data.put("timestampMillis", date.getTime());
    data.put("project", fullName);
    data.put("build", buildId);
    data.put("instance", config.getInstanceId());

    return data;
  }

  public void sendNodeUpdate(boolean isStart) throws IOException
  {
    Map<String, Object> data = createData();
    Result result = run.getResult();
    if (result != null)
    {
      data.put("result", result.toString());
    }
    long duration = run.getDuration();
    if (duration > 0)
    {
      data.put("duration", run.getDuration());
    }

    if (nodeInfo != null)
    {
      nodeInfo.appendNodeInfo(data);
    }
    
    if (isStart)
    {
      data.put("eventType", eventPrefix + "Start");
    }
    else
    {
      data.put("eventType", eventPrefix + "End");
    }
    
    Map<String, Map<String, Object>> nodes = new HashMap<>();
    FlowGraphTable flowGraphTable = new FlowGraphTable(run.getExecution());
    flowGraphTable.build();
    
    for (Row row : flowGraphTable.getRows())
    {
      FlowNode node = row.getNode();
      Map<String, Object> nodeInfo = new HashMap<>();
      nodeInfo.put("id", node.getId());
      String stepName = ElasticSearchLogStorageFactory.getStepName(node);
      if (stepName != null)
      {
        nodeInfo.put("step", stepName);
      }
      if (node.getEnclosingId() != null)
      {
        nodeInfo.put("enclosingId", node.getEnclosingId());
      }
      nodeInfo.put("displayName", node.getDisplayName());
      nodeInfo.put("status", getStatus(node));
      nodeInfo.put("duration", row.getDurationMillis());
      ErrorAction error = node.getError();
      if (error != null)
      {
        Throwable t = error.getError();
        if (t != null)
        {
          nodeInfo.put("errorMessage", t.getMessage());
        }
      }
      nodes.put(node.getId(), nodeInfo);
    }
    data.put("nodes", nodes);
    getElasticSearchWriter().push(JSONObject.fromObject(data).toString());
  }

  @Override
  public void close() throws IOException
  {

    // TODO: What happens if we have jenkins restart in between? Is the sender recreated or reloaded via CPS
    //       Maybe we should get the run by querying jenkins.

    sendNodeUpdate(false);
    if (logger != null)
    {
      logger = null;
      writer = null;
    }
  }

  private String getStatus(FlowNode node)
  {
    ErrorAction error = node.getError();
    if (error != null)
    {
      if (error.getError() instanceof FlowInterruptedException)
      {
        return "ABORTED";
      }
      else
      {
        return "FAILURE";
      }
    }
    else
    {
      if (node.isActive())
      {
        return "RUNNING";
      }
    }

    return "SUCCESS";
  }

  private ElasticSearchWriter getElasticSearchWriter() throws IOException
  {
    if (writer == null)
    {
      writer = new ElasticSearchWriter(config.getUri(), config.getUsername(), config.getPassword());
      if (config.getTrustKeyStore() != null)
      {
        writer.setTrustKeyStore(config.getTrustKeyStore());
      }
    }
    return writer;
  }

  private class ElasticSearchOutputStream extends LineTransformationOutputStream
  {
    @Override
    protected void eol(byte[] b, int len) throws IOException
    {
      Map<String, Object> data = createData();

      String line = new String(b, 0, len, StandardCharsets.UTF_8);
      line = ConsoleNote.removeNotes(line).trim();

      data.put("message", line);
      data.put("eventType", "message");
      if (nodeInfo != null)
      {
        nodeInfo.appendNodeInfo(data);
      }
      getElasticSearchWriter().push(JSONObject.fromObject(data).toString());
    }
  }
}
