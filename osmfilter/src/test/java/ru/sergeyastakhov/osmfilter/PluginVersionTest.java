/**
 * PluginVersionTest.java
 *
 * Copyright (C) 2013 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmfilter;

import junit.framework.TestCase;
import org.java.plugin.registry.Version;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class PluginVersionTest extends TestCase
{
  public void testVersionCompatible()
  {
    Version coreVersion = Version.parse("0.42-6-gf39a160-dirty");
    Version requiredCoreVersion = Version.parse("0.42");

//    assertTrue("Not compatible", coreVersion.isCompatibleWith(requiredCoreVersion));
  }
}
