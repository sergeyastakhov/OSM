/**
 * MatchRule.java
 * <p>
 *  Copyright (C) 2017 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmareatag.rules;

import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import ru.sergeyastakhov.osmareatag.EntityArea;
import ru.sergeyastakhov.osmareatag.EntityGeometryFactory;

import java.util.*;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class MatchRule
{
  private EntityType type;

  private List<TagMatcher> tagMatchers = new ArrayList<>();

  private List<GeometryMatcher> geometryMatchers = new ArrayList<>();

  public MatchRule(String _type)
  {
    if( _type != null )
    {
      switch( _type )
      {
        case "node":
          type = EntityType.Node;
          break;
        case "way":
          type = EntityType.Way;
          break;
        case "relation":
          type = EntityType.Relation;
          break;
      }
    }
  }

  public void add(TagMatcher matcher) { tagMatchers.add(matcher); }

  public void add(GeometryMatcher matcher) { geometryMatchers.add(matcher); }


  public boolean match(EntityType entityType, Map<String, String> tags)
  {
    if( type != null && type != entityType )
      return false;

    for( TagMatcher matcher : tagMatchers )
    {
      if( !matcher.matchTags(tags) )
      {
        return false;
      }
    }

    return true;
  }

  public Map<String, Collection<EntityArea<?>>> match
      (Entity entity, Map<String, String> tags, EntityGeometryFactory geometryFactory, Map<String, AreaRule> areaRuleMap)
  {
    if( !match(entity.getType(), tags) )
      return null;

    Map<String, Collection<EntityArea<?>>> matchedAreas = new LinkedHashMap<>();

    for( GeometryMatcher matcher : geometryMatchers )
    {
      Map<String, Collection<EntityArea<?>>> entityAreas = matcher.matchGeometry(entity, tags, geometryFactory, areaRuleMap);
      if( entityAreas==null )
        return null;

      matchedAreas.putAll(entityAreas);
    }

    return matchedAreas;
  }
}
