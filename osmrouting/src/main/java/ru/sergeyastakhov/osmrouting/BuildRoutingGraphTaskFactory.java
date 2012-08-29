/**
 * $Id$
 *
 * Copyright (C) 2012 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmrouting;

import java.util.Arrays;

import org.openstreetmap.osmosis.core.OsmosisRuntimeException;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerType;
import org.openstreetmap.osmosis.core.pipeline.common.TaskConfiguration;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManager;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.pipeline.v0_6.SinkSourceManager;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class BuildRoutingGraphTaskFactory extends TaskManagerFactory
{
  private static final String ARG_ID_TRACKER_TYPE = "idTrackerType";
  private static final IdTrackerType DEFAULT_ID_TRACKER_TYPE = IdTrackerType.IdList;

  private static final String ARG_GRAPH_LEVEL = "graphLevel";

  protected IdTrackerType getIdTrackerType(
    TaskConfiguration taskConfig)
  {
    if( doesArgumentExist(taskConfig, ARG_ID_TRACKER_TYPE) )
    {
      String idTrackerType = getStringArgument(taskConfig, ARG_ID_TRACKER_TYPE);

      try
      {
        return IdTrackerType.valueOf(idTrackerType);
      }
      catch( IllegalArgumentException e )
      {
        throw new OsmosisRuntimeException(
          "Argument " + ARG_ID_TRACKER_TYPE + " for task " + taskConfig.getId()
            + " must contain a valid id tracker type.", e);
      }

    }
    else
    {
      return DEFAULT_ID_TRACKER_TYPE;
    }
  }

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

  @Override
  protected TaskManager createTaskManagerImpl(TaskConfiguration taskConfig)
  {
    IdTrackerType idTrackerType = getIdTrackerType(taskConfig);
    GraphLevel graphLevel = getGraphLevel(taskConfig);

    return new SinkSourceManager(
      taskConfig.getId(),
      new BuildRoutingGraphTask(idTrackerType, graphLevel),
      taskConfig.getPipeArgs()
    );
  }
}
