/**
 * $Id$
 *
 * Copyright (C) 2012-2017 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmrouting;

import org.openstreetmap.osmosis.core.container.v0_6.*;
import org.openstreetmap.osmosis.core.domain.v0_6.CommonEntityData;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.lifecycle.ReleasableIterator;
import org.openstreetmap.osmosis.core.store.SimpleObjectStore;
import org.openstreetmap.osmosis.core.store.SingleClassObjectSerializationFactory;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.core.task.v0_6.SinkSource;

import java.util.*;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class SplitRoutingGraphTask implements SinkSource, EntityProcessor
{
  private Sink sink;

  private SimpleObjectStore<WayContainer> allWays;

  private Map<Long, Integer> nodeWayUsages = new HashMap<>();

  private IdFactory wayIdFactory = EntityIdFactory.wayIdFactory;

  public SplitRoutingGraphTask()
  {
    allWays = new SimpleObjectStore<>(
        new SingleClassObjectSerializationFactory(WayContainer.class), "srgt_wy", true);
  }

  @Override
  public void initialize(Map<String, Object> metaData) {}

  @Override
  public void process(EntityContainer entityContainer)
  {
    entityContainer.process(this);
  }

  @Override
  public void complete()
  {
    ReleasableIterator<WayContainer> wayIterator = allWays.iterate();
    while( wayIterator.hasNext() )
    {
      WayContainer wayContainer = wayIterator.next();

      Way way = wayContainer.getEntity();

      List<WayNode> wayNodeList = new ArrayList<>();
      boolean firstSplit = true;

      for( WayNode wayNode : way.getWayNodes() )
      {
        wayNodeList.add(wayNode);

        long nodeId = wayNode.getNodeId();

        Integer counter = nodeWayUsages.get(nodeId);

        if( counter != null && counter > 1 && wayNodeList.size() > 1 )
        {
          // Node used in more than two ways

          long id;

          if( firstSplit )
          {
            id = way.getId();
            firstSplit = false;
          }
          else
          {
            id = wayIdFactory.nextId();
          }

          CommonEntityData entityData = new CommonEntityData
            (id, 1, way.getTimestamp(), way.getUser(), 0, way.getTags());

          entityData.getTags().add(new Tag("original_id", String.valueOf(way.getId())));

          Way newWay = new Way(entityData, wayNodeList);

          sink.process(new WayContainer(newWay));

          wayNodeList.clear();
          wayNodeList.add(wayNode);
        }
      }

      if( wayNodeList.size() == way.getWayNodes().size() )
      {
        sink.process(wayContainer);
      }
      else if( wayNodeList.size() > 1 )
      {
        CommonEntityData entityData = new CommonEntityData
          (wayIdFactory.nextId(), 1, way.getTimestamp(), way.getUser(), 0, way.getTags());

        entityData.getTags().add(new Tag("original_id", String.valueOf(way.getId())));

        Way newWay = new Way(entityData, wayNodeList);

        sink.process(new WayContainer(newWay));
      }
    }

    sink.complete();
  }

  @Override
  public void release()
  {
    sink.release();
  }

  @Override
  public void setSink(Sink _sink)
  {
    sink = _sink;
  }

  @Override
  public void process(BoundContainer bound)
  {
    sink.process(bound);
  }

  @Override
  public void process(NodeContainer node)
  {
    sink.process(node);
  }

  @Override
  public void process(WayContainer wayContainer)
  {
    Way way = wayContainer.getEntity();

    Set<Long> nodeIds = new HashSet<>();

    for( WayNode wayNode : way.getWayNodes() )
    {
      long nodeId = wayNode.getNodeId();

      nodeIds.add(nodeId);
    }

    for( Long nodeId : nodeIds )
    {
      Integer counter = nodeWayUsages.get(nodeId);

      if( counter == null ) counter = 1;
      else counter++;

      nodeWayUsages.put(nodeId, counter);
    }

    allWays.add(wayContainer);
  }

  @Override
  public void process(RelationContainer relation)
  {
  }
}
