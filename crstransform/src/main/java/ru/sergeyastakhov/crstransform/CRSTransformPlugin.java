/**
 * $Id$
 *
 * Copyright (C) 2012 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.crstransform;

import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.plugin.PluginLoader;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class CRSTransformPlugin implements PluginLoader
{
  @Override
  public Map<String, TaskManagerFactory> loadTaskFactories()
  {
    Map<String, TaskManagerFactory> map = new HashMap<String, TaskManagerFactory>();

    map.put("crs-transform", new CoordinateTransformationTaskFactory());

    return map;
  }
}
