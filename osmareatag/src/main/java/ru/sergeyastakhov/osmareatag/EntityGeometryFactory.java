/**
 * EntityGeometryFactory.java
 * <p>
 *  Copyright (C) 2017 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmareatag;

import com.vividsolutions.jts.geom.Geometry;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public interface EntityGeometryFactory
{
  Geometry createGeometry(Entity entity, String entityName);

  Geometry createAreaGeometry(Entity entity, String entityName);
}
