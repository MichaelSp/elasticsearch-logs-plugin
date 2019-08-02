package com.sap.prd.jenkins.plugins.pipeline_elasticsearch_logs;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkinsci.plugins.workflow.actions.ErrorAction;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.actions.WarningAction;
import org.jenkinsci.plugins.workflow.flow.GraphListener;
import org.jenkinsci.plugins.workflow.graph.AtomNode;
import org.jenkinsci.plugins.workflow.graph.BlockEndNode;
import org.jenkinsci.plugins.workflow.graph.BlockStartNode;
import org.jenkinsci.plugins.workflow.graph.FlowEndNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.graph.FlowStartNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException;

import hudson.model.Result;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class ElasticSearchGraphListener implements GraphListener.Synchronous
{
  private static final Logger LOGGER = Logger.getLogger(ElasticSearchGraphListener.class.getName());

  private final ElasticSearchWriter writer;
  private final String instanceId;
  protected final String fullName;
  protected final String buildId;

  public ElasticSearchGraphListener(WorkflowRun run, ElasticSearchSerializableConfiguration config) throws IOException
  {
    writer = ElasticSearchWriter.createElasticSearchWriter(config);
    instanceId = config.getInstanceId();
    fullName = run.getParent().getFullName();
    buildId = run.getId();
    LOGGER.log(Level.INFO, "Initializing Graphlistener for: {0}", run.getDisplayName());
  }

  @Override
  public void onNewHead(FlowNode node)
  {
    LOGGER.log(Level.INFO, node.getId() + ":" + node.getDisplayName());

    try
    {
      for (FlowNode parent : node.getParents())
      {
        if (parent instanceof AtomNode)
        {
          sendAtomNodeEnd(parent, node);
        }
        else if (node instanceof BlockEndNode)
        {
          sendNodeEnd(parent);
        }
      }
      
      if (node instanceof AtomNode || node instanceof BlockStartNode)
      {
        sendNodeStart(node);
      }
      if (node instanceof FlowEndNode)
      {
        sendNodeEnd(node);
      }
    }
    catch (IOException e)
    {
      LOGGER.log(Level.SEVERE, "Failed to push data to Elastic Search", e);
    }
  }

  private String getEventType(FlowNode node)
  {
    if (node instanceof AtomNode)
    {
      return "flowGraph::atomNodeStart";
    }
    if (node instanceof FlowStartNode)
    {
      return "flowGraph::flowStart";
    }
    if (node instanceof FlowEndNode)
    {
      return "flowGraph::flowEnd";
    }
    if (node instanceof BlockStartNode)
    {
      return "flowGraph::nodeStart";
    }
    if (node instanceof BlockEndNode)
    {
      return "flowGraph::nodeEnd";
    }
    
    return "unknown";
  }
  
  private void sendAtomNodeEnd(FlowNode node, FlowNode successor) throws IOException
  {
    Map<String, Object> data = createData(node);
    data.put("eventType", "flowGraph::atomNodeEnd");
    data.put("result", getStatus(node));
    data.put("duration", getDuration(node, successor));
    String errorMessage = getErrorMessage(node);
    if (errorMessage != null)
    {
      data.put("errorMessage", errorMessage);
    }

    writer.push(JSONObject.fromObject(data).toString());
  }

  private void sendNodeEnd(FlowNode node) throws IOException
  {
    Map<String, Object> data = createData(node);
    data.put("eventType", getEventType(node));
    FlowNode startNode = ((BlockEndNode<?>) node).getStartNode();
    data.put("startId", startNode.getId());

    data.put("result", getStatus(node));
    data.put("duration", getDuration(startNode, node));

    String errorMessage = getErrorMessage(node);
    if (errorMessage != null)
    {
      data.put("errorMessage", errorMessage);
    }

    writer.push(JSONObject.fromObject(data).toString());
  }

  private void sendNodeStart(FlowNode node) throws IOException
  {
    Map<String, Object> data = createData(node);

    data.put("eventType", getEventType(node));
    writer.push(JSONObject.fromObject(data).toString());
  }
  
  private long getDuration(FlowNode startNode, FlowNode endNode)
  {
      return TimingAction.getStartTime(endNode) -  TimingAction.getStartTime(startNode); 
  }

  private Map<String, Object> createData(FlowNode node) throws IOException
  {
    Map<String, Object> data = new LinkedHashMap<>();
    Date date = new Date();
    data.put("timestamp", ZonedDateTime.now(ZoneOffset.UTC).format(ElasticSearchSender.UTC_MILLIS));
    data.put("timestampMillis", date.getTime());
    data.put("project", fullName);
    data.put("build", buildId);
    data.put("instance", instanceId);
    List<FlowNode> predecessors = node.getParents();
    if (predecessors.size() > 0)
    {
      JSONArray p = new JSONArray();
      for (FlowNode parent: predecessors)
      {
        p.add(parent.getId());
      }
      data.put("predecessors", p);
    }
    NodeInfo nodeInfo = new NodeInfo(node);
    nodeInfo.appendNodeInfo(data);

    return data;
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
  
  private String getStatus(FlowNode node)
  {
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
