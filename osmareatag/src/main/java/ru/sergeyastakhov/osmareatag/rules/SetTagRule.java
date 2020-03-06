/**
 * SetTagRule.java
 * <p>
 *  Copyright (C) 2020 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmareatag.rules;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import ru.sergeyastakhov.osmareatag.EntityArea;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class SetTagRule implements OutputElement
{
  private Tag tagTemplate;

  private String contextArea;

  public SetTagRule(String _key, String _value, String _contextArea)
  {
    tagTemplate = new Tag(_key, _value);

    contextArea = _contextArea;
  }

  @Override
  public void apply(Collection<Tag> tags, Map<String, Collection<EntityArea>> matchedResult)
  {
    if( contextArea != null )
    {
      Collection<EntityArea> entityAreas = matchedResult.get(contextArea);

      if( entityAreas != null )
      {
        for( EntityArea area : entityAreas )
        {
          Tag tagToSet = area.resolveTag(tagTemplate);

          setTag(tags, tagToSet);
        }
      }
    }
    else
    {
      setTag(tags, tagTemplate);
    }
  }

  private void setTag(Collection<Tag> tags, Tag tagToSet)
  {
    for( Iterator<Tag> iterator = tags.iterator(); iterator.hasNext(); )
    {
      Tag tag = iterator.next();

      String tagName = tag.getKey();

      if( tagName.equals(tagToSet.getKey()) )
      {
        iterator.remove();
      }
    }

    tags.add(tagToSet);
  }
}
