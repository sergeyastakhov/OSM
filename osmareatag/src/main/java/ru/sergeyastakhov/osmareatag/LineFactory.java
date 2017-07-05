/**
 * LineFactory.java
 * <p>
 * Copyright (C) 2017 Sergey Astakhov. All Rights Reserved
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
import org.openstreetmap.osmosis.core.store.NoSuchIndexElementException;
import ru.sergeyastakhov.osmareatag.rules.AreaRule;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class LineFactory
{
  private static final Logger log = Logger.getLogger(AreaRule.class.getName());

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

    List<Coordinate> points = new ArrayList<>(wayNodes.size());

    for( WayNode wayNode : wayNodes )
    {
      try
      {
        NodeContainer nodeContainer = nodeReader.get(wayNode.getNodeId());

        Node node = nodeContainer.getEntity();

        points.add(new Coordinate(node.getLongitude(), node.getLatitude()));
      }
      catch( NoSuchIndexElementException ex )
      {
        log.fine("Missing node "+wayNode + " in way "+way+", try to ignore");
      }
    }

    return points.toArray(new Coordinate[points.size()]);
  }

  public LineString createLineString(Way way)
  {
    return geometryFactory.createLineString(getCoordinates(way));
  }
}
