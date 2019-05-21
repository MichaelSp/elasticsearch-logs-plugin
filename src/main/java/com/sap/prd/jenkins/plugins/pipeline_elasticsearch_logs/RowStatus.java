package com.sap.prd.jenkins.plugins.pipeline_elasticsearch_logs;

import java.util.HashMap;
import java.util.Map;

import org.jenkinsci.plugins.workflow.actions.ErrorAction;
import org.jenkinsci.plugins.workflow.actions.WarningAction;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException;
import org.jenkinsci.plugins.workflow.support.visualization.table.FlowGraphTable.Row;

import hudson.model.Result;

public class RowStatus
{

  private final String nodeId;
  private final String stepName;
  private final String displayName;
  private final String enclosingId;
  private final long startTimeMillis;
  private String status = null;
  private long duration;
  private String errorMessage = null;

  public RowStatus(Row row)
  {
    FlowNode node = row.getNode();
    nodeId = node.getId();
    stepName = ElasticSearchLogStorageFactory.getStepName(node);
    displayName = node.getDisplayName();
    enclosingId = node.getEnclosingId();
    startTimeMillis = row.getStartTimeMillis();

    status = getStatus(node);
    duration = row.getDurationMillis();
    errorMessage = getErrorMessage(node);
  }

  public String getNodeId()
  {
    return nodeId;
  }

  private String getErrorMessage(FlowNode node)
  {
    String errorMessage = null;
    ErrorAction error = node.getError();
    if (error != null)
    {
      errorMessage = error.getError().getMessage();
    }
    return errorMessage;
  }

  public boolean updateRow(Row row)
  {
    FlowNode node = row.getNode();
    if (!node.getId().equals(nodeId))
    {
      throw new IllegalArgumentException("The given row represents a different node");
    }
    duration = row.getDurationMillis();
    String status = getStatus(node);

    boolean statusChange = !this.status.equals(status);
    errorMessage = getErrorMessage(node);

    this.status = status;
    return statusChange;
  }

  public Map<String, Object> getData()
  {
    Map<String, Object> nodeInfo = new HashMap<>();
    nodeInfo.put("id", nodeId);
    if (stepName != null)
    {
      nodeInfo.put("step", stepName);
    }
    if (enclosingId != null)
    {
      nodeInfo.put("enclosingId", enclosingId);
    }

    nodeInfo.put("displayName", displayName);
    nodeInfo.put("status", status);
    nodeInfo.put("duration", duration);
    nodeInfo.put("startTimeMillis", startTimeMillis);

    if (errorMessage != null)
    {
      nodeInfo.put("errorMessage", errorMessage);
    }

    return nodeInfo;
  }

  private String getStatus(FlowNode node)
  {
    if (node.isActive())
    {
      return "RUNNING";
    }

    ErrorAction error = node.getError();
    WarningAction warning = node.getPersistentAction(WarningAction.class);
    if (error != null)
    {
      if (error.getError() instanceof FlowInterruptedException)
      {
        return ((FlowInterruptedException) error.getError()).getResult().toString();
      }
      else
      {
        return Result.FAILURE.toString();
      }
    }
    else if (warning != null)
    {
      return warning.getResult().toString();
    }

    return Result.SUCCESS.toString();
  }

}
