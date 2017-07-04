/**
 * OutputElement.java
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
public interface OutputElement
{
  void apply(Collection<Tag> tags, Map<String, Collection<EntityArea>> matchedResult);
}
