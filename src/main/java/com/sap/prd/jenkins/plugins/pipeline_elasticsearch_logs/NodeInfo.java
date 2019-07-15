package com.sap.prd.jenkins.plugins.pipeline_elasticsearch_logs;

import java.io.Serializable;
import java.util.Map;

import org.jenkinsci.plugins.workflow.actions.LabelAction;
import org.jenkinsci.plugins.workflow.actions.ThreadNameAction;
import org.jenkinsci.plugins.workflow.actions.WorkspaceAction;
import org.jenkinsci.plugins.workflow.cps.steps.ParallelStep;
import org.jenkinsci.plugins.workflow.graph.BlockStartNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.graph.StepNode;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.support.steps.ExecutorStep;
import org.jenkinsci.plugins.workflow.support.steps.StageStep;

/**
 * Information about a FlowNode to be sent with message events.
 *
 */
public class NodeInfo implements Serializable
{
  private static final long serialVersionUID = 1L;

  protected final String nodeId;
  protected final String stepName;
  protected final String stageName;
  protected final String stageId;
  protected final String parallelBranchName;
  protected final String parallelBranchId;
  protected final String agentName;
  protected final String displayName;

  public NodeInfo(FlowNode node)
  {

    FlowNode stage = getStage(node);
    FlowNode parallelBranch = getParallelBranch(node);
    String stageName = null;
    String stageId = null;
    String parallelBranchName = null;
    String parallelBranchId = null;
    
    if (stage != null)
    {
      stageId = stage.getId();
      LabelAction labelAction = stage.getAction(LabelAction.class);
      if (labelAction != null)
      {
        stageName = labelAction.getDisplayName();
      }
    }

    if (parallelBranch != null)
    {
      parallelBranchId = parallelBranch.getId();
      ThreadNameAction labelAction = parallelBranch.getAction(ThreadNameAction.class);
      if (labelAction != null)
      {
        parallelBranchName = labelAction.getThreadName();
      }
    }

    this.stepName = ElasticSearchLogStorageFactory.getStepName(node);
    this.agentName = getAgentName(node);
    this.nodeId = node.getId();
    this.stageName = stageName;
    this.stageId = stageId;
    this.parallelBranchName = parallelBranchName;
    this.parallelBranchId = parallelBranchId;
    this.displayName = node.getDisplayName();

  }

  /**
   * Appends this nodes info to the given map.
   * 
   * @param data
   *          The map to receive the node info
   */
  public void appendNodeInfo(Map<String, Object> data)
  {
    data.put("flowNodeId", nodeId);
    if (stepName != null)
    {
      data.put("step", stepName);
    }
    if (stageName != null)
    {
      data.put("stageName", stageName);
    }
    if (stageId != null)
    {
      data.put("stageId", stageId);
    }
    if (parallelBranchName != null)
    {
      data.put("parallelBranchName", parallelBranchName);
    }
    if (parallelBranchId != null)
    {
      data.put("parallelBranchId", parallelBranchId);
    }
    if (agentName != null)
    {
      data.put("agent", agentName);
    }
    if (displayName != null)
    {
      data.put("displayName", displayName);
    }
  }

  /**
   * Returns the FlowNode of a stage step that encloses the given FlowNode.
   * 
   * @param node
   *          The FlowNode to check.
   * @return The BlockStartNode representing the start of the stage step or null if not inside a
   *         stage.
   */
  private FlowNode getStage(FlowNode node)
  {
    for (BlockStartNode bsn : node.iterateEnclosingBlocks())
    {
      if (bsn instanceof StepNode)
      {
        StepDescriptor descriptor = ((StepNode) bsn).getDescriptor();
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

  /**
   * Returns the FlowNode of a parallel branch step that encloses the given FlowNode.
   * 
   * @param node
   *          The FlowNode to check.
   * @return The BlockStartNode representing the start of the parallel branch step or null if not
   *         inside a parallel branch.
   */
  private FlowNode getParallelBranch(FlowNode node)
  {
    for (BlockStartNode bsn : node.iterateEnclosingBlocks())
    {
      if (bsn instanceof StepNode)
      {
        StepDescriptor descriptor = ((StepNode) bsn).getDescriptor();
        if (descriptor instanceof ParallelStep.DescriptorImpl)
        {
          ThreadNameAction threadNameAction = bsn.getAction(ThreadNameAction.class);
          if (threadNameAction != null)
          {
            return bsn;
          }
        }
      }
    }

    return null;
  }

  /**
   * Returns the name of the agent this FlowNode is running on.
   * 
   * @param node
   *          The FlowNode to check
   * @return The name of the agent or null if not running on an agent.
   */
  private String getAgentName(FlowNode node)
  {
    for (BlockStartNode bsn : node.iterateEnclosingBlocks())
    {
      if (bsn instanceof StepNode)
      {
        StepDescriptor descriptor = ((StepNode) bsn).getDescriptor();
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
    return String.format("Node: %s, Step: %s, Stage: %s (%s), Agent: %s", nodeId, stepName, stageName, stageId,
          agentName);
  }

}
