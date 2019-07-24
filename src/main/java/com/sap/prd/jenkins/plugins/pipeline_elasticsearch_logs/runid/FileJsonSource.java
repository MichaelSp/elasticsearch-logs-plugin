package com.sap.prd.jenkins.plugins.pipeline_elasticsearch_logs.runid;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.kohsuke.stapler.DataBoundConstructor;

import com.google.common.io.Files;

import hudson.Extension;
import hudson.model.Descriptor;

public class FileJsonSource extends JsonSource
{

  private String jsonFile;

  @DataBoundConstructor
  public FileJsonSource(String jsonFile)
  {
    this.jsonFile = jsonFile;
  }

  public String getJsonFile()
  {
    return jsonFile;
  }

  @Override
  public String getJson()
  {
    File file = new File(jsonFile);
    try
    {
      return Files.toString(file, StandardCharsets.UTF_8);
    }
    catch (IOException e)
    {
      //TODO: log message, maybe throw runtime exception
      return "{}";
    }
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
