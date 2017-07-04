/**
 * EntityGeometryFactoryImpl.java
 * <p>
 * Copyright (C) 2017 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmareatag;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.*;
import org.openstreetmap.osmosis.core.store.IndexedObjectStoreReader;

import java.util.logging.Logger;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class EntityGeometryFactoryImpl implements EntityGeometryFactory
{
  private static final Logger log = Logger.getLogger(EntityGeometryFactoryImpl.class.getName());

  private static final String ROLE_OUTER = "outer";
  private static final String ROLE_INNER = "inner";

  private GeometryFactory geometryFactory;

  private IndexedObjectStoreReader<NodeContainer> nodeReader;
  private IndexedObjectStoreReader<WayContainer> wayReader;

  private LineFactory lineFactory;

  public EntityGeometryFactoryImpl(GeometryFactory _geometryFactory,
                                   IndexedObjectStoreReader<NodeContainer> _nodeReader,
                                   IndexedObjectStoreReader<WayContainer> _wayReader)
  {
    geometryFactory = _geometryFactory;
    nodeReader = _nodeReader;
    wayReader = _wayReader;

    lineFactory = new LineFactory(geometryFactory, nodeReader);
  }

  @Override
  public Geometry createGeometry(Entity entity, String entityName)
  {
    switch( entity.getType() )
    {
      case Node:
      {
        Node node = (Node) entity;
        return geometryFactory.createPoint(new Coordinate(node.getLongitude(), node.getLatitude()));
      }
      case Way:
      {
        return lineFactory.createLineString((Way) entity);
      }
    }

    return null;
  }

  @Override
  public Geometry createAreaGeometry(Entity entity, String entityName)
  {
    switch( entity.getType() )
    {
      case Node:
      {
        Node node = (Node) entity;
        return geometryFactory.createPoint(new Coordinate(node.getLongitude(), node.getLatitude()));
      }
      case Way:
      {
        AreaBuilder areaBuilder = new AreaBuilder(geometryFactory);

        LineString outerLine = lineFactory.createLineString((Way) entity);

        areaBuilder.addOuterLine(outerLine);

        return areaBuilder.createGeometry(entityName);
      }
      case Relation:
      {
        return getRelationGeometry((Relation) entity, entityName);
      }
    }

    return null;
  }

  private Geometry getRelationGeometry(Relation relation, String entityName)
  {
    AreaBuilder areaBuilder = new AreaBuilder(geometryFactory);

    for( RelationMember relationMember : relation.getMembers() )
    {
      EntityType memberType = relationMember.getMemberType();
      if( memberType != EntityType.Way )
        continue;

      long memberId = relationMember.getMemberId();

      try
      {
        WayContainer wayContainer = wayReader.get(memberId);
        Way way = wayContainer.getEntity();

        LineString lineString = lineFactory.createLineString(way);

        String role = relationMember.getMemberRole();
        if( role == null || role.isEmpty() || role.equals(ROLE_OUTER) )
        {
          areaBuilder.addOuterLine(lineString);
        }
        else if( role.equals(ROLE_INNER) )
        {
          areaBuilder.addInnerLine(lineString);
        }
      }
      catch( Exception ex )
      {
        log.fine("Skipping bad member (" + memberType + "#" + memberId + ") in " + entityName + " : " + ex);
      }
    }

    return areaBuilder.createGeometry(entityName);
  }
}
