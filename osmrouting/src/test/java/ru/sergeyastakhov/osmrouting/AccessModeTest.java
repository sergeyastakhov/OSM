/**
 * $Id$
 *
 * Copyright (C) 2012 CSBI. All Rights Reserved
 */
package ru.sergeyastakhov.osmrouting;

import java.util.Map;
import java.util.HashMap;

import junit.framework.TestCase;

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
    Map<AccessMode, String> accessMap = new HashMap<AccessMode, String>();

    Map<AccessMode, Boolean> onewayAccessMap = new HashMap<AccessMode, Boolean>();
    onewayAccessMap.put(AccessMode.VEHICLE, false);
    onewayAccessMap.put(AccessMode.PSV, true);

    Map<AccessMode, String> resultAccessMap = AccessMode.restrictTags(accessMap, onewayAccessMap);

    Map<AccessMode, String> testAccessMap = new HashMap<AccessMode, String>();
    testAccessMap.put(AccessMode.VEHICLE, "no");
    testAccessMap.put(AccessMode.PSV, "yes");

    assertEquals(testAccessMap, resultAccessMap);
  }

  public void testSubModeAccess()
  {
    Map<AccessMode, String> testAccessMap = new HashMap<AccessMode, String>();
    testAccessMap.put(AccessMode.VEHICLE, "no");
    testAccessMap.put(AccessMode.PSV, "yes");

    assertTrue("No access!", AccessMode.VEHICLE.hasAnySubModeAccess(testAccessMap));

    testAccessMap = new HashMap<AccessMode, String>();
    testAccessMap.put(AccessMode.ACCESS, "no");
    testAccessMap.put(AccessMode.FOOT, "yes");

    assertFalse("Should be no access!", AccessMode.VEHICLE.hasAnySubModeAccess(testAccessMap));
  }
}
