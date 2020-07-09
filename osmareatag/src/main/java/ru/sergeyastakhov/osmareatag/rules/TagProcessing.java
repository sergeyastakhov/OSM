/**
 * TagProcessing.java
 * <p>
 *  Copyright (C) 2017 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmareatag.rules;

import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.task.v0_6.Initializable;
import ru.sergeyastakhov.osmareatag.EntityGeometryFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class TagProcessing implements Initializable
{
  private Map<String, AreaRule> areaRuleMap = new LinkedHashMap<>();

  private List<TransformRule> transformRules = new ArrayList<>();

  public void add(AreaRule areaRule)
  {
    areaRuleMap.put(areaRule.getId(), areaRule);
  }

  public void add(TransformRule transformRule)
  {
    transformRules.add(transformRule);
  }

  @Override
  public void initialize(Map<String, Object> metaData)
  {
    for( AreaRule areaRule : areaRuleMap.values() )
    {
      areaRule.initialize(metaData);
    }
  }

  @Override
  public void complete()
  {
    for( AreaRule areaRule : areaRuleMap.values() )
    {
      areaRule.complete();
    }
  }

  @Override
  public void close()
  {
    for( AreaRule areaRule : areaRuleMap.values() )
    {
      areaRule.close();
    }
  }

  public void prepareAreaRules(EntityContainer entityContainer, Map<String, String> tags)
  {
    for( AreaRule areaRule : areaRuleMap.values() )
    {
      areaRule.prepareAreaRules(entityContainer, tags);
    }
  }

  public void buildIndexes(EntityGeometryFactory geometryFactory)
  {
    for( AreaRule areaRule : areaRuleMap.values() )
    {
      areaRule.buildIndexes(geometryFactory);
    }
  }


  public <E extends EntityContainer> E processEntity(E entityContainer, Map<String, String> tags, EntityGeometryFactory geometryFactory)
  {
    for( TransformRule transformRule : transformRules )
    {
      entityContainer = transformRule.processEntity(entityContainer, tags, geometryFactory, areaRuleMap);
    }

    return entityContainer;
  }
}
