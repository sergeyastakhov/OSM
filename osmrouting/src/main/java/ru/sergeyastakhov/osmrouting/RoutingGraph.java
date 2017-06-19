/**
 * $Id$
 *
 * Copyright (C) 2012-2017 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmrouting;

import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.filter.common.IdTracker;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerFactory;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerType;

import java.util.Comparator;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class RoutingGraph
{
  public static final Comparator<RoutingGraph> BY_SIZE = new Comparator<RoutingGraph>()
  {
    @Override
    public int compare(RoutingGraph g1, RoutingGraph g2)
    {
      return g1.wayCount - g2.wayCount;
    }
  };

  private IdTracker nodes;
  private IdTracker ways;

  private int wayCount;

  public RoutingGraph()
  {
    nodes = IdTrackerFactory.createInstance(IdTrackerType.Dynamic);
    ways = IdTrackerFactory.createInstance(IdTrackerType.Dynamic);
  }

  public boolean containsNode(long nodeId)
  {
    return nodes.get(nodeId);
  }

  public boolean containsWay(long wayId)
  {
    return ways.get(wayId);
  }

  public void addWay(Way way)
  {
    for( WayNode wayNode : way.getWayNodes() )
    {
      long nodeId = wayNode.getNodeId();
      nodes.set(nodeId);
    }

    ways.set(way.getId());

    wayCount++;
  }

  public void merge(RoutingGraph otherGraph)
  {
    nodes.setAll(otherGraph.nodes);
    ways.setAll(otherGraph.ways);

    wayCount += otherGraph.wayCount;
  }

  public IdTracker getNodes()
  {
    return nodes;
  }

  public IdTracker getWays()
  {
    return ways;
  }

  public int getWayCount()
  {
    return wayCount;
  }

  @Override
  public String toString()
  {
    return "RoutingGraph{" +
      "wayCount=" + wayCount +
      '}';
  }
}
