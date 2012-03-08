/**
 * $Id$
 *
 * Copyright (C) 2010 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmfilter;

import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.plugin.PluginLoader;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class OSMFilterPlugin implements PluginLoader
{
  public Map<String, TaskManagerFactory> loadTaskFactories()
  {
    Map<String, TaskManagerFactory> map = new HashMap<String, TaskManagerFactory>();
    map.put("used-way-and-node", new UsedWayAndNodeFilterTaskFactory());
    map.put("uwn", new UsedWayAndNodeFilterTaskFactory());

    map.put("construction-way", new ConstructionWayFilterTaskFactory());
    return map;
  }
}
