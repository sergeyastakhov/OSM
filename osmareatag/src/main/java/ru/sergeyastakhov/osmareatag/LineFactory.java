/**
 * LineFactory.java
 * <p>
 * Copyright (C) 2017 RNIC. All Rights Reserved
 */
package ru.sergeyastakhov.osmareatag;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.store.IndexedObjectStoreReader;

import java.util.List;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class LineFactory
{
  private GeometryFactory geometryFactory;
  private IndexedObjectStoreReader<NodeContainer> nodeReader;

  public LineFactory(GeometryFactory _geometryFactory, IndexedObjectStoreReader<NodeContainer> _nodeReader)
  {
    geometryFactory = _geometryFactory;
    nodeReader = _nodeReader;
  }

  private Coordinate[] getCoordinates(Way way)
  {
    List<WayNode> wayNodes = way.getWayNodes();

    Coordinate[] points = new Coordinate[wayNodes.size()];

    for( int i = 0; i < wayNodes.size(); i++ )
    {
      WayNode wayNode = wayNodes.get(i);

      NodeContainer nodeContainer = nodeReader.get(wayNode.getNodeId());

      Node node = nodeContainer.getEntity();

      points[i] = new Coordinate(node.getLongitude(), node.getLatitude());
    }

    return points;
  }

  public LineString createLineString(Way way)
  {
    return geometryFactory.createLineString(getCoordinates(way));
  }
}
