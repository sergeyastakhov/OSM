/**
 * $Id$
 *
 * Copyright (C) 2010 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmfilter;

import org.openstreetmap.osmosis.core.pipeline.common.TaskConfiguration;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManager;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.pipeline.v0_6.SinkSourceManager;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class ConstructionWayFilterTaskFactory extends TaskManagerFactory
{
  private static final String DAYS_BEFORE_OPENING = "daysBeforeOpening";
  private static final String DAYS_AFTER_CHECKING = "daysAfterChecking";
  private static final String WRITE_ERROR_XML = "writeErrorXML";

  @Override
  protected TaskManager createTaskManagerImpl(TaskConfiguration taskConfig)
  {
    int daysBeforeOpening = getIntegerArgument(taskConfig, DAYS_BEFORE_OPENING, -1);
    int daysAfterChecking = getIntegerArgument(taskConfig, DAYS_AFTER_CHECKING, -1);

    String writeErrorXML = getStringArgument(taskConfig, WRITE_ERROR_XML, null);

    return new SinkSourceManager(
      taskConfig.getId(),
      new ConstructionWayFilter(daysBeforeOpening, daysAfterChecking, writeErrorXML),
      taskConfig.getPipeArgs()
    );
  }
}