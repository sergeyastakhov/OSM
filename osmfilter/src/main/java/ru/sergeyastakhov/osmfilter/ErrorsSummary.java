/**
 * $Id$
 *
 * Copyright (C) 2012 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmfilter;

import java.util.Map;
import java.util.HashMap;
import java.io.PrintWriter;

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

  public void writeTo(PrintWriter writer)
  {
    writer.println("<summary>");

    writer.println("<total>"+total+"</total>");

    for( ElementErrorType errorType : ElementErrorType.values() )
    {
      Integer counter = typedCounters.get(errorType);

      if( counter==null ) counter = 0;

      writer.println(errorType.toXMLTag(counter));
    }

    writer.println("</summary>");
  }
}
