/**
 * TagMatcher.java
 * <p>
 *  Copyright (C) 2017 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmareatag.rules;

import java.util.Map;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public interface TagMatcher
{
  boolean matchTags(Map<String, String> tags);
}
