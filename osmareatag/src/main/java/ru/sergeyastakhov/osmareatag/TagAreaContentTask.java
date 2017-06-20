/**
 * TagAreaContentTask.java
 * <p>
 * Copyright (C) 2017 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmareatag;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.index.strtree.STRtree;
import org.openstreetmap.osmosis.core.container.v0_6.*;
import org.openstreetmap.osmosis.core.domain.v0_6.*;
import org.openstreetmap.osmosis.core.filter.common.IdTracker;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerFactory;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerType;
import org.openstreetmap.osmosis.core.lifecycle.ReleasableIterator;
import org.openstreetmap.osmosis.core.store.IndexedObjectStore;
import org.openstreetmap.osmosis.core.store.IndexedObjectStoreReader;
import org.openstreetmap.osmosis.core.store.SimpleObjectStore;
import org.openstreetmap.osmosis.core.store.SingleClassObjectSerializationFactory;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.core.task.v0_6.SinkSource;

import java.util.*;
import java.util.logging.Logger;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class TagAreaContentTask implements SinkSource, EntityProcessor
{
  private static final Logger log = Logger.getLogger(TagAreaContentTask.class.getName());

  private static final int SRID_WGS84 = 4326;

  private static final String ROLE_OUTER = "outer";
  private static final String ROLE_INNER = "inner";

  public static final String TAG_TYPE = "type";

  public static final String TYPE_MULTIPOLYGON = "multipolygon";

  private Sink sink;

  private String areaTagName;
  private Set<String> areaTagValues;

  private IndexedObjectStore<NodeContainer> indexedNodes;
  private IndexedObjectStore<WayContainer> indexedWays;

  private IdTracker areaWays;
  private SimpleObjectStore<RelationContainer> areaRelations;

  private SimpleObjectStore<NodeContainer> allNodes;
  private SimpleObjectStore<WayContainer> allWays;
  private SimpleObjectStore<RelationContainer> allRelations;

  private STRtree areaIndex;

  private Tag insideTag;
  private Tag outsideTag;

  public TagAreaContentTask(String _areaTagName, String[] _areaTagValues, Tag _insideTag, Tag _outsideTag)
  {
    areaTagName = _areaTagName;
    areaTagValues = new HashSet<>();
    areaTagValues.addAll(Arrays.asList(_areaTagValues));

    insideTag = _insideTag;
    outsideTag = _outsideTag;
  }

  @Override
  public void initialize(Map<String, Object> metaData)
  {
    sink.initialize(metaData);

    indexedNodes = new IndexedObjectStore<>(
        new SingleClassObjectSerializationFactory(NodeContainer.class), "tact_nd");
    indexedWays = new IndexedObjectStore<>(
        new SingleClassObjectSerializationFactory(WayContainer.class), "tact_wy");

    areaWays = IdTrackerFactory.createInstance(IdTrackerType.Dynamic);
    areaRelations = new SimpleObjectStore<>(
        new SingleClassObjectSerializationFactory(RelationContainer.class), "tact_rl", true);

    allNodes = new SimpleObjectStore<>(
        new SingleClassObjectSerializationFactory(NodeContainer.class), "tact_and", true);
    allWays = new SimpleObjectStore<>(
        new SingleClassObjectSerializationFactory(WayContainer.class), "tact_awy", true);
    allRelations = new SimpleObjectStore<>(
        new SingleClassObjectSerializationFactory(RelationContainer.class), "tact_arl", true);


    areaIndex = new STRtree();
  }


  @Override
  public void process(BoundContainer bound) { sink.process(bound); }

  @Override
  public void process(NodeContainer nodeContainer)
  {
    allNodes.add(nodeContainer);

    Node node = nodeContainer.getEntity();
    long nodeId = node.getId();
    indexedNodes.add(nodeId, nodeContainer);
  }

  @Override
  public void process(WayContainer wayContainer)
  {
    allWays.add(wayContainer);

    Way way = wayContainer.getEntity();

    long wayId = way.getId();
    indexedWays.add(wayId, wayContainer);

    Map<String, String> tags = ((TagCollection) way.getTags()).buildMap();

    String areaTagValue = tags.get(areaTagName);
    if( areaTagValue != null && areaTagValues.contains(areaTagValue) )
    {
      areaWays.set(wayId);
    }
  }

  @Override
  public void process(RelationContainer relationContainer)
  {
    allRelations.add(relationContainer);

    Relation relation = relationContainer.getEntity();

    long relationId = relation.getId();

    Map<String, String> tags = ((TagCollection) relation.getTags()).buildMap();

    String typeValue = tags.get(TAG_TYPE);

    if( typeValue == null || !typeValue.equals(TYPE_MULTIPOLYGON) )
    {
      // Обрабатываем только мультиполигоны
      return;
    }

    String areaTagValue = tags.get(areaTagName);
    if( areaTagValue != null && areaTagValues.contains(areaTagValue) )
    {
      areaRelations.add(relationContainer);
    }
  }

  @Override
  public void process(EntityContainer entityContainer)
  {
    entityContainer.process(this);
  }

  @Override
  public void complete()
  {
    indexedNodes.complete();
    indexedWays.complete();

    areaRelations.complete();

    allNodes.complete();
    allWays.complete();
    allRelations.complete();

    log.info("Creating area spatial index...");

    IndexedObjectStoreReader<NodeContainer> nodeReader = indexedNodes.createReader();
    IndexedObjectStoreReader<WayContainer> wayReader = indexedWays.createReader();

    GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), SRID_WGS84);

    LineFactory lineFactory = new LineFactory(geometryFactory, nodeReader);

    for( Long wayId : areaWays )
    {
      String entityName = "way #" + wayId;

      WayContainer wayContainer = wayReader.get(wayId);
      Way way = wayContainer.getEntity();

      try
      {
        AreaBuilder areaBuilder = new AreaBuilder(geometryFactory);

        LineString outerLine = lineFactory.createLineString(way);

        areaBuilder.addOuterLine(outerLine);

        Geometry geometry = areaBuilder.createGeometry(entityName);

        if( geometry != null )
        {
          areaIndex.insert(geometry.getEnvelopeInternal(), new EntityArea<>(geometry, way));
        }
        else
        {
          log.fine("Error processing " + entityName + " : Invalid geometry");
        }
      }
      catch( Exception ex )
      {
        log.warning("Error processing " + entityName + " : " + ex);
      }
    }

    ReleasableIterator<RelationContainer> areaRelationIterator = areaRelations.iterate();
    while( areaRelationIterator.hasNext() )
    {
      RelationContainer relationContainer = areaRelationIterator.next();

      Relation relation = relationContainer.getEntity();

      long relationId = relation.getId();
      String entityName = "relation #" + relationId;

      try
      {
        Geometry geometry = getRelationGeometry(wayReader, geometryFactory, lineFactory, relation, entityName);

        if( geometry != null )
        {
          areaIndex.insert(geometry.getEnvelopeInternal(), new EntityArea<>(geometry, relation));
        }
        else
        {
          log.fine("Error processing " + entityName + " : Invalid geometry");
        }
      }
      catch( Exception ex )
      {
        log.warning("Error processing " + entityName + " : " + ex);
      }
    }

    areaRelationIterator.release();

    areaIndex.build();

    log.info("Send data");

    ReleasableIterator<NodeContainer> nodeIterator = allNodes.iterate();
    while( nodeIterator.hasNext() )
    {
      NodeContainer nodeContainer = nodeIterator.next();

      Node node = nodeContainer.getEntity();
      long nodeId = node.getId();

      sink.process(nodeContainer);
    }

    nodeIterator.release();

    ReleasableIterator<WayContainer> wayIterator = allWays.iterate();
    while( wayIterator.hasNext() )
    {
      WayContainer wayContainer = wayIterator.next();
      Way way = wayContainer.getEntity();

      String entityName = "way #" + way.getId();

      try
      {
        LineString wayLine = lineFactory.createLineString(way);

        wayContainer = markEntityArea(wayContainer, wayLine);
      }
      catch( Exception ex )
      {
        log.warning("Error processing " + entityName + " : " + ex);
      }

      sink.process(wayContainer);
    }

    wayIterator.release();


    ReleasableIterator<RelationContainer> relationIterator = allRelations.iterate();
    while( relationIterator.hasNext() )
    {
      RelationContainer relationContainer = relationIterator.next();

      Relation relation = relationContainer.getEntity();

      long relationId = relation.getId();
      String entityName = "relation #" + relationId;

      Map<String, String> tags = ((TagCollection) relation.getTags()).buildMap();

      String typeValue = tags.get(TAG_TYPE);

      if( typeValue != null && typeValue.equals(TYPE_MULTIPOLYGON) )
      {
        try
        {
          Geometry geometry = getRelationGeometry(wayReader, geometryFactory, lineFactory, relation, entityName);

          if( geometry != null )
          {
            relationContainer = markEntityArea(relationContainer, geometry);
          }
          else
          {
            log.fine("Error processing " + entityName + " : Invalid geometry");
          }
        }
        catch( Exception ex )
        {
          log.warning("Error processing " + entityName + " : " + ex);
        }
      }

      sink.process(relationContainer);
    }

    relationIterator.release();

    nodeReader.release();
    wayReader.release();

    log.info("Sending complete.");

    sink.complete();
  }

  private <E extends EntityContainer> E markEntityArea(E entityContainer, Geometry entityGeometry)
  {
    Entity entity = entityContainer.getEntity();

    EntityArea<?> insideArea = null;

    List<EntityArea<?>> areas = areaIndex.query(entityGeometry.getEnvelopeInternal());

    for( EntityArea<?> area : areas )
    {
      if( area.entity.equals(entity) )
        continue;

      IntersectionMatrix relate = area.geometry.relate(entityGeometry);

      entityContainer = (E) entityContainer.getWriteableInstance();

      if( relate.isCovers() )
      {
        insideArea = area;
        break;
      }
    }

    if( insideArea != null )
    {
      tagInsideEntity(entityContainer.getEntity(),insideArea);
    }
    else
    {
      tagOutsideEntity(entityContainer.getEntity());
    }

    return entityContainer;
  }

  private Geometry getRelationGeometry
      (IndexedObjectStoreReader<WayContainer> _wayReader,
       GeometryFactory geometryFactory, LineFactory lineFactory, Relation relation,
       String entityName)
  {
    AreaBuilder areaBuilder = new AreaBuilder(geometryFactory);

    for( RelationMember relationMember : relation.getMembers() )
    {
      EntityType memberType = relationMember.getMemberType();
      if( memberType != EntityType.Way )
        continue;

      long memberId = relationMember.getMemberId();

      WayContainer wayContainer = _wayReader.get(memberId);
      Way way = wayContainer.getEntity();

      LineString lineString = lineFactory.createLineString(way);

      String role = relationMember.getMemberRole();
      if( role == null || role.length() == 0 || role.equals(ROLE_OUTER) )
      {
        areaBuilder.addOuterLine(lineString);
      }
      else if( role.equals(ROLE_INNER) )
      {
        areaBuilder.addInnerLine(lineString);
      }
    }

    return areaBuilder.createGeometry(entityName);
  }

  private void tagInsideEntity(Entity entity, EntityArea<?> area)
  {
    if( insideTag == null )
      return;

    Collection<Tag> tags = entity.getTags();

    for( Tag tag : tags )
    {
      String tagName = tag.getKey();

      if( tagName.equals(insideTag.getKey()) )
      {
        return;
      }
    }

    tags.add(area.resolveTag(insideTag));
  }

  private void tagOutsideEntity(Entity entity)
  {
    if( outsideTag == null )
      return;

    Collection<Tag> tags = entity.getTags();

    for( Tag tag : tags )
    {
      String tagName = tag.getKey();

      if( tagName.equals(outsideTag.getKey()) )
      {
        return;
      }
    }

    tags.add(outsideTag);
  }

  @Override
  public void release()
  {
    sink.release();

    indexedNodes.release();
    indexedWays.release();

    areaRelations.release();

    allNodes.release();
    allWays.release();
    allRelations.release();

    areaIndex = null;
  }

  @Override
  public void setSink(Sink _sink) { sink = _sink; }
}
