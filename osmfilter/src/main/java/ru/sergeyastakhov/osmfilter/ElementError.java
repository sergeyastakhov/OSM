/**
 * $Id$
 *
 * Copyright (C) 2012 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmfilter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

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

  public void writeTo(XMLStreamWriter writer) throws XMLStreamException
  {
    writer.writeStartElement("error");

    writer.writeAttribute("wayId", String.valueOf(way.getId()));
    writer.writeAttribute("errorType", String.valueOf(errorType));

    if( bound != null )
    {
      writer.writeStartElement("bound");

      writer.writeAttribute("right", String.valueOf(bound.getRight()));
      writer.writeAttribute("left", String.valueOf(bound.getLeft()));
      writer.writeAttribute("top", String.valueOf(bound.getTop()));
      writer.writeAttribute("bottom", String.valueOf(bound.getBottom()));

      writer.writeEndElement();
    }

    if( openingDate != null )
    {
      writer.writeStartElement("opening_date");
      writer.writeCharacters(openingDate);
      writer.writeEndElement();
    }

    if( checkDate != null )
    {
      writer.writeStartElement("check_date");
      writer.writeCharacters(checkDate);
      writer.writeEndElement();
    }

    writer.writeEndElement();
  }
}
