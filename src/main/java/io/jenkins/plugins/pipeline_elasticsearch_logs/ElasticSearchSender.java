package io.jenkins.plugins.pipeline_elasticsearch_logs;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import hudson.console.LineTransformationOutputStream;
import hudson.model.BuildListener;
import hudson.model.Result;
import net.sf.json.JSONObject;

public class ElasticSearchSender implements BuildListener, Closeable
{
  private static final Logger LOGGER = Logger.getLogger(ElasticSearchSender.class.getName());

  private static final long serialVersionUID = 1;

  private transient @CheckForNull PrintStream logger;
  protected final @CheckForNull NodeInfo nodeInfo;

  protected transient ElasticSearchWriter writer;
  protected final ElasticSearchRunConfiguration config;
  protected transient @CheckForNull WorkflowRun run;
  protected String eventPrefix;
  protected transient final NodeGraphStatus nodeGraphStatus;

  public ElasticSearchSender(@CheckForNull NodeInfo nodeInfo, @Nonnull ElasticSearchRunConfiguration config,  @CheckForNull WorkflowRun run, @Nonnull NodeGraphStatus nodeGraphStatus) throws IOException
  {
    this.nodeInfo = nodeInfo;
    this.config = config;
    this.run = run;
    this.nodeGraphStatus = nodeGraphStatus;
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

  private void sendNodeUpdate(boolean isStart) throws IOException
  {
    Map<String, Object> data = config.createData();
    if (run != null)
    {
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
    }

    if (nodeInfo != null)
    {
      nodeInfo.appendNodeInfo(data);
    }

    if (isStart)
    {
      data.put("eventType", "flowGraph::" + eventPrefix + "Start");
    }
    else
    {
      data.put("eventType", "flowGraph::" + eventPrefix + "End");
    }

    List<Map<String, Object>> nodes = new ArrayList<>();

    List<RowStatus> rows;
    if (run == null)
    {
      rows = nodeGraphStatus.getUpdatedRows();
    }
    else
    {
      if (isStart)
      {
        rows = Collections.emptyList();
      }
      else
      {
        rows = nodeGraphStatus.getRows();
      }
    }
    for (RowStatus row : rows)
    {
      nodes.add(row.getData());
    }
    if (nodes.size() > 0)
    {
      data.put("nodes", nodes);
    }

    LOGGER.log(Level.FINEST, "Sending data: {0}", JSONObject.fromObject(data).toString());
    getElasticSearchWriter().push(JSONObject.fromObject(data).toString());
  }

  @Override
  public void close() throws IOException
  {
    // TODO: What happens if we have jenkins restart in between? Is the sender recreated or reloaded via CPS
    //       Maybe we should get the run by querying jenkins.

    try {
      sendNodeUpdate(false);
      logger = null;
      writer = null;
    }
    finally
    {
      if (run != null)
      {
        ElasticSearchLogStorageFactory.get().removeNodeGraphStatus(run);
      }
    }
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
      Map<String, Object> data = config.createData();

      ConsoleNotes.parse(b, len, data, config.isSaveAnnotations());
      data.put("eventType", eventPrefix + "Message");
      if (nodeInfo != null)
      {
        nodeInfo.appendNodeInfo(data);
      }

      LOGGER.log(Level.FINEST, "Sending data: {0}", JSONObject.fromObject(data).toString());
      getElasticSearchWriter().push(JSONObject.fromObject(data).toString());
    }
  }
}
