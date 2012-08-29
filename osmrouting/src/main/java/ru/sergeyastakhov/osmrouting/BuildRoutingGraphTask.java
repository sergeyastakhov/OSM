/**
 * $Id$
 *
 * Copyright (C) 2012 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmrouting;

import java.util.logging.Logger;

import org.openstreetmap.osmosis.core.container.v0_6.*;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
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
  private GraphLevel graphLevel;

  private SimpleObjectStore<NodeContainer> allNodes;
  private SimpleObjectStore<WayContainer> routingWays;

  private IdTracker requiredNodes;

  public BuildRoutingGraphTask(IdTrackerType idTrackerType, GraphLevel _graphLevel)
  {
    allNodes = new SimpleObjectStore<NodeContainer>(
      new SingleClassObjectSerializationFactory(NodeContainer.class), "brgt_nd", true);
    routingWays = new SimpleObjectStore<WayContainer>(
      new SingleClassObjectSerializationFactory(WayContainer.class), "brgt_wy", true);

    requiredNodes = IdTrackerFactory.createInstance(idTrackerType);

    graphLevel = _graphLevel;
  }

  @Override
  public void process(EntityContainer entityContainer)
  {
    entityContainer.process(this);
  }

  @Override
  public void complete()
  {
    log.info("Send on all required nodes");

    ReleasableIterator<NodeContainer> nodeIterator = allNodes.iterate();
    while( nodeIterator.hasNext() )
    {
      NodeContainer nodeContainer = nodeIterator.next();

      if( !isRequiredNode(nodeContainer.getEntity()) )
      {
        continue;
      }

      sink.process(nodeContainer);
    }

    nodeIterator.release();

    log.info("Send on all required ways");

    ReleasableIterator<WayContainer> wayIterator = routingWays.iterate();
    while( wayIterator.hasNext() )
    {
      WayContainer wayContainer = wayIterator.next();

      sink.process(wayContainer);
    }
    wayIterator.release();

    sink.complete();
  }

  private boolean isRequiredNode(Node node)
  {
    long nodeId = node.getId();

    return requiredNodes.get(nodeId);
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
    allNodes.add(node);
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

    for( WayNode nodeReference : way.getWayNodes() )
    {
      long nodeId = nodeReference.getNodeId();
      requiredNodes.set(nodeId);
    }

    routingWays.add(wayContainer);
  }

  @Override
  public void process(RelationContainer relation)
  {
  }
}
