/**
 * TagAreaContentTaskFactory.java
 * <p>
 * Copyright (C) 2017 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmareatag;

import org.openstreetmap.osmosis.core.OsmosisRuntimeException;
import org.openstreetmap.osmosis.core.pipeline.common.TaskConfiguration;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManager;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.pipeline.v0_6.SinkSourceManager;
import ru.sergeyastakhov.osmareatag.rules.TagProcessing;
import ru.sergeyastakhov.osmareatag.rules.XMLTagProcessingLoader;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class TagAreaContentTaskFactory extends TaskManagerFactory
{
  @Override
  protected TaskManager createTaskManagerImpl(TaskConfiguration taskConfig)
  {
    String configFile = getStringArgument(taskConfig, "file",
        getDefaultStringArgument(taskConfig, "tag-processing.xml"));

    boolean prepareOnly = getBooleanArgument(taskConfig, "prepareOnly", false);

    TagProcessing tagProcessing;

    try
    {
      tagProcessing = new XMLTagProcessingLoader().load(configFile);
    }
    catch( Exception ex )
    {
      throw new OsmosisRuntimeException("Can't load tag processing config from file " + configFile, ex);
    }

    if( tagProcessing==null )
    {
      throw new OsmosisRuntimeException("Can't find tag processing config in file " + configFile);
    }

    return new SinkSourceManager(
        taskConfig.getId(),
        new TagAreaContentTask(tagProcessing, prepareOnly),
        taskConfig.getPipeArgs()
    );
  }
}
