/**
 * $Id$
 * <p>
 * Copyright (C) 2012-2017 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmrouting;

import org.openstreetmap.osmosis.core.container.v0_6.*;
import org.openstreetmap.osmosis.core.domain.v0_6.*;
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
  private SimpleObjectStore<RelationContainer> allRelations;

  private Map<Long, Integer> nodeWayUsages = new HashMap<>();

  private IdFactory wayIdFactory = EntityIdFactory.wayIdFactory;

  private Map<Long, Set<IdReplacement>> wayIdReplacement = new HashMap<>();

  public SplitRoutingGraphTask() {}

  @Override
  public void initialize(Map<String, Object> metaData)
  {
    allWays = new SimpleObjectStore<>(
        new SingleClassObjectSerializationFactory(WayContainer.class), "srgt_wy", true);
    allRelations = new SimpleObjectStore<>(
        new SingleClassObjectSerializationFactory(RelationContainer.class), "srgt_rl", true);

    sink.initialize(metaData);
  }

  @Override
  public void process(EntityContainer entityContainer)
  {
    entityContainer.process(this);
  }

  @Override
  public void complete()
  {
    Way lastNewWay = null;

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

          lastNewWay = newWay;

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

        lastNewWay = newWay;
      }

      if( lastNewWay != null )
      {
        List<WayNode> wayNodes = lastNewWay.getWayNodes();
        WayNode lastNode = wayNodes.get(wayNodes.size() - 1);

        long lastNodeId = lastNode.getNodeId();
        Set<IdReplacement> idReplacements = wayIdReplacement.get(lastNodeId);
        if( idReplacements==null )
        {
          idReplacements = new HashSet<>();
          wayIdReplacement.put(lastNodeId, idReplacements);
        }
        idReplacements.add(new IdReplacement(way.getId(), lastNewWay.getId()));
      }
    }

    wayIterator.close();

    allWays.complete();

    ReleasableIterator<RelationContainer> relationIterator = allRelations.iterate();

    while( relationIterator.hasNext() )
    {
      RelationContainer relationContainer = relationIterator.next();

      Relation relation = relationContainer.getEntity();

      // Replace way id in restrictions

      RelationMember viaNode = null;

      for( RelationMember member : relation.getMembers() )
      {
        String role = member.getMemberRole();

        if( role.equals("via") && member.getMemberType() == EntityType.Node )
          viaNode = member;
      }

      if( viaNode != null )
      {
        Set<IdReplacement> idReplacements = wayIdReplacement.get(viaNode.getMemberId());
        if( idReplacements != null )
        {
          relation = relation.getWriteableInstance();

          List<RelationMember> members = relation.getMembers();

          for( int i = 0; i < members.size(); i++ )
          {
            RelationMember member = members.get(i);

            if( member.getMemberType() == EntityType.Way )
            {
              for( IdReplacement idReplacement : idReplacements )
              {
                if( member.getMemberId() == idReplacement.originalId )
                {
                  members.set(i,
                      new RelationMember(idReplacement.newId, member.getMemberType(), member.getMemberRole()));
                  break;
                }
              }
            }
          }

          relationContainer = new RelationContainer(relation);
        }
      }

      sink.process(relationContainer);
    }

    relationIterator.close();

    allRelations.complete();

    sink.complete();
  }

  @Override
  public void close()
  {
    allWays.close();
    allRelations.close();

    nodeWayUsages.clear();
    wayIdReplacement.clear();

    sink.close();
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
  public void process(RelationContainer relationContainer)
  {
    allRelations.add(relationContainer);
  }
}
