package com.sap.prd.jenkins.plugins.pipeline_elasticsearch_logs.runid;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Set;
import java.util.logging.Logger;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.LogTaskListener;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class JsonRunIdProvider extends RunIdProvider
{
  
  private static final Logger LOGGER = Logger.getLogger(JsonRunIdProvider.class.getName());

  private JsonSource jsonSource;

  @DataBoundConstructor
  public JsonRunIdProvider(JsonSource jsonSource)
  {
    this.jsonSource = jsonSource;
  }
  
  public JsonSource getJsonSource()
  {
    return jsonSource;
  }

  @Override
  public JSONObject getRunId(Run<?, ?> run, String instanceId)
  {
    JSONObject jsonObject = jsonSource.getJsonObject();
    expand(jsonObject, getEnvOrEmpty(run, instanceId));
    return jsonObject;
  }

  private EnvVars getEnvOrEmpty(Run<?, ?> run, String instanceId) {
    try {
      EnvVars env = run.getEnvironment(new LogTaskListener(LOGGER, LOGGER.getLevel()));
      env.put("instanceId", instanceId);
      return env;
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
    return new EnvVars();
  }

  /**
   * Recursively expands all String values based on the provided EnvVars.
   * @param object instanceof JSONObject or JSONArray. Others are be ignored,
   * @param env
   */
  protected void expand(Object object, EnvVars env) {
    if(object instanceof JSONObject) {
      JSONObject jsonObject = ((JSONObject)object);
      Set keys = ((JSONObject)object).keySet();
      for(Object keyObject : keys) {
        if(!(keyObject instanceof String)) continue;
        String key = (String)keyObject;
        Object value = jsonObject.get(key);
        if(value instanceof String) {
          jsonObject.put(key, env.expand((String)value));
        } else {
          expand(value, env);
        }
      }
    } else if(object instanceof JSONArray) {
      JSONArray array = (JSONArray)object;
      for(int i = 0; i < array.size(); i++) {
        Object value = array.get(i);
        if(value instanceof String) {
          array.set(i, env.expand((String)value));
        } else {
          expand(value, env);
        }
      }
    }
  }

  @Extension
  @Symbol("json")
  public static class DescriptorImpl extends RunIdProviderDescriptor
  {
    @Override
    public String getDisplayName()
    {
      return "JSON File";
    }
  }

}

