/**
 * $Id$
 *
 * Copyright (C) 2010 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmfilter;

import java.util.logging.Logger;
import java.util.Collection;

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
public class UsedWayAndNodeFilter implements SinkSource, EntityProcessor
{
  private static final Logger log = Logger.getLogger(UsedWayAndNodeFilter.class.getName());

  private Sink sink;
  private SimpleObjectStore<NodeContainer> allNodes;
  private SimpleObjectStore<WayContainer> allWays;
  private SimpleObjectStore<RelationContainer> allRelations;
  private IdTracker requiredNodes;
  private IdTracker requiredWays;
  private boolean usedByTag;

  /**
   * Creates a new instance.
   *
   * @param idTrackerType Defines the id tracker implementation to use.
   */
  public UsedWayAndNodeFilter(IdTrackerType idTrackerType, boolean _usedByTag)
  {
    allNodes = new SimpleObjectStore<NodeContainer>(
          new SingleClassObjectSerializationFactory(NodeContainer.class), "afnd", true);
    allWays = new SimpleObjectStore<WayContainer>(
      new SingleClassObjectSerializationFactory(WayContainer.class), "afwy", true);
    allRelations = new SimpleObjectStore<RelationContainer>(
      new SingleClassObjectSerializationFactory(RelationContainer.class), "afrl", true);

    requiredNodes = IdTrackerFactory.createInstance(idTrackerType);
    requiredWays = IdTrackerFactory.createInstance(idTrackerType);
    usedByTag = _usedByTag;
  }


  public void process(EntityContainer entityContainer)
  {
    // Ask the entity container to invoke the appropriate processing method
    // for the entity type.
    entityContainer.process(this);
  }

  public void process(BoundContainer boundContainer)
  {
    // By default, pass it on unchanged
    sink.process(boundContainer);
  }


  public void process(NodeContainer container)
  {
    allNodes.add(container);
  }


  public void process(WayContainer container)
  {
    allWays.add(container);
  }

  public void process(RelationContainer container)
  {
    for( RelationMember member : container.getEntity().getMembers() )
    {
      EntityType memberType = member.getMemberType();

      if( memberType.equals(EntityType.Way) )
      {
        requiredWays.set(member.getMemberId());
      }
      else if( memberType.equals(EntityType.Node) )
      {
        requiredNodes.set(member.getMemberId());
      }        
    }

    allRelations.add(container);
  }


  public void complete()
  {
    log.info("Send on all required nodes");
    sendNodes();

    log.info("Send on all required ways");
    sendWays();

    log.info("Send on all relations"); 
    sendRelations();

    // done
    sink.complete();
  }

  private boolean isEmptyTagSet(Collection<Tag> tags)
  {
    if( tags.isEmpty() )
      return true;

    return tags.size()==1 && tags.iterator().next().getKey().equals("created_by");
  }

  private boolean isRequiredNode(Node node)
  {
    long nodeId = node.getId();

    if( requiredNodes.get(nodeId) )
      return true;

    return usedByTag && !isEmptyTagSet(node.getTags());
  }

  private void sendNodes()
  {
    // mark all nodes as required
    ReleasableIterator<WayContainer> wayIterator = allWays.iterate();
    while( wayIterator.hasNext() )
    {
      Way way = wayIterator.next().getEntity();

      if( !isRequiredWay(way) )
        continue;

      for( WayNode nodeReference : way.getWayNodes() )
      {
        long nodeId = nodeReference.getNodeId();
        requiredNodes.set(nodeId);
      }
    }
    wayIterator.release();

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
  }

  private boolean isRequiredWay(Way way)
  {
    long wayId = way.getId();

    if( requiredWays.get(wayId) )
      return true;

    return usedByTag && !isEmptyTagSet(way.getTags());
  }

  private void sendWays()
  {
    ReleasableIterator<WayContainer> wayIterator = allWays.iterate();
    while( wayIterator.hasNext() )
    {
      WayContainer wayContainer = wayIterator.next();

      if( !isRequiredWay(wayContainer.getEntity()) )
        continue;

      sink.process(wayContainer);
    }
    wayIterator.release();
  }

  private void sendRelations()
  {
    ReleasableIterator<RelationContainer> relationIterator = allRelations.iterate();
    while( relationIterator.hasNext() )
    {
      sink.process(relationIterator.next());
    }
    relationIterator.release();
  }

  public void release()
  {
    if( allNodes != null )
    {
      allNodes.release();
    }
    if( allWays != null )
    {
      allWays.release();
    }
    if( allRelations != null )
    {
      allRelations.release();
    }
    sink.release();
  }


  public void setSink(Sink sink)
  {
    this.sink = sink;
	}
}
