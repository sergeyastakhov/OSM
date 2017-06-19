/**
 * $Id$
 *
 * Copyright (C) 2012 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmrouting;

import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.plugin.PluginLoader;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class OSMRoutingPlugin implements PluginLoader
{
  @Override
  public Map<String, TaskManagerFactory> loadTaskFactories()
  {
    Map<String, TaskManagerFactory> map = new HashMap<>();

    map.put("build-routing-graph", new BuildRoutingGraphTaskFactory());

    map.put("split-routing-graph", new SplitRoutingGraphTaskFactory());

    map.put("directed-graph-lines", new DirectedGraphLinesTaskFactory());

    return map;
  }
}
