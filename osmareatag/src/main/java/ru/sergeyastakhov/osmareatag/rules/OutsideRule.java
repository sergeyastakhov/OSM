/**
 * OutsideRule.java
 * <p>
 *  Copyright (C) 2017 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmareatag.rules;

import com.vividsolutions.jts.geom.Geometry;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import ru.sergeyastakhov.osmareatag.EntityArea;
import ru.sergeyastakhov.osmareatag.EntityGeometryFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class OutsideRule implements GeometryMatcher
{
  private String area;

  public OutsideRule(String _area) { area = _area; }

  @Override
  public Map<String, Collection<EntityArea<?>>> matchGeometry
      (Entity entity, Map<String, String> tags,
       EntityGeometryFactory geometryFactory,
       Map<String, AreaRule> areaRuleMap)
  {
    AreaRule areaRule = areaRuleMap.get(area);
    if( areaRule==null )
      return null;

    String entityName = entity.getType() + "#" + entity.getId();

    Geometry geometry = geometryFactory.createGeometry(entity, entityName);
    if( geometry==null )
      return null;

    Collection<EntityArea<?>> areas = areaRule.getAreas(geometry);
    if( !areas.isEmpty() )
      return null;

    return Collections.singletonMap(area, areas);
  }
}
