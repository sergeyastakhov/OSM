/**
 * $Id$
 *
 * Copyright (C) 2014 CSBI. All Rights Reserved
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
public enum HighwayType
{
  MOTORWAY(GraphLevel.TRUNK, true),
  MOTORWAY_LINK(GraphLevel.TRUNK, true),
  TRUNK(GraphLevel.TRUNK),
  TRUNK_LINK(GraphLevel.TRUNK),
  PRIMARY(GraphLevel.PRIMARY),
  PRIMARY_LINK(GraphLevel.PRIMARY),
  SECONDARY(GraphLevel.SECONDARY),
  SECONDARY_LINK(GraphLevel.SECONDARY),
  TERTIARY(GraphLevel.TERTIARY),
  TERTIARY_LINK(GraphLevel.TERTIARY),
  UNCLASSIFIED(GraphLevel.RESIDENTIAL),
  RESIDENTIAL(GraphLevel.RESIDENTIAL),
  SERVICE(GraphLevel.SERVICE),
  TRACK(GraphLevel.TRACK),
  PEDESTRIAN(GraphLevel.FOOTWAY),
  FOOTWAY(GraphLevel.FOOTWAY),
  STEPS(GraphLevel.FOOTWAY),
  PATH(GraphLevel.FOOTWAY);

  static final Map<String, HighwayType> typeMap = new HashMap<String, HighwayType>();

  static
  {
    for( HighwayType highwayType : HighwayType.values() )
    {
      typeMap.put(highwayType.name().toLowerCase(), highwayType);
    }
  }

  private GraphLevel level;
  private boolean oneway;

  private HighwayType(GraphLevel _level)
  {
    level = _level;
  }

  private HighwayType(GraphLevel _level, boolean _oneway)
  {
    level = _level;
    oneway = _oneway;
  }

  public GraphLevel getLevel()
  {
    return level;
  }

  public boolean isOneway()
  {
    return oneway;
  }

  public static HighwayType fromHighwayValue(String highwayValue)
  {
    return typeMap.get(highwayValue);
  }

  public static HighwayType fromFerryValue(String ferryValue)
  {
    return fromHighwayValue(ferryValue);
  }

  public static HighwayType getWayType(Way way)
  {
    String highwayValue = null;
    boolean routeFerry = false;
    String ferryValue = null;
    String constructionValue = null;

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
      else if( key.equals("construction") )
      {
        constructionValue = value;
      }
    }

    if( highwayValue != null )
    {
      if( highwayValue.equals("construction") )
        highwayValue = constructionValue;

      return fromHighwayValue(highwayValue);
    }

    if( routeFerry && ferryValue != null )
    {
      return fromFerryValue(ferryValue);
    }

    return null;
  }
}
