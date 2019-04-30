package com.sap.prd.jenkins.plugins.pipeline_elasticsearch_logs;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.CheckForNull;

import org.apache.commons.lang.time.FastDateFormat;

import hudson.console.ConsoleNote;
import hudson.console.LineTransformationOutputStream;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Run;
import net.sf.json.JSONObject;

public class ElasticSearchSender implements BuildListener, Closeable
{
  //private static final Logger LOGGER = Logger.getLogger(ElasticSearchSender.class.getName());

  private static final FastDateFormat TIME_FORMATTER = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

  private static final long serialVersionUID = 1;

  private transient @CheckForNull PrintStream logger;
  protected final String fullName;
  protected final String buildId;
  protected final String nodeId;
  protected final String stepName;
  protected final String stageName;
  protected final String agentName;
  
  protected transient ElasticSearchWriter writer;
  protected final ElasticSearchSerializableConfiguration config;
  protected transient Run<?, ?> run;

  public ElasticSearchSender(String fullName, String buildId, String nodeId, String stepName, String stageName, String agentName,
      ElasticSearchSerializableConfiguration config)
  {
    this.fullName = fullName;
    this.buildId = buildId;
    this.nodeId = nodeId;
    this.stepName = stepName;
    this.stageName = stageName;
    this.agentName = agentName;
    this.config = config;
  }

  public void setRun(Run<?, ?> run)
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

  @Override
  public void close() throws IOException
  {

    // TODO: What happens if we have jenkins restart in between? Is the sender recreated or reloaded via CPS
    //       Maybe we should get the run by querying jenkins.

    // run is only set for the overall logger but not for the individual flow nodes
    if (run != null)
    {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("project", fullName);
      data.put("build", buildId);
      data.put("timestamp", TIME_FORMATTER.format(new Date()));
      Result result = run.getResult();
      if (result != null)
      {
        data.put("result", result.toString());
      }
      data.put("duration", run.getDuration());
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
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("timestamp", TIME_FORMATTER.format(new Date()));

      String line = new String(b, 0, len, "UTF-8");
      line = ConsoleNote.removeNotes(line).trim();
      
      data.put("message", line);
      data.put("project", fullName);
      data.put("build", buildId);
      data.put("instance", config.getInstanceId());
      if (nodeId != null)
      {
        data.put("node", nodeId);
      }
      if (stepName != null)
      {
        data.put("step", stepName);
      }
      if (stageName != null)
      {
        data.put("stage", stageName);
      }
      if (agentName != null)
      {
        data.put("agent", agentName);
      }
      getElasticSearchWriter().push(JSONObject.fromObject(data).toString());
    }
  }
}
