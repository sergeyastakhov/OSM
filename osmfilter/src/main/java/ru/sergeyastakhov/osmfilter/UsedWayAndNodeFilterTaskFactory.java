/**
 * $Id$
 *
 * Copyright (C) 2010 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmfilter;

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
public class UsedWayAndNodeFilterTaskFactory extends TaskManagerFactory
{
  private static final String ARG_ID_TRACKER_TYPE = "idTrackerType";
  private static final IdTrackerType DEFAULT_ID_TRACKER_TYPE = IdTrackerType.Dynamic;

  private static final String ARG_USED_BY_TAG = "usedByTag";

  /**
   * Utility method that returns the IdTrackerType to use for a given taskConfig.
   *
   * @param taskConfig Contains all information required to instantiate and configure
   *                   the task.
   * @return The entity identifier tracker type.
   */
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

  @Override
  protected TaskManager createTaskManagerImpl(TaskConfiguration taskConfig)
  {
    IdTrackerType idTrackerType = getIdTrackerType(taskConfig);

    boolean usedByTag = getBooleanArgument(taskConfig, ARG_USED_BY_TAG, true);

    return new SinkSourceManager(
      taskConfig.getId(),
      new UsedWayAndNodeFilter(idTrackerType, usedByTag),
      taskConfig.getPipeArgs()
    );
  }
}
