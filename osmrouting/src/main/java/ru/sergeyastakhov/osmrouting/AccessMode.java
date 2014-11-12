/**
 * $Id$
 *
 * Copyright (C) 2012 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmrouting;

import java.util.EnumMap;
import java.util.Map;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public enum AccessMode
{
  ACCESS(null),
  FOOT(ACCESS),
  VEHICLE(ACCESS),
  BICYCLE(VEHICLE),
  MOTOR_VEHICLE(VEHICLE),
  MOTORCYCLE(MOTOR_VEHICLE),
  MOPED(MOTOR_VEHICLE),
  MOTORCAR(MOTOR_VEHICLE),
  PSV(MOTOR_VEHICLE),
  BUS(PSV),
  TAXI(PSV),
  HGV(MOTOR_VEHICLE),
  EMERGENCY(ACCESS);

  private AccessMode parent;

  AccessMode(AccessMode _parent)
  {
    parent = _parent;
  }

  @Override
  public String toString()
  {
    return name().toLowerCase();
  }

  public static AccessMode fromString(String name)
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

  public static Map<AccessMode, String> restrictTags
     (Map<AccessMode, String> accessMap, Map<AccessMode, Boolean> restrictions)
  {
    Map<AccessMode, String> resultAccessMap = new EnumMap<AccessMode, String>(AccessMode.class);

    for( Map.Entry<AccessMode, Boolean> entry : restrictions.entrySet() )
    {
      resultAccessMap.put(entry.getKey(), entry.getValue() ? "yes": "no");
    }

    for( Map.Entry<AccessMode, String> entry : accessMap.entrySet() )
    {
      AccessMode accessMode = entry.getKey();
      String accessValue = entry.getValue();

      String newAccessValue = accessMode.restrictBy(accessValue, restrictions);

      resultAccessMap.put(accessMode, newAccessValue);
    }

    return resultAccessMap;
  }

  private String restrictBy(String accessValue, Map<AccessMode, Boolean> restrictions)
  {
    for( Map.Entry<AccessMode, Boolean> entry : restrictions.entrySet() )
    {
      AccessMode mode = entry.getKey();
      boolean allowed = entry.getValue();

      if( !allowed && mode.isEqualsOrParentFor(this) )
      {
        accessValue = "no";
      }
    }

    return accessValue;
  }

  public boolean isEqualsOrParentFor(AccessMode accessMode)
  {
    while( accessMode!=null )
    {
      if( accessMode.equals(this) )
        return true;

      accessMode = accessMode.parent;
    }

    return false;
  }

  public boolean hasAnySubModeAccess(Map<AccessMode, String> accessMap)
  {
    boolean hasAccess = false;
    boolean hasRestriction = false;

    for( Map.Entry<AccessMode, String> entry : accessMap.entrySet() )
    {
      AccessMode accessMode = entry.getKey();
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
