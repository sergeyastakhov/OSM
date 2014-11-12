/**
 * $Id$
 *
 * Copyright (C) 2012 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmrouting;

import org.openstreetmap.osmosis.core.domain.v0_6.Way;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public enum GraphLevel
{
  TRUNK,
  PRIMARY,
  SECONDARY,
  TERTIARY,
  RESIDENTIAL,
  SERVICE,
  TRACK,
  FOOTWAY;

  public static GraphLevel getWayLevel(Way way)
  {
    HighwayType highwayType = HighwayType.getWayType(way);
    return highwayType != null ? highwayType.getLevel() : null;
  }
}
