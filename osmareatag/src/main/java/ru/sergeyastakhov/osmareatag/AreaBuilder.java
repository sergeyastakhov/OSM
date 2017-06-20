/**
 * AreaBuilder.java
 * <p>
 * Copyright (C) 2017 RNIC. All Rights Reserved
 */
package ru.sergeyastakhov.osmareatag;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.operation.linemerge.LineMerger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class AreaBuilder
{
  private static final Logger log = Logger.getLogger(AreaBuilder.class.getName());

  private GeometryFactory geometryFactory;

  private LineMerger outerMerger = new LineMerger();
  private LineMerger innerMerger = new LineMerger();

  public AreaBuilder(GeometryFactory _geometryFactory)
  {
    geometryFactory = _geometryFactory;
  }

  public void addOuterLine(LineString line)
  {
    outerMerger.add(line);
  }

  public void addInnerLine(LineString line)
  {
    innerMerger.add(line);
  }

  public Geometry createGeometry(String entity)
  {
    List<Polygon> polygons = new ArrayList<>();

    Collection<LineString> outerLines = outerMerger.getMergedLineStrings();

    List<LinearRing> outerRings = new ArrayList<>();

    for( LineString outerLine : outerLines )
    {
      if( outerLine.isRing() )
      {
        outerRings.add(geometryFactory.createLinearRing(outerLine.getCoordinateSequence()));
      }
      else
      {
        log.fine("Entity " + entity + ": Outer line is not a ring - " + outerLine);
      }
    }

    Collection<LineString> innerStrings = innerMerger.getMergedLineStrings();
    List<LineString> innerLines = innerStrings != null ? new ArrayList<>(innerStrings) : null;

    for( LinearRing outerRing : outerRings )
    {
      List<LinearRing> innerRings = new ArrayList<>();

      if( innerLines != null )
      {
        for( Iterator<LineString> iterator = innerLines.iterator(); iterator.hasNext(); )
        {
          LineString innerLine = iterator.next();

          if( outerRing.covers(innerLine) )
          {
            if( innerLine.isRing() )
            {
              innerRings.add(geometryFactory.createLinearRing(innerLine.getCoordinateSequence()));
            }
            else
            {
              log.fine("Entity " + entity + ": Inner line is not a ring - " + innerLine);
            }

            iterator.remove();
          }
        }
      }

      Polygon polygon = geometryFactory.createPolygon(outerRing, innerRings.toArray(new LinearRing[innerRings.size()]));

      polygons.add(polygon);
    }

    if( polygons.isEmpty() )
    {
      return null;
    }

    if( polygons.size() == 1 )
    {
      return polygons.get(0);
    }

    Polygon[] array = polygons.toArray(new Polygon[polygons.size()]);

    return geometryFactory.createMultiPolygon(array);
  }
}
