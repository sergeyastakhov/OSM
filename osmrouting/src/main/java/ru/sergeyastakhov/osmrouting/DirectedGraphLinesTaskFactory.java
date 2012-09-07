/**
 * $Id$
 *
 * Copyright (C) 2012 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmrouting;

import org.openstreetmap.osmosis.core.pipeline.common.TaskConfiguration;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManager;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.pipeline.v0_6.SinkSourceManager;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class DirectedGraphLinesTaskFactory extends TaskManagerFactory
{
  @Override
  protected TaskManager createTaskManagerImpl(TaskConfiguration taskConfig)
  {
    boolean vehicleOnly = getBooleanArgument(taskConfig, "vehicleOnly", true);

    return new SinkSourceManager(
      taskConfig.getId(),
      new DirectedGraphLinesTask(vehicleOnly),
      taskConfig.getPipeArgs()
    );
  }
}
