/**
 * EntityArea.java
 * <p>
 * Copyright (C) 2017 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmareatag;

import com.vividsolutions.jts.geom.Geometry;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.TagCollection;

import java.util.Map;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class EntityArea<E extends Entity>
{
  Geometry geometry;
  E entity;

  Map<String, String> tags;

  KeywordSubst tagResolver;


  public EntityArea(Geometry _geometry, E _entity)
  {
    geometry = _geometry;
    entity = _entity;

    tags = ((TagCollection) entity.getTags()).buildMap();
    tagResolver = new KeywordSubst(tags);
  }

  public Tag resolveTag(Tag tagTemplate)
  {
    String tagName = tagTemplate.getKey();
    String tagValue = tagTemplate.getValue();

    return new Tag(tagResolver.resolve(tagName), tagResolver.resolve(tagValue));
  }
}
