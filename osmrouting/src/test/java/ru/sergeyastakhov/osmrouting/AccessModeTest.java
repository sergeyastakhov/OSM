/**
 * $Id$
 *
 * Copyright (C) 2012 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmrouting;

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class AccessModeTest extends TestCase
{
  public void testEqualsOrParentFor()
  {
    assertTrue("Vehicle/PSV", AccessMode.VEHICLE.isEqualsOrParentFor(AccessMode.PSV));    
  }

  public void testRestrictTags()
  {
    Map<AccessMode, String> accessMap = new HashMap<>();

    Map<AccessMode, Boolean> onewayAccessMap = new HashMap<>();
    onewayAccessMap.put(AccessMode.VEHICLE, false);
    onewayAccessMap.put(AccessMode.PSV, true);

    Map<AccessMode, String> resultAccessMap = AccessMode.restrictTags(accessMap, onewayAccessMap);

    Map<AccessMode, String> testAccessMap = new HashMap<>();
    testAccessMap.put(AccessMode.VEHICLE, "no");
    testAccessMap.put(AccessMode.PSV, "yes");

    assertEquals(testAccessMap, resultAccessMap);
  }

  public void testSubModeAccess()
  {
    Map<AccessMode, String> testAccessMap = new HashMap<>();
    testAccessMap.put(AccessMode.VEHICLE, "no");
    testAccessMap.put(AccessMode.PSV, "yes");

    assertTrue("No access!", AccessMode.VEHICLE.hasAnySubModeAccess(testAccessMap));

    testAccessMap = new HashMap<>();
    testAccessMap.put(AccessMode.ACCESS, "no");
    testAccessMap.put(AccessMode.FOOT, "yes");

    assertFalse("Should be no access!", AccessMode.VEHICLE.hasAnySubModeAccess(testAccessMap));
  }
}
