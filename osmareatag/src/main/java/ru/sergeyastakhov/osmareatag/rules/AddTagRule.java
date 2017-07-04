/**
 * AddTagRule.java
 * <p>
 *  Copyright (C) 2017 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmareatag.rules;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import ru.sergeyastakhov.osmareatag.EntityArea;

import java.util.Collection;
import java.util.Map;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class AddTagRule implements OutputElement
{
  private Tag tagTemplate;

  private String contextArea;

  public AddTagRule(String _key, String _value, String _contextArea)
  {
    tagTemplate = new Tag(_key, _value);

    contextArea = _contextArea;
  }

  @Override
  public void apply(Collection<Tag> tags, Map<String, Collection<EntityArea<?>>> matchedResult)
  {
    if( contextArea != null )
    {
      Collection<EntityArea<?>> entityAreas = matchedResult.get(contextArea);

      if( entityAreas != null )
      {
        for( EntityArea<?> area : entityAreas )
        {
          Tag addTag = area.resolveTag(tagTemplate);

          addTagIfNotExist(tags, addTag);
        }
      }
    }
    else
    {
      addTagIfNotExist(tags, tagTemplate);
    }
  }

  private void addTagIfNotExist(Collection<Tag> tags, Tag addTag)
  {
    for( Tag tag : tags )
    {
      String tagName = tag.getKey();

      if( tagName.equals(addTag.getKey()) )
      {
        return;
      }
    }

    tags.add(addTag);
  }
}
