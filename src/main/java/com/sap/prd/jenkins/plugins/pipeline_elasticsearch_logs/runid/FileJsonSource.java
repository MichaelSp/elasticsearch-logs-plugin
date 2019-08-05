package com.sap.prd.jenkins.plugins.pipeline_elasticsearch_logs.runid;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.kohsuke.stapler.DataBoundConstructor;

import com.google.common.io.Files;

import hudson.Extension;
import hudson.model.Descriptor;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

public class FileJsonSource extends JsonSource
{

  private String jsonFile;
  private JSONObject jsonObject;

  @DataBoundConstructor
  public FileJsonSource(String jsonFile) throws IOException, JSONException
  {
    this.jsonFile = jsonFile;
    File file = new File(jsonFile);
    String jsonString = Files.toString(file, StandardCharsets.UTF_8);
    this.jsonObject = JSONObject.fromObject(jsonString);
  }

  public String getJsonFile()
  {
    return jsonFile;
  }

  @Override
  public String getJsonString()
  {
      return jsonObject.toString();
  }

  @Override
  public JSONObject getJsonObject() {
      return jsonObject;
  }
  
  @Extension
  public static class DescriptorImpl extends Descriptor<JsonSource>
  {

    @Override
    public String getDisplayName()
    {
      return "JSON file";
    }
  }

}
