/**
 * EntityArea.java
 * <p>
 * Copyright (C) 2017 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmareatag;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.TagCollection;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class EntityArea implements Serializable
{
  private static final long serialVersionUID = 1L;

  private Geometry areaGeometry;
  private Map<String, String> tags;

  private transient PreparedGeometry preparedAreaGeometry;

  private transient KeywordSubst tagResolver;


  public EntityArea(Geometry _areaGeometry, Entity entity)
  {
    areaGeometry = _areaGeometry;

    tags = ((TagCollection) entity.getTags()).buildMap();
  }

  @Override
  public String toString()
  {
    return "EntityArea{" +
        "tags#=" + tags.size() + " name=" + tags.get("name") +
        '}';
  }

  public Tag resolveTag(Tag tagTemplate)
  {
    if( tagResolver == null )
    {
      tagResolver = new KeywordSubst(tags);
    }

    String tagName = tagTemplate.getKey();
    String tagValue = tagTemplate.getValue();

    return new Tag(tagResolver.resolve(tagName), tagResolver.resolve(tagValue));
  }

  public boolean isInside(Geometry geometry)
  {
    if( preparedAreaGeometry == null )
    {
      preparedAreaGeometry = PreparedGeometryFactory.prepare(areaGeometry);
    }

    return preparedAreaGeometry.covers(geometry);
  }
}
