package com.sap.prd.jenkins.plugins.pipeline_elasticsearch_logs;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import hudson.console.LineTransformationOutputStream;
import hudson.model.BuildListener;
import net.sf.json.JSONObject;

public class ElasticSearchSender implements BuildListener, Closeable
{
  private static final Logger LOGGER = Logger.getLogger(ElasticSearchSender.class.getName());

  static final DateTimeFormatter UTC_MILLIS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

  private static final long serialVersionUID = 1;

  private transient @CheckForNull PrintStream logger;
  protected final @CheckForNull NodeInfo nodeInfo;

  protected transient ElasticSearchWriter writer;
  protected final ElasticSearchRunConfiguration config;
  protected String eventPrefix;

  public ElasticSearchSender(@CheckForNull NodeInfo nodeInfo,
        @Nonnull ElasticSearchRunConfiguration config) throws IOException
  {
    this.nodeInfo = nodeInfo;
    this.config = config;
    if (nodeInfo != null)
    {
      eventPrefix = "node";
    }
    else
    {
      eventPrefix = "build";
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
    // TODO: What happens if we have jenkins restart in between? Is the sender recreated or reloaded via CPS
    //       Maybe we should get the run by querying jenkins.

    logger = null;
    writer = null;
  }

  private ElasticSearchWriter getElasticSearchWriter() throws IOException
  {
    if (writer == null)
    {
      writer = ElasticSearchWriter.createElasticSearchWriter(config);
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

      LOGGER.log(Level.FINEST, "Sending data: {0}", JSONObject.fromObject(data).toString());
      getElasticSearchWriter().push(JSONObject.fromObject(data).toString());
    }
  }
}
