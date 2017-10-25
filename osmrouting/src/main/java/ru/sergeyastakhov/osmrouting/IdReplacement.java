/**
 * IdReplacement.java
 * <p>
 * Copyright (C) 2017 RNIC. All Rights Reserved
 */
package ru.sergeyastakhov.osmrouting;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
class IdReplacement
{
  long originalId;
  long newId;

  IdReplacement(long _originalId, long _newId)
  {
    originalId = _originalId;
    newId = _newId;
  }
}
