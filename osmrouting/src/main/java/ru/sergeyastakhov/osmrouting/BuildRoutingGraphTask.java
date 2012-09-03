/**
 * $Id$
 *
 * Copyright (C) 2012 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmrouting;

import java.util.Collection;
import java.util.EnumMap;
import java.util.logging.Logger;

import org.openstreetmap.osmosis.core.container.v0_6.*;
import org.openstreetmap.osmosis.core.domain.v0_6.*;
import org.openstreetmap.osmosis.core.filter.common.IdTracker;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerFactory;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerType;
import org.openstreetmap.osmosis.core.lifecycle.ReleasableIterator;
import org.openstreetmap.osmosis.core.store.SimpleObjectStore;
import org.openstreetmap.osmosis.core.store.SingleClassObjectSerializationFactory;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.core.task.v0_6.SinkSource;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class BuildRoutingGraphTask implements SinkSource, EntityProcessor
{
  private static final Logger log = Logger.getLogger(BuildRoutingGraphTask.class.getName());

  private Sink sink;

  private IdTrackerType idTrackerType;
  private GraphLevel graphLevel;
  private MinorGraphsAction minorGraphsAction;

  private SimpleObjectStore<NodeContainer> allNodes;
  private SimpleObjectStore<WayContainer> routingWays;
  private SimpleObjectStore<RelationContainer> routingRelations;

  private IdTracker requiredNodes;

  private EnumMap<GraphLevel, GraphSet> graphs = new EnumMap<GraphLevel, GraphSet>(GraphLevel.class);

  public BuildRoutingGraphTask
    (IdTrackerType _idTrackerType, GraphLevel _graphLevel, MinorGraphsAction _minorGraphsAction)
  {
    idTrackerType = _idTrackerType;

    graphLevel = _graphLevel;

    minorGraphsAction = _minorGraphsAction;

    allNodes = new SimpleObjectStore<NodeContainer>(
      new SingleClassObjectSerializationFactory(NodeContainer.class), "brgt_nd", true);
    routingWays = new SimpleObjectStore<WayContainer>(
      new SingleClassObjectSerializationFactory(WayContainer.class), "brgt_wy", true);
    routingRelations = new SimpleObjectStore<RelationContainer>(
      new SingleClassObjectSerializationFactory(RelationContainer.class), "brgt_rl", true);

    requiredNodes = IdTrackerFactory.createInstance(idTrackerType);
  }

  @Override
  public void process(EntityContainer entityContainer)
  {
    entityContainer.process(this);
  }

  @Override
  public void complete()
  {
    if( graphs.isEmpty() )
    {
      log.warning("No graphs found, nothing to do!");

      sink.complete();

      return;
    }

    for( GraphSet graphSet : graphs.values() )
    {
      graphSet.sortGraphs();
      log.info("GraphSet: " + graphSet);
    }

    GraphLevel maxLevel = GraphLevel.values()[graphs.size() - 1];

    log.info("Max level: " + maxLevel);

    GraphSet maxLevelGraphSet = graphs.get(maxLevel);

    log.info("Total ways at max level: " + maxLevelGraphSet.getWayCount());

    RoutingGraph maxGraph = maxLevelGraphSet.getMaxGraph();

    log.info("Max size graph: " + maxGraph.getWayCount() + " ways");

    log.info("Send on all required nodes");

    ReleasableIterator<NodeContainer> nodeIterator = allNodes.iterate();
    while( nodeIterator.hasNext() )
    {
      NodeContainer nodeContainer = nodeIterator.next();

      Node node = nodeContainer.getEntity();
      long nodeId = node.getId();

      if( !requiredNodes.get(nodeId) )
      {
        continue;
      }

      if( minorGraphsAction == MinorGraphsAction.PASS || minorGraphsAction == MinorGraphsAction.MARK )
      {
      }
      else
      {
        boolean maxGraphNode = maxGraph.containsNode(nodeId);

        if( minorGraphsAction == MinorGraphsAction.DROP )
        {
          if( !maxGraphNode )
            continue;
        }
        else if( minorGraphsAction == MinorGraphsAction.ONLY )
        {
          if( maxGraphNode )
            continue;
        }
      }

      sink.process(nodeContainer);
    }

    nodeIterator.release();

    allNodes.release();

    requiredNodes = null;

    log.info("Send on all required ways");

    IdTracker outputWays = IdTrackerFactory.createInstance(idTrackerType);

    ReleasableIterator<WayContainer> wayIterator = routingWays.iterate();
    while( wayIterator.hasNext() )
    {
      WayContainer wayContainer = wayIterator.next();
      Way way = wayContainer.getEntity();

      if( minorGraphsAction == MinorGraphsAction.PASS )
      {
      }
      else
      {
        boolean maxGraphWay = maxGraph.containsWay(way.getId());

        if( minorGraphsAction == MinorGraphsAction.DROP )
        {
          if( !maxGraphWay )
            continue;
        }
        else if( minorGraphsAction == MinorGraphsAction.ONLY )
        {
          if( maxGraphWay )
            continue;
        }
        else if( minorGraphsAction == MinorGraphsAction.MARK )
        {
          wayContainer = wayContainer.getWriteableInstance();

          GraphLevel wayLevel = GraphLevel.getWayLevel(way);

          for( GraphSet graphSet : graphs.values() )
          {
            graphSet.markWayMinority(wayContainer.getEntity(), wayLevel);
          }
        }
      }

      sink.process(wayContainer);

      outputWays.set(way.getId());
    }

    wayIterator.release();

    routingWays.release();

    log.info("Send on all required relations");

    ReleasableIterator<RelationContainer> relationIterator = routingRelations.iterate();
    while( relationIterator.hasNext() )
    {
      RelationContainer relationContainer = relationIterator.next();
      Relation relation = relationContainer.getEntity();

      boolean usedRelation = false;

      for( RelationMember member : relation.getMembers() )
      {
        if( member.getMemberType() == EntityType.Way && outputWays.get(member.getMemberId()) )
        {
          usedRelation = true;
          break;
        }
      }

      if( usedRelation )
      {
        sink.process(relationContainer);
      }
    }

    relationIterator.release();

    routingRelations.release();

    log.info("Sending complete.");

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
  public void process(NodeContainer nodeContainer)
  {
    allNodes.add(nodeContainer);
  }

  @Override
  public void process(WayContainer wayContainer)
  {
    Way way = wayContainer.getEntity();

    GraphLevel wayLevel = GraphLevel.getWayLevel(way);

    if( wayLevel == null )
      return;

    if( graphLevel.compareTo(wayLevel) < 0 )
      return;

    for( WayNode wayNode : way.getWayNodes() )
    {
      long nodeId = wayNode.getNodeId();
      requiredNodes.set(nodeId);
    }

    for( GraphLevel level : GraphLevel.values() )
    {
      if( level.compareTo(graphLevel) > 0 )
        break;

      if( level.compareTo(wayLevel) < 0 )
        continue;

      GraphSet graphSet = graphs.get(level);

      if( graphSet == null )
      {
        graphSet = new GraphSet(idTrackerType, level);
        graphs.put(level, graphSet);
      }

      graphSet.add(way);
    }

    routingWays.add(wayContainer);
  }

  @Override
  public void process(RelationContainer relationContainer)
  {
    Relation relation = relationContainer.getEntity();

    Collection<Tag> relationTags = relation.getTags();
    for( Tag tag : relationTags )
    {
      String key = tag.getKey();
      String value = tag.getValue();

      if( key.equals("type") && value.equals("restriction") )
      {
        routingRelations.add(relationContainer);
        break;
      }
    }
  }
}
