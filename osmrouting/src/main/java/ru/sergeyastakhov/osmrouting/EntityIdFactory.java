/**
 * $Id$
 *
 * Copyright (C) 2012 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmrouting;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class EntityIdFactory implements IdFactory
{
  public static final IdFactory nodeIdFactory = new EntityIdFactory();
  public static final IdFactory wayIdFactory = new EntityIdFactory();
  public static final IdFactory relationIdFactory = new EntityIdFactory();

  private long nextId = -1;

  @Override
  public synchronized long nextId()
  {
    return nextId--;
  }
}
