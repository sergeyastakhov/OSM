/**
 * $Id$
 *
 * Copyright (C) 2012-2017 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmrouting;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

import java.util.*;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class GraphSet
{
  private GraphLevel graphLevel;
  private List<RoutingGraph> graphs = new ArrayList<>();

  private Map<Long, RoutingGraph> graphsByNodes = new HashMap<>();

  public GraphSet(GraphLevel _graphLevel)
  {
    graphLevel = _graphLevel;
  }

  public GraphLevel getGraphLevel()
  {
    return graphLevel;
  }

  private List<RoutingGraph> getRoutingGraphsByNodes(List<WayNode> nodes)
  {
    Set<RoutingGraph> result = new HashSet<>();

    for( WayNode wayNode : nodes )
    {
      long nodeId = wayNode.getNodeId();

      RoutingGraph routingGraph = graphsByNodes.get(nodeId);
      if( routingGraph != null )
      {
        result.add(routingGraph);
      }
    }

    List<RoutingGraph> resultList = new ArrayList<>(result);

    if( resultList.size() > 1 )
    {
      Collections.sort(resultList, Collections.reverseOrder(RoutingGraph.BY_SIZE));
    }

    return resultList;
  }

  public void add(Way way)
  {
    List<RoutingGraph> wayGraphs = getRoutingGraphsByNodes(way.getWayNodes());

    RoutingGraph resultGraph;

    if( wayGraphs.isEmpty() )
    {
      // Not connected to other graphs

      resultGraph = new RoutingGraph();
      graphs.add(resultGraph);
    }
    else if( wayGraphs.size() == 1 )
    {
      // Single graph
      resultGraph = wayGraphs.get(0);
    }
    else
    {
      // Bridge way - connected to several graphs, merging them
      resultGraph = wayGraphs.remove(0);

      for( RoutingGraph wayGraph : wayGraphs )
      {
        resultGraph.merge(wayGraph);
        graphs.remove(wayGraph);

        for( Long nodeId : wayGraph.getNodes() )
        {
          graphsByNodes.put(nodeId, resultGraph);
        }
      }
    }

    resultGraph.addWay(way);

    for( WayNode wayNode : way.getWayNodes() )
    {
      long nodeId = wayNode.getNodeId();

      graphsByNodes.put(nodeId, resultGraph);
    }
  }

  public void sortGraphs()
  {
    Collections.sort(graphs, Collections.reverseOrder(RoutingGraph.BY_SIZE));
  }


  public RoutingGraph getMaxGraph()
  {
    sortGraphs();
    return graphs.get(0);
  }

  public int getWayCount()
  {
    int totalWays = 0;

    for( RoutingGraph graph : graphs )
    {
      totalWays += graph.getWayCount();
    }

    return totalWays;
  }

  @Override
  public String toString()
  {
    return "GraphSet{" +
      "graphLevel=" + graphLevel +
      ", graphs count=" + graphs.size() +
      ", graphs=" + graphs +
      '}';
  }

  public boolean isLevelLessThan(GraphLevel testLevel)
  {
    return graphLevel.compareTo(testLevel) < 0;
  }

  public void markWayMinority(Way way, GraphLevel wayLevel)
  {
    if( isLevelLessThan(wayLevel) )
      return;

    RoutingGraph levelMaxGraph = getMaxGraph();

    boolean levelMaxGraphWay = levelMaxGraph.containsWay(way.getId());

    if( levelMaxGraphWay )
      return;

    String minorTagKey = "minor_graph:" + graphLevel.name().toLowerCase();

    Collection<Tag> tags = way.getTags();
    tags.add(new Tag(minorTagKey, "yes"));
  }
}
