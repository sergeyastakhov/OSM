/**
 * TagAreaContentTaskFactory.java
 * <p>
 * Copyright (C) 2017 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmareatag;

import org.openstreetmap.osmosis.core.OsmosisRuntimeException;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.pipeline.common.TaskConfiguration;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManager;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.pipeline.v0_6.SinkSourceManager;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class TagAreaContentTaskFactory extends TaskManagerFactory
{
  @Override
  protected TaskManager createTaskManagerImpl(TaskConfiguration taskConfig)
  {
    String areaTagName = getStringArgument(taskConfig, "areaTagName");
    String[] areaTagValues = getStringArgument(taskConfig, "areaTagValue").split(";");

    String insideTagStr = getStringArgument(taskConfig, "insideTag", null);
    Tag insideTag = null;

    if( insideTagStr != null )
    {
      String[] split = insideTagStr.split("=");
      if( split.length != 2 )
      {
        throw new OsmosisRuntimeException(
            "Argument insideTag for task " + taskConfig.getId()
                + " must contain tag expression. Examples: \"maxspeed=RU:urban\" \"is_in:${place}=${name}\"");
      }

      insideTag = new Tag(split[0], split[1]);
    }

    String outsideTagStr = getStringArgument(taskConfig, "outsideTag", null);

    Tag outsideTag = null;

    if( outsideTagStr != null )
    {
      String[] split = outsideTagStr.split("=");
      if( split.length != 2 )
      {
        throw new OsmosisRuntimeException(
            "Argument outsideTag for task " + taskConfig.getId()
                + " must contain tag expression. Example: \"maxspeed=RU:rural\"");
      }

      outsideTag = new Tag(split[0], split[1]);
    }

    return new SinkSourceManager(
        taskConfig.getId(),
        new TagAreaContentTask(areaTagName, areaTagValues, insideTag, outsideTag),
        taskConfig.getPipeArgs()
    );
  }
}
