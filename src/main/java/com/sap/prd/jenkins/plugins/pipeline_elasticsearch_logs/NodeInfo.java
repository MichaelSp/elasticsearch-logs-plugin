package com.sap.prd.jenkins.plugins.pipeline_elasticsearch_logs;

import java.io.Serializable;
import java.util.Map;

import hudson.Util;

public class NodeInfo implements Serializable
{
  private static final long serialVersionUID = 1L;

  protected final String nodeId;
  protected final String stepName;
  protected final String stageName;
  protected final String stageId;
  protected final String agentName;
  
  public NodeInfo(String nodeId, String stepName, String stageName, String stageId, String agentName)
  {
    this.nodeId = nodeId;
    this.stepName = stepName;
    this.stageName = stageName;
    this.stageId = stageId;
    this.agentName = Util.fixEmptyAndTrim(agentName);
  }

  public void appendNodeInfo(Map<String, Object> data)
  {
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
      data.put("stageId", stageId);
    }
    if (stageId != null)
    {
      data.put("stage", stageName);
    }
    if (agentName != null)
    {
      data.put("agent", agentName);
    }
  }
}
