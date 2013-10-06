/**
 * ConversionListener.java
 *
 * Copyright (C) 2013 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osm2dcm;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public interface ConversionListener
{
  void conversionCompleted(MapConversionTask task);
}
