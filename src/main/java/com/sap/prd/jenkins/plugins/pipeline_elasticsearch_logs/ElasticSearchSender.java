package com.sap.prd.jenkins.plugins.pipeline_elasticsearch_logs;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;

import hudson.console.ConsoleNote;
import hudson.console.LineTransformationOutputStream;
import hudson.model.BuildListener;
import hudson.model.Run;
import net.sf.json.JSONObject;

public class ElasticSearchSender implements BuildListener, Closeable
{
  private static final Logger LOGGER = Logger.getLogger(ElasticSearchSender.class.getName());

  private static final long serialVersionUID = 1;

  private transient @CheckForNull PrintStream logger;
  protected final String fullName;
  protected final String buildId;
  protected final String nodeId;
  protected final String stepName;
  protected final String stageName;
  protected transient Run<?, ?> run;

  protected ElasticSearchSender(String fullName, String buildId, String nodeId, String stepName, String stageName)
  {
    this.fullName = fullName;
    this.buildId = buildId;
    this.nodeId = nodeId;
    this.stepName = stepName;
    this.stageName = stageName;
  }

  void setRun(Run<?, ?> run)
  {
    this.run = run;
  }

  static final class MasterSender extends ElasticSearchSender
  {
    private static final long serialVersionUID = 1;

    MasterSender(String fullName, String buildId, String nodeId, String stepName, String stageName)
    {
      super(fullName, buildId, nodeId, stepName, stageName);
    }

    private Object writeReplace() throws IOException
    {
      return new AgentSender(fullName, buildId, nodeId, stepName, stageName);
    }
  }

  static final class AgentSender extends ElasticSearchSender
  {
    private static final long serialVersionUID = 1;

    protected AgentSender(String fullName, String buildId, String nodeId, String stepName, String stageName)
    {
      super(fullName, buildId, nodeId, stepName, stageName);
    }
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
    if (logger != null)
    {
      logger = null;
    }
  }

  private class ElasticSearchOutputStream extends LineTransformationOutputStream
  {
    @Override
    protected void eol(byte[] b, int len) throws IOException
    {
      String line = new String(b, 0, len, "UTF-8");
      line = ConsoleNote.removeNotes(line).trim();
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("message", line);
      data.put("project", fullName);
      data.put("build", buildId);
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
      LOGGER.log(Level.INFO, "output: {0}", JSONObject.fromObject(data).toString());
    }
  }
}
