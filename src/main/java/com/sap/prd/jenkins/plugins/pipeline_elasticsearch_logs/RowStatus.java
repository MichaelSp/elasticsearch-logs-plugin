package com.sap.prd.jenkins.plugins.pipeline_elasticsearch_logs;

import java.util.HashMap;
import java.util.Map;

import org.jenkinsci.plugins.workflow.actions.ErrorAction;
import org.jenkinsci.plugins.workflow.actions.LabelAction;
import org.jenkinsci.plugins.workflow.actions.ThreadNameAction;
import org.jenkinsci.plugins.workflow.actions.WarningAction;
import org.jenkinsci.plugins.workflow.cps.steps.ParallelStep;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.graph.StepNode;
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.support.steps.StageStep;
import org.jenkinsci.plugins.workflow.support.visualization.table.FlowGraphTable.Row;

import hudson.model.Result;

/**
 * Helper class that holds the status of row from the FlowGraphTable so we can check if the status has changed.
 *
 */
public class RowStatus implements Comparable<RowStatus>
{

  private final String nodeId;
  private final Integer id;
  private final String stepName;
  private final String displayName;
  private final String enclosingId;
  private final long startTimeMillis;
  private final String stageName;
  private final String parallelBranchName;
  private String status = null;
  private long duration;
  private String errorMessage = null;

  public RowStatus(Row row)
  {
    FlowNode node = row.getNode();
    nodeId = node.getId();
    id = Integer.parseInt(nodeId);
    stepName = ElasticSearchLogStorageFactory.getStepName(node);
    displayName = node.getDisplayName();
    enclosingId = node.getEnclosingId();
    startTimeMillis = row.getStartTimeMillis();

    status = getStatus(node);
    duration = row.getDurationMillis();
    errorMessage = getErrorMessage(node);
    stageName = getStageName(node);
    parallelBranchName = getParallelBranchName(node);
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
  
  private String getStageName(FlowNode node)
  {
    if (node instanceof StepNode)
    {
      StepDescriptor descriptor = ((StepNode)node).getDescriptor();
      if (descriptor instanceof StageStep.DescriptorImpl)
      {
        LabelAction labelAction = node.getAction(LabelAction.class);
        if (labelAction != null)
        {
          return labelAction.getDisplayName();
        }
      }
    }
    return null;
  }

  private String getParallelBranchName(FlowNode node)
  {
    if (node instanceof StepNode)
    {
      StepDescriptor descriptor = ((StepNode)node).getDescriptor();
      if (descriptor instanceof ParallelStep.DescriptorImpl)
      {
        ThreadNameAction threadNameAction = node.getAction(ThreadNameAction.class);
        if (threadNameAction != null)
        {
          return threadNameAction.getThreadName();
        }
      }
    }
    return null;
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

    if (stageName != null)
    {
      nodeInfo.put("stageName", stageName);
    }

    if (parallelBranchName != null)
    {
      nodeInfo.put("parallelBranchName", parallelBranchName);
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

  @Override
  public int compareTo(RowStatus o)
  {
    return id.compareTo(o.id);
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj == null)
    {
      return false;
    }
    if (!(obj instanceof RowStatus))
    {
      return false;
    }
    RowStatus other = (RowStatus) obj;
    return id.equals(other.id);
  }

}
