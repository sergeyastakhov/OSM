/**
 * TagRule.java
 * <p>
 *  Copyright (C) 2017 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmareatag.rules;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class TagRule implements TagMatcher
{
  private Pattern keyPattern;
  private Pattern valuePattern;

  public TagRule(String _keyPattern, String _valuePattern)
  {
    keyPattern = Pattern.compile(_keyPattern);
    valuePattern = Pattern.compile(_valuePattern);
  }

  @Override
  public boolean matchTags(Map<String, String> tags)
  {
    for( Map.Entry<String, String> entry : tags.entrySet() )
    {
      Matcher keyMatcher = keyPattern.matcher(entry.getKey());
      if( !keyMatcher.matches() )
        continue;

      Matcher valueMatcher = valuePattern.matcher(entry.getValue());
      if( valueMatcher.matches() )
      {
        return true;
      }
    }

    return false;
  }
}
