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
    String[] areaTagValues = getStringArgument(taskConfig, "areaTagValue").split("\\|");

    String markEntityTagName = getStringArgument(taskConfig, "markEntityTagName");

    String markEntityTagValue = getStringArgument(taskConfig, "markEntityTagValue", null);

    String[] markEntityTagValues = markEntityTagValue != null ? markEntityTagValue.split("\\|") : new String[0];

    String insideTagStr = getStringArgument(taskConfig, "insideTag", null);
    Tag insideTag = null;

    if( insideTagStr != null )
    {
      int index = insideTagStr.indexOf('=');
      if( index == -1 )
      {
        throw new OsmosisRuntimeException(
            "Argument insideTag for task " + taskConfig.getId()
                + " must contain tag expression. Examples: \"maxspeed=RU:urban\" \"is_in:${place}=${name}\"");
      }

      insideTag = new Tag(insideTagStr.substring(0, index), insideTagStr.substring(index + 1));
    }

    String outsideTagStr = getStringArgument(taskConfig, "outsideTag", null);

    Tag outsideTag = null;

    if( outsideTagStr != null )
    {
      int index = outsideTagStr.indexOf('=');
      if( index == -1 )
      {
        throw new OsmosisRuntimeException(
            "Argument outsideTag for task " + taskConfig.getId()
                + " must contain tag expression. Example: \"maxspeed=RU:rural\"");
      }

      outsideTag = new Tag(outsideTagStr.substring(0, index), outsideTagStr.substring(index + 1));
    }

    return new SinkSourceManager(
        taskConfig.getId(),
        new TagAreaContentTask(areaTagName, areaTagValues, markEntityTagName, markEntityTagValues, insideTag, outsideTag),
        taskConfig.getPipeArgs()
    );
  }
}
