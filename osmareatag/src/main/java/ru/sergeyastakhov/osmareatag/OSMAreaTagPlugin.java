/**
 * OSMAreaTagPlugin.java
 * <p>
 * Copyright (C) 2017 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmareatag;

import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.plugin.PluginLoader;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class OSMAreaTagPlugin implements PluginLoader
{
  @Override
  public Map<String, TaskManagerFactory> loadTaskFactories()
  {
    Map<String, TaskManagerFactory> map = new HashMap<>();

    map.put("tag-area-content", new TagAreaContentTaskFactory());
    map.put("tag-relation-content", new TagRelationContentTaskFactory());

    return map;
  }
}
