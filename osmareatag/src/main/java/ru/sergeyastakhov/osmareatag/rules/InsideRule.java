/**
 * InsideRule.java
 * <p>
 * Copyright (C) 2017 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmareatag.rules;

import com.vividsolutions.jts.geom.Geometry;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import ru.sergeyastakhov.osmareatag.EntityArea;
import ru.sergeyastakhov.osmareatag.EntityGeometryFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class InsideRule implements GeometryMatcher
{
  private static final Logger log = Logger.getLogger(InsideRule.class.getName());

  private String area;

  public InsideRule(String _area) { area = _area; }

  @Override
  public Map<String, Collection<EntityArea>> matchGeometry
      (Entity entity, Map<String, String> tags,
       EntityGeometryFactory geometryFactory,
       Map<String, AreaRule> areaRuleMap)
  {
    AreaRule areaRule = areaRuleMap.get(area);
    if( areaRule == null )
      return null;

    String entityName = entity.getType() + "#" + entity.getId();

    Geometry geometry = geometryFactory.createGeometry(entity, entityName);
    if( geometry == null )
      return null;

    Collection<EntityArea> areas = areaRule.getAreas(geometry);

    log.info("Entity " + entity + " area=" + area + " areas=" + areas);

    return Collections.singletonMap(area, areas);
  }
}
