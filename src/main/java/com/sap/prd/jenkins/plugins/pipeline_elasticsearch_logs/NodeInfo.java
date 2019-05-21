package com.sap.prd.jenkins.plugins.pipeline_elasticsearch_logs;

import java.io.Serializable;
import java.util.Map;

import org.jenkinsci.plugins.workflow.actions.LabelAction;
import org.jenkinsci.plugins.workflow.actions.WorkspaceAction;
import org.jenkinsci.plugins.workflow.graph.BlockStartNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.graph.StepNode;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.support.steps.ExecutorStep;
import org.jenkinsci.plugins.workflow.support.steps.StageStep;

public class NodeInfo implements Serializable
{
  private static final long serialVersionUID = 1L;

  protected final String nodeId;
  protected final String stepName;
  protected final String stageName;
  protected final String stageId;
  protected final String agentName;
  
  public NodeInfo(FlowNode node)
  {
    
    FlowNode stage = getStage(node);
    String stageName = null;
    String stageId = null;

    if (stage != null)
    {
      stageId = stage.getId();
      LabelAction labelAction = stage.getAction(LabelAction.class);
      if (labelAction != null)
      {
        stageName = labelAction.getDisplayName();
      }
    }
    this.stepName = ElasticSearchLogStorageFactory.getStepName(node);
    this.agentName = getAgentName(node);
    this.nodeId = node.getId();
    this.stageName = stageName;
    this.stageId = stageId;
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
  
  private FlowNode getStage(FlowNode node)
  {
    for (BlockStartNode bsn : node.iterateEnclosingBlocks())
    {
      if (bsn instanceof StepNode)
      {
        StepDescriptor descriptor = ((StepNode)bsn).getDescriptor();
        if (descriptor instanceof StageStep.DescriptorImpl)
        {
          LabelAction labelAction = bsn.getAction(LabelAction.class);
          if (labelAction != null)
          {
            return bsn;
          }
        }
      }
    }

    return null;
  }

  private String getAgentName(FlowNode node)
  {
    for (BlockStartNode bsn : node.iterateEnclosingBlocks())
    {
      if (bsn instanceof StepNode)
      {
        StepDescriptor descriptor = ((StepNode)bsn).getDescriptor();
        if (descriptor instanceof ExecutorStep.DescriptorImpl)
        {
          WorkspaceAction workspaceAction = bsn.getAction(WorkspaceAction.class);
          if (workspaceAction != null)
          {
            return workspaceAction.getNode();
          }
        }
      }
    }

    return null;
  }

  @Override
  public String toString()
  {
    return String.format("Node: %s, Step: %s, Stage: %s (%s), Agent: %s", nodeId, stepName, stageName, stageId, agentName);
  }
  
  
}
