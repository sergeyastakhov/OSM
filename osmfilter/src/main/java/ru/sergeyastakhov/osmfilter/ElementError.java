/**
 * $Id$
 *
 * Copyright (C) 2012 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmfilter;

import java.io.PrintWriter;

import org.openstreetmap.osmosis.core.domain.v0_6.Bound;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class ElementError
{
  private Way way;
  private ElementErrorType errorType;
  private Bound bound;
  private String openingDate;
  private String checkDate;

  public ElementError(Way _way, ElementErrorType _errorType, String _openingDate, String _checkDate)
  {
    way = _way;
    errorType = _errorType;
    openingDate = _openingDate;
    checkDate = _checkDate;
  }

  public void updateBound(Node node)
  {
    for( WayNode wayNode : way.getWayNodes() )
    {
      if( node.getId() == wayNode.getNodeId() )
      {
        Bound nodeBound = new Bound
          (node.getLongitude(), node.getLongitude(), node.getLatitude(), node.getLatitude(), "ErrorWayBound");

        if( bound == null )
          bound = nodeBound;
        else
          bound = bound.union(nodeBound);
      }
    }
  }

  public void writeTo(PrintWriter writer)
  {
    writer.println("<error wayId=\"" + way.getId() + "\" errorType=\"" + errorType + "\">");

    if( bound != null )
    {
      writer.println("<bound right=\"" + bound.getRight() + "\" left=\"" + bound.getLeft()
        + "\" top=\"" + bound.getTop() + "\" bottom=\"" + bound.getBottom() + "\"/>");
    }

    if( openingDate != null )
    {
      writer.println("<opening_date>" + openingDate + "</opening_date>");
    }

    if( checkDate != null )
    {
      writer.println("<check_date>" + checkDate + "</check_date>");
    }

    writer.println("</error>");
  }
}
