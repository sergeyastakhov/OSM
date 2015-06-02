/**
 * WatchdogService.java
 *
 * Copyright (C) 2015 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osm2dcm;

import java.util.concurrent.TimeUnit;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public interface WatchdogService
{
  void startService();
  void stopService();

  void taskStarted(long timeout, TimeUnit unit);
  void taskStopped();

  Runnable wrapRunnable(Runnable task, long timeout, TimeUnit unit);
}
