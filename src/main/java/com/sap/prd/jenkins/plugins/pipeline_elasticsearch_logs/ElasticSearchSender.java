package com.sap.prd.jenkins.plugins.pipeline_elasticsearch_logs;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.CheckForNull;

import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
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

  public ElasticSearchSender(String fullName, String buildId, NodeInfo nodeInfo,
      ElasticSearchSerializableConfiguration config)
  {
    this.fullName = fullName;
    this.buildId = buildId;
    this.nodeInfo = nodeInfo;
    this.config = config;
  }

  public void setRun(WorkflowRun run)
  {
    this.run = run;
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
    data.put("timestampMillis",date.getTime());
    data.put("project", fullName);
    data.put("build", buildId);
    data.put("instance", config.getInstanceId());

    return data;
  }

  @Override
  public void close() throws IOException
  {

    // TODO: What happens if we have jenkins restart in between? Is the sender recreated or reloaded via CPS
    //       Maybe we should get the run by querying jenkins.

    // run is only set for the overall logger but not for the individual flow nodes
    if (run != null)
    {
      Map<String, Object> data = createData();
      Result result = run.getResult();
      if (result != null)
      {
        data.put("result", result.toString());
      }
      data.put("duration", run.getDuration());

      FlowExecution e = run.getExecution();
      Map<String, Map<String, Object>> nodes = new HashMap<>();
      FlowGraphTable t = new FlowGraphTable(e);
      t.build();
      for (Row r: t.getRows())
      {
        FlowNode n = r.getNode();
        Map<String, Object> nodeInfo = new HashMap<>();
        nodeInfo.put("id", n.getId());
        String stepName = ElasticSearchLogStorageFactory.getStepName(n);
        if (stepName != null)
        {
          nodeInfo.put("step", stepName);
        }
        if (n.getEnclosingId() != null)
        {
          nodeInfo.put("enclosingId", n.getEnclosingId());
        }
        nodeInfo.put("duration", r.getDurationMillis());
        nodes.put(n.getId(), nodeInfo);
      }
      data.put("nodes", nodes);
      getElasticSearchWriter().push(JSONObject.fromObject(data).toString());
    }
    if (logger != null)
    {
      logger = null;
      writer = null;
    }
  }

  private ElasticSearchWriter getElasticSearchWriter() throws IOException
  {
    if (writer == null)
    {
      URI uri = null;
      try
      {
        String scheme = "http";
        if (config.isSsl())
        {
          scheme = "https";
        }
        uri = new URI(scheme, null, config.getHost(), config.getPort(), config.getKey(), null, null);
      }
      catch (URISyntaxException e)
      {
        throw new IOException(e);
      }
      writer = new ElasticSearchWriter(uri, config.getUsername(), config.getPassword());
      if (config.isSsl())
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
      if (nodeInfo != null)
      {
        nodeInfo.appendNodeInfo(data);
      }
      getElasticSearchWriter().push(JSONObject.fromObject(data).toString());
    }
  }
}
