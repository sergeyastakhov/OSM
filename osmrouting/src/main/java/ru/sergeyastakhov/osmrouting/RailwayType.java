/**
 * RailwayType.java
 * <p/>
 * Copyright (C) 2017 RNIC. All Rights Reserved
 */
package ru.sergeyastakhov.osmrouting;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public enum RailwayType
{
  RAIL(RailwayAccessMode.TRAIN),
  TRAM(RailwayAccessMode.TRAM);

  private RailwayAccessMode defaultAccessMode;

  RailwayType(RailwayAccessMode _defaultAccessMode)
  {
    defaultAccessMode = _defaultAccessMode;
  }

  public RailwayAccessMode getDefaultAccessMode()
  {
    return defaultAccessMode;
  }

  static final Map<String, RailwayType> typeMap = new HashMap<>();

  static
  {
    for( RailwayType RailwayType : RailwayType.values() )
    {
      typeMap.put(RailwayType.name().toLowerCase(), RailwayType);
    }
  }

  public static RailwayType fromRailwayValue(String railwayValue)
  {
    return typeMap.get(railwayValue);
  }

  public static RailwayType fromFerryValue(String ferryValue)
  {
    return fromRailwayValue(ferryValue);
  }

  public static RailwayType getWayType(Way way)
  {
    String railwayValue = null;
    boolean routeFerry = false;
    String ferryValue = null;
    String constructionValue = null;

    Collection<Tag> wayTags = way.getTags();
    for( Tag tag : wayTags )
    {
      String key = tag.getKey();
      String value = tag.getValue();

      switch( key )
      {
        case "railway":
          railwayValue = value;
          break;
        case "route":
          routeFerry = value.equals("ferry");
          break;
        case "ferry":
          ferryValue = value;
          break;
        case "construction":
          constructionValue = value;
          break;
      }
    }

    if( railwayValue != null )
    {
      if( railwayValue.equals("construction") )
        railwayValue = constructionValue;

      return fromRailwayValue(railwayValue);
    }

    if( routeFerry && ferryValue != null )
    {
      return fromFerryValue(ferryValue);
    }

    return null;
  }
}
