/**
 * $Id$
 *
 * Copyright (C) 2012 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.crstransform;


import java.util.logging.Level;
import java.util.logging.Logger;

import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.core.task.v0_6.SinkSource;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class CoordinateTransformationTask implements SinkSource
{
  private static final Logger log = Logger.getLogger(CoordinateTransformationTask.class.getName());

  private Sink sink;
  private MathTransform mathTransform;

  public CoordinateTransformationTask(MathTransform _mathTransform)
  {
    mathTransform = _mathTransform;
  }

  private void transform(Node node)
  {
    double[] array = {node.getLongitude(), node.getLatitude()};

    try
    {
      mathTransform.transform(array, 0, array, 0, 1);

      node.setLongitude(array[0]);
      node.setLatitude(array[1]);
    }
    catch( TransformException e )
    {
      log.log(Level.SEVERE,
         "Can't transform coordinate (" + node.getLongitude() + "," + node.getLatitude() + ") for Node "
            + node.getId() + ": " + e, e);
    }
  }

  @Override
  public void process(EntityContainer entityContainer)
  {
    Entity entity = entityContainer.getEntity();

    if( entity instanceof Node )
    {
      transform((Node) entity);
    }

    sink.process(entityContainer);
  }


  @Override
  public void setSink(Sink _sink)
  {
    sink = _sink;
  }


  @Override
  public void complete()
  {
    sink.complete();
  }


  @Override
  public void release()
  {
    sink.release();
  }
}
