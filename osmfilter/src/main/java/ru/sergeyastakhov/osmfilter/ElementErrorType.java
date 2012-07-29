/**
 * $Id$
 *
 * Copyright (C) 2012 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmfilter;

import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public enum ElementErrorType
{
  OPENING_DATE_FORMAT_ERROR,
  CHECK_DATE_FORMAT_ERROR,
  OPENING_DATE_PASSED,
  CHECK_DATE_TOO_OLD;

  public void writeTo(XMLStreamWriter writer, int counter) throws XMLStreamException
  {
    writer.writeStartElement(name().toLowerCase());
    writer.writeCharacters(String.valueOf(counter));
    writer.writeEndElement();
  }
}
