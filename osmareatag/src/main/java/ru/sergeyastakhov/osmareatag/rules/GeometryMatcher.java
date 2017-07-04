/**
 * GeometryMatcher.java
 * <p>
 *  Copyright (C) 2017 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmareatag.rules;

import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import ru.sergeyastakhov.osmareatag.EntityArea;
import ru.sergeyastakhov.osmareatag.EntityGeometryFactory;

import java.util.Collection;
import java.util.Map;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public interface GeometryMatcher
{
  Map<String, Collection<EntityArea>> matchGeometry
      (Entity entity, Map<String, String> tags, EntityGeometryFactory geometryFactory, Map<String, AreaRule> areaRuleMap);
}
