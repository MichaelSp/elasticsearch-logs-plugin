package com.sap.prd.jenkins.plugins.pipeline_elasticsearch_logs;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.Extension;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.listeners.RunListener;
import net.sf.json.JSONObject;

@Extension
public class ElasticSearchRunListener extends RunListener<Run<?, ?>>
{

  @Override
  public void onFinalized(Run<?, ?> run)
  {
    try
    {
      ElasticSearchConfiguration config = ElasticSearchGlobalConfiguration.get().getElasticSearch();
      if (config == null)
      {
        return;
      }

      ElasticSearchSerializableConfiguration config2 = config.getSerializableConfiguration();

      ElasticSearchWriter writer = ElasticSearchWriter.createElasticSearchWriter(config2);
      Map<String, Object> data = createData(run, config2);

      data.put("eventType", "buildEnd");
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
      writer.push(JSONObject.fromObject(data).toString());
    }
    catch (IOException e)
    {
      LOGGER.log(Level.SEVERE, "Failed to get Executable of FlowExecution.");
    }
  }

  private static final Logger LOGGER = Logger.getLogger(ElasticSearchRunListener.class.getName());

  @Override
  public void onInitialize(Run<?, ?> run)
  {
    ElasticSearchConfiguration config = ElasticSearchGlobalConfiguration.get().getElasticSearch();

    if (config == null)
    {
      return;
    }

    try
    {
      ElasticSearchSerializableConfiguration config2 = config.getSerializableConfiguration();

      ElasticSearchWriter writer = ElasticSearchWriter.createElasticSearchWriter(config2);
      Map<String, Object> data = createData(run, config2);

      data.put("eventType", "buildStart");
      writer.push(JSONObject.fromObject(data).toString());
    }
    catch (IOException e)
    {
      LOGGER.log(Level.SEVERE, "Failed to get Executable of FlowExecution.");
    }

  }

  private Map<String, Object> createData(Run<?, ?> run, ElasticSearchSerializableConfiguration config)
  {
    Map<String, Object> data = new LinkedHashMap<>();
    Date date = new Date();
    data.put("timestamp", ZonedDateTime.now(ZoneOffset.UTC).format(ElasticSearchSender.UTC_MILLIS));
    data.put("timestampMillis", date.getTime());
    data.put("project", run.getParent().getFullName());
    data.put("build", run.getId());
    data.put("instance", config.getInstanceId());

    return data;
  }

}
