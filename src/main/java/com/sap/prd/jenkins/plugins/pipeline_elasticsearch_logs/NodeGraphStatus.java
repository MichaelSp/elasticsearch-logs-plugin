package com.sap.prd.jenkins.plugins.pipeline_elasticsearch_logs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.support.visualization.table.FlowGraphTable;
import org.jenkinsci.plugins.workflow.support.visualization.table.FlowGraphTable.Row;

public class NodeGraphStatus
{

  //private static final Logger LOGGER = Logger.getLogger(NodeGraphStatus.class.getName());
  
  private Map<String, RowStatus> rows;
  private final WorkflowRun run;

  public NodeGraphStatus(WorkflowRun run)
  {
    this.run = run;
    rows = Collections.synchronizedMap(new TreeMap<>());
  }

  public synchronized List<RowStatus> getUpdatedRows()
  {
    List<RowStatus> updatedRows = new ArrayList<>();
    FlowGraphTable flowGraphTable = new FlowGraphTable(run.getExecution());
    flowGraphTable.build();
    for (Row row : flowGraphTable.getRows())
    {
      String id = row.getNode().getId();
      RowStatus rowStatus = rows.get(id);
      if (rowStatus == null)
      {
        rowStatus = new RowStatus(row);
        rows.put(rowStatus.getNodeId(), rowStatus);
        updatedRows.add(rowStatus);
      }
      else
      {
        if (rowStatus.updateRow(row))
        {
          updatedRows.add(rowStatus);
        }
      }

    }
    return updatedRows;
  }

  public List<RowStatus> getRows()
  {
    getUpdatedRows();
    List<RowStatus> snapshot = new ArrayList<>();
    snapshot.addAll(rows.values());
    return snapshot;
  }
}
