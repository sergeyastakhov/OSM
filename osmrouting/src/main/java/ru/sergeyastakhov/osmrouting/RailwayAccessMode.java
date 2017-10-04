/**
 * RailwayAccessMode.java
 * <p/>
 * Copyright (C) 2017 RNIC. All Rights Reserved
 */
package ru.sergeyastakhov.osmrouting;

import java.util.EnumMap;
import java.util.Map;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public enum RailwayAccessMode
{
  ACCESS(null),
  TRAIN(ACCESS),
  TRAM(ACCESS);

  private RailwayAccessMode parent;

  RailwayAccessMode(RailwayAccessMode _parent)
  {
    parent = _parent;
  }

  @Override
  public String toString() { return name().toLowerCase(); }

  public static RailwayAccessMode fromString(String name)
  {
    try
    {
      return valueOf(name.toUpperCase());
    }
    catch( IllegalArgumentException e )
    {
      return null;
    }
  }

  public static Map<RailwayAccessMode, String> restrictTags
      (Map<RailwayAccessMode, String> accessMap, Map<RailwayAccessMode, Boolean> restrictions)
  {
    Map<RailwayAccessMode, String> resultAccessMap = new EnumMap<>(RailwayAccessMode.class);

    for( Map.Entry<RailwayAccessMode, Boolean> entry : restrictions.entrySet() )
    {
      resultAccessMap.put(entry.getKey(), entry.getValue() ? "yes": "no");
    }

    for( Map.Entry<RailwayAccessMode, String> entry : accessMap.entrySet() )
    {
      RailwayAccessMode accessMode = entry.getKey();
      String accessValue = entry.getValue();

      String newAccessValue = accessMode.restrictBy(accessValue, restrictions);

      resultAccessMap.put(accessMode, newAccessValue);
    }

    return resultAccessMap;
  }

  private String restrictBy(String accessValue, Map<RailwayAccessMode, Boolean> restrictions)
  {
    for( Map.Entry<RailwayAccessMode, Boolean> entry : restrictions.entrySet() )
    {
      RailwayAccessMode mode = entry.getKey();
      boolean allowed = entry.getValue();

      if( !allowed && mode.isEqualsOrParentFor(this) )
      {
        accessValue = "no";
      }
    }

    return accessValue;
  }

  public boolean isEqualsOrParentFor(RailwayAccessMode accessMode)
  {
    while( accessMode!=null )
    {
      if( accessMode.equals(this) )
        return true;

      accessMode = accessMode.parent;
    }

    return false;
  }

  public boolean hasAnySubModeAccess(Map<RailwayAccessMode, String> accessMap)
  {
    boolean hasAccess = false;
    boolean hasRestriction = false;

    for( Map.Entry<RailwayAccessMode, String> entry : accessMap.entrySet() )
    {
      RailwayAccessMode accessMode = entry.getKey();
      String accessValue = entry.getValue();

      boolean allow = !accessValue.equals("no");

      if( !allow && accessMode.isEqualsOrParentFor(this) )
      {
        hasRestriction = true;
      }

      if( allow && isEqualsOrParentFor(accessMode) )
      {
        hasAccess = true;
      }
    }

    return !hasRestriction || hasAccess;
  }
}
