/**
 * $Id$
 *
 * Copyright (C) 2012 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.crstransform;

import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.openstreetmap.osmosis.core.OsmosisRuntimeException;
import org.openstreetmap.osmosis.core.pipeline.common.TaskConfiguration;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManager;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.pipeline.v0_6.SinkSourceManager;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class CoordinateTransformationTaskFactory extends TaskManagerFactory
{
  private CoordinateReferenceSystem getCRSArgument(TaskConfiguration taskConfig, String param, String defaultValue)
  {
    String crs = getStringArgument(taskConfig, param, defaultValue);

    try
    {
      return CRS.decode(crs);
    }
    catch( FactoryException e )
    {
      throw new OsmosisRuntimeException(
         "Argument " + param + " for task " + taskConfig.getId()
            + " must contain a CRS code.", e);
    }
  }

  @Override
  protected TaskManager createTaskManagerImpl(TaskConfiguration taskConfig)
  {
    CoordinateReferenceSystem crsFrom = getCRSArgument(taskConfig, "from", "EPSG:4326");
    CoordinateReferenceSystem crsTo = getCRSArgument(taskConfig, "to", "EPSG:3857");

    MathTransform mathTransform;

    try
    {
      mathTransform = CRS.findMathTransform(crsFrom, crsTo);
    }
    catch( FactoryException e )
    {
      throw new OsmosisRuntimeException
         ("Can't create MathTransform from " + crsFrom.getName() + " to " + crsTo.getName(), e);
    }

    return new SinkSourceManager
       (taskConfig.getId(), new CoordinateTransformationTask(mathTransform), taskConfig.getPipeArgs());
  }
}
