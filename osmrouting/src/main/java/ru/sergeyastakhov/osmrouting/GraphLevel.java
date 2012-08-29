/**
 * $Id$
 *
 * Copyright (C) 2012 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmrouting;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
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

  private static final Map<String, GraphLevel> highwayLevels;

  static
  {
    highwayLevels = new HashMap<String, GraphLevel>();
    highwayLevels.put("motorway", TRUNK);
    highwayLevels.put("motorway_link", TRUNK);
    highwayLevels.put("trunk", TRUNK);
    highwayLevels.put("trunk_link", TRUNK);

    highwayLevels.put("primary", PRIMARY);
    highwayLevels.put("primary_link", PRIMARY);

    highwayLevels.put("secondary", SECONDARY);
    highwayLevels.put("secondary_link", SECONDARY);

    highwayLevels.put("tertiary", TERTIARY);
    highwayLevels.put("tertiary_link", TERTIARY);

    highwayLevels.put("unclassified", RESIDENTIAL);
    highwayLevels.put("residential", RESIDENTIAL);

    highwayLevels.put("service", SERVICE);

    highwayLevels.put("track", TRACK);

    highwayLevels.put("pedestrian", FOOTWAY);
    highwayLevels.put("footway", FOOTWAY);
    highwayLevels.put("steps", FOOTWAY);
    highwayLevels.put("path", FOOTWAY);
  }


  public static GraphLevel fromHighwayValue(String highwayValue)
  {
    return highwayLevels.get(highwayValue);
  }

  public static GraphLevel fromFerryValue(String ferryValue)
  {
    return fromHighwayValue(ferryValue);
  }

  public static GraphLevel getWayLevel(Way way)
  {
    String highwayValue = null;
    boolean routeFerry = false;
    String ferryValue = null;

    Collection<Tag> wayTags = way.getTags();
    for( Tag tag : wayTags )
    {
      String key = tag.getKey();
      String value = tag.getValue();

      if( key.equals("highway") )
      {
        highwayValue = value;
      }
      else if( key.equals("route") )
      {
        routeFerry = value.equals("ferry");
      }
      else if( key.equals("ferry") )
      {
        ferryValue = value;
      }
    }

    if( highwayValue != null )
    {
      return fromHighwayValue(highwayValue);
    }

    if( routeFerry && ferryValue!=null )
    {
      return fromFerryValue(ferryValue);
    }

    return null;
  }
}
