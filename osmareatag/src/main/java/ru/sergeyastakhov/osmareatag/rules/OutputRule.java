/**
 * OutputRule.java
 * <p>
 *  Copyright (C) 2017 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmareatag.rules;

import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import ru.sergeyastakhov.osmareatag.EntityArea;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class OutputRule
{
  private List<OutputElement> outputs = new ArrayList<>();

  public void add(OutputElement outputElement)
  {
    outputs.add(outputElement);
  }

  public <E extends EntityContainer> E processEntity(E entityContainer,
                                                     Map<String, Collection<EntityArea>> matchedResult)
  {
    entityContainer = (E) entityContainer.getWriteableInstance();

    Entity entity = entityContainer.getEntity();

    Collection<Tag> tags = entity.getTags();

    for( OutputElement output : outputs )
    {
      output.apply(tags, matchedResult);
    }

    return entityContainer;
  }
}
