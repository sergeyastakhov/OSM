/**
 * $Id$
 *
 * Copyright (C) 2012 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmfilter;

import java.util.HashMap;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class ErrorsSummary
{
  private int total;
  private Map<ElementErrorType, Integer> typedCounters = new HashMap<ElementErrorType, Integer>();

  public void newError(ElementErrorType errorType)
  {
    Integer counter = typedCounters.get(errorType);

    if( counter == null )
      counter = 1;
    else
      counter++;

    typedCounters.put(errorType, counter);

    total++;
  }

  public void writeTo(XMLStreamWriter writer) throws XMLStreamException
  {
    writer.writeStartElement("summary");

    writer.writeStartElement("total");
    writer.writeCharacters(String.valueOf(total));
    writer.writeEndElement();

    for( ElementErrorType errorType : ElementErrorType.values() )
    {
      Integer counter = typedCounters.get(errorType);

      if( counter==null ) counter = 0;

      errorType.writeTo(writer, counter);
    }

    writer.writeEndElement();
  }
}
