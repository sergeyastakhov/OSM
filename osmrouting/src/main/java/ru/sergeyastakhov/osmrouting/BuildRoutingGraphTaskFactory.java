/**
 * $Id$
 *
 * Copyright (C) 2012-2017 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmrouting;

import org.openstreetmap.osmosis.core.OsmosisRuntimeException;
import org.openstreetmap.osmosis.core.pipeline.common.TaskConfiguration;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManager;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.pipeline.v0_6.SinkSourceManager;

import java.util.Arrays;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class BuildRoutingGraphTaskFactory extends TaskManagerFactory
{
  private static final String ARG_GRAPH_LEVEL = "graphLevel";
  private static final String ARG_MINOR_GRAPHS_ACTION = "minorGraphsAction";

  private GraphLevel getGraphLevel(TaskConfiguration taskConfig)
  {
    if( !doesArgumentExist(taskConfig, ARG_GRAPH_LEVEL) )
      return GraphLevel.SERVICE;

    String graphLevelName = getStringArgument(taskConfig, ARG_GRAPH_LEVEL);

    try
    {
      return GraphLevel.valueOf(graphLevelName.toUpperCase());
    }
    catch( IllegalArgumentException e )
    {
      throw new OsmosisRuntimeException(
        "Argument " + ARG_GRAPH_LEVEL + " for task " + taskConfig.getId()
          + " must contain a valid graph level value."
          + " Supported values are: " + Arrays.toString(GraphLevel.values()), e);
    }
  }

  private MinorGraphsAction getMinorGraphsAction(TaskConfiguration taskConfig)
  {
    if( !doesArgumentExist(taskConfig, ARG_MINOR_GRAPHS_ACTION) )
      return MinorGraphsAction.MARK;

    String minorGraphsActionName = getStringArgument(taskConfig, ARG_MINOR_GRAPHS_ACTION);

    try
    {
      return MinorGraphsAction.valueOf(minorGraphsActionName.toUpperCase());
    }
    catch( IllegalArgumentException e )
    {
      throw new OsmosisRuntimeException(
        "Argument " + ARG_MINOR_GRAPHS_ACTION + " for task " + taskConfig.getId()
          + " must contain a valid action name."
          + " Supported values are: " + Arrays.toString(MinorGraphsAction.values()), e);
    }
  }

  @Override
  protected TaskManager createTaskManagerImpl(TaskConfiguration taskConfig)
  {
    GraphLevel graphLevel = getGraphLevel(taskConfig);
    MinorGraphsAction minorGraphsAction = getMinorGraphsAction(taskConfig);

    return new SinkSourceManager(
      taskConfig.getId(),
      new BuildRoutingGraphTask(graphLevel, minorGraphsAction),
      taskConfig.getPipeArgs()
    );
  }
}
