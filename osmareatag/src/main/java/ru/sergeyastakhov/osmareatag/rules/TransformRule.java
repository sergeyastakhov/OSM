/**
 * TransformRule.java
 * <p>
 * Copyright (C) 2017 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmareatag.rules;

import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import ru.sergeyastakhov.osmareatag.EntityArea;
import ru.sergeyastakhov.osmareatag.EntityGeometryFactory;

import java.util.Collection;
import java.util.Map;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class TransformRule extends MatchOwner
{
  private String name;

  private OutputRule outputRule;

  public void setName(String _name)
  {
    name = _name;
  }

  public <E extends EntityContainer> void setOutputRule(OutputRule _outputRule)
  {
    outputRule = _outputRule;
  }

  public <E extends EntityContainer> E processEntity(E entityContainer, Map<String, String> tags,
                                                     EntityGeometryFactory geometryFactory,
                                                     Map<String, AreaRule> areaRuleMap)
  {
    Entity entity = entityContainer.getEntity();

    Map<String, Collection<EntityArea>> matchedResult = matchRule.match(entity, tags, geometryFactory, areaRuleMap);

    if( matchedResult!=null )
    {
      entityContainer = outputRule.processEntity(entityContainer, matchedResult);
    }

    return entityContainer;
  }
}
