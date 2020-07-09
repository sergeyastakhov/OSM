/**
 * TagAreaContentTask.java
 * <p>
 * Copyright (C) 2017 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmareatag;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import org.openstreetmap.osmosis.core.container.v0_6.*;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.TagCollection;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.lifecycle.ReleasableIterator;
import org.openstreetmap.osmosis.core.store.IndexedObjectStore;
import org.openstreetmap.osmosis.core.store.IndexedObjectStoreReader;
import org.openstreetmap.osmosis.core.store.SimpleObjectStore;
import org.openstreetmap.osmosis.core.store.SingleClassObjectSerializationFactory;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.core.task.v0_6.SinkSource;
import ru.sergeyastakhov.osmareatag.rules.TagProcessing;

import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class TagAreaContentTask implements SinkSource, EntityProcessor
{
  private static final Logger log = Logger.getLogger(TagAreaContentTask.class.getName());

  private static final int SRID_WGS84 = 4326;

  public static final String TAG_TYPE = "type";

  public static final String TYPE_MULTIPOLYGON = "multipolygon";
  public static final String TYPE_BOUNDARY = "boundary";

  private Sink sink;

  private TagProcessing tagProcessing;

  private boolean prepareOnly;

  private IndexedObjectStore<NodeContainer> indexedNodes;
  private IndexedObjectStore<WayContainer> indexedWays;

  private SimpleObjectStore<NodeContainer> allNodes;
  private SimpleObjectStore<WayContainer> allWays;
  private SimpleObjectStore<RelationContainer> allRelations;

  public TagAreaContentTask(TagProcessing _tagProcessing, boolean _prepareOnly)
  {
    tagProcessing = _tagProcessing;
    prepareOnly = _prepareOnly;
  }

  @Override
  public void initialize(Map<String, Object> metaData)
  {
    sink.initialize(metaData);

    tagProcessing.initialize(metaData);

    indexedNodes = new IndexedObjectStore<>(
        new SingleClassObjectSerializationFactory(NodeContainer.class), "tact_nd");
    indexedWays = new IndexedObjectStore<>(
        new SingleClassObjectSerializationFactory(WayContainer.class), "tact_wy");

    allNodes = new SimpleObjectStore<>(
        new SingleClassObjectSerializationFactory(NodeContainer.class), "tact_and", true);
    allWays = new SimpleObjectStore<>(
        new SingleClassObjectSerializationFactory(WayContainer.class), "tact_awy", true);
    allRelations = new SimpleObjectStore<>(
        new SingleClassObjectSerializationFactory(RelationContainer.class), "tact_arl", true);
  }


  @Override
  public void process(BoundContainer bound) { sink.process(bound); }

  @Override
  public void process(NodeContainer nodeContainer)
  {
    if( !prepareOnly )
    {
      allNodes.add(nodeContainer);
    }

    Node node = nodeContainer.getEntity();
    long nodeId = node.getId();
    indexedNodes.add(nodeId, nodeContainer);
  }

  @Override
  public void process(WayContainer wayContainer)
  {
    if( !prepareOnly )
    {
      allWays.add(wayContainer);
    }

    Way way = wayContainer.getEntity();

    long wayId = way.getId();
    indexedWays.add(wayId, wayContainer);

    Map<String, String> tags = ((TagCollection) way.getTags()).buildMap();

    tagProcessing.prepareAreaRules(wayContainer, tags);
  }

  @Override
  public void process(RelationContainer relationContainer)
  {
    if( !prepareOnly )
    {
      allRelations.add(relationContainer);
    }

    Relation relation = relationContainer.getEntity();

    long relationId = relation.getId();

    Map<String, String> tags = ((TagCollection) relation.getTags()).buildMap();

    String typeValue = tags.get(TAG_TYPE);

    // Обрабатываем только мультиполигоны
    if( typeValue != null && (typeValue.equals(TYPE_MULTIPOLYGON) || typeValue.equals(TYPE_BOUNDARY)) )
    {
      tagProcessing.prepareAreaRules(relationContainer, tags);
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

    allNodes.complete();
    allWays.complete();
    allRelations.complete();

    log.info("Creating area spatial indexes...");

    tagProcessing.complete();

    IndexedObjectStoreReader<NodeContainer> nodeReader = indexedNodes.createReader();
    IndexedObjectStoreReader<WayContainer> wayReader = indexedWays.createReader();

    GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), SRID_WGS84);

    EntityGeometryFactory entityGeometryFactory = new EntityGeometryFactoryImpl(geometryFactory, nodeReader, wayReader);

    tagProcessing.buildIndexes(entityGeometryFactory);

    if( !prepareOnly )
    {
      log.info("Send data");

      ReleasableIterator<NodeContainer> nodeIterator = allNodes.iterate();
      while( nodeIterator.hasNext() )
      {
        NodeContainer nodeContainer = nodeIterator.next();

        Node node = nodeContainer.getEntity();

        Map<String, String> tags = ((TagCollection) node.getTags()).buildMap();

        String entityName = "node #" + node.getId();

        try
        {
          nodeContainer = tagProcessing.processEntity(nodeContainer, tags, entityGeometryFactory);
        }
        catch( Exception ex )
        {
          log.warning("Error processing " + entityName + " : " + ex);
        }

        sink.process(nodeContainer);
      }

      nodeIterator.close();

      ReleasableIterator<WayContainer> wayIterator = allWays.iterate();
      while( wayIterator.hasNext() )
      {
        WayContainer wayContainer = wayIterator.next();
        Way way = wayContainer.getEntity();

        Map<String, String> tags = ((TagCollection) way.getTags()).buildMap();

        String entityName = "way #" + way.getId();

        try
        {
          wayContainer = tagProcessing.processEntity(wayContainer, tags, entityGeometryFactory);
        }
        catch( Exception ex )
        {
          log.warning("Error processing " + entityName + " : " + ex);
        }
        sink.process(wayContainer);
      }

      wayIterator.close();


      ReleasableIterator<RelationContainer> relationIterator = allRelations.iterate();
      while( relationIterator.hasNext() )
      {
        RelationContainer relationContainer = relationIterator.next();

        Relation relation = relationContainer.getEntity();

        Map<String, String> tags = ((TagCollection) relation.getTags()).buildMap();

        String typeValue = tags.get(TAG_TYPE);

        if( typeValue != null && (typeValue.equals(TYPE_MULTIPOLYGON) || typeValue.equals(TYPE_BOUNDARY)) )
        {
          long relationId = relation.getId();

          String entityName = "relation #" + relationId;

          try
          {
            relationContainer = tagProcessing.processEntity(relationContainer, tags, entityGeometryFactory);
          }
          catch( Exception ex )
          {
            log.warning("Error processing " + entityName + " : " + ex);
          }
        }

        sink.process(relationContainer);
      }

      relationIterator.close();

      log.info("Sending complete.");
    }

    nodeReader.close();
    wayReader.close();

    sink.complete();
  }

  @Override
  public void close()
  {
    sink.close();

    indexedNodes.close();
    indexedWays.close();

    allNodes.close();
    allWays.close();
    allRelations.close();
  }

  @Override
  public void setSink(Sink _sink) { sink = _sink; }
}
