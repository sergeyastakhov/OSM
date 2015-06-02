/**
 * InterruptWatchdogService.java
 *
 * Copyright (C) 2015 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osm2dcm;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class InterruptWatchdogService implements WatchdogService
{
  private static final Logger log = Logger.getLogger("ru.sergeyastakhov.osm2dcm.InterruptWatchdogService");

  private ConcurrentMap<Thread, Date> watchedThreads = new ConcurrentHashMap<Thread, Date>();

  private ScheduledExecutorService scheduler;
  private ScheduledFuture<?> interruptTask;

  public InterruptWatchdogService()
  {
    startService();
  }

  @Override
  public void startService()
  {
    scheduler = Executors.newScheduledThreadPool(1);
    interruptTask = scheduler.scheduleAtFixedRate(new Runnable()
    {
      @Override
      public void run()
      {
        interruptStaleThreads();
      }
    }, 30, 30, TimeUnit.SECONDS);
  }

  @Override
  public void stopService()
  {
    scheduler.shutdown();
  }

  private void interruptStaleThreads()
  {
    Date now = new Date();

    for( Map.Entry<Thread, Date> entry : watchedThreads.entrySet() )
    {
      Date timeoutMark = entry.getValue();
      if( timeoutMark.before(now) )
      {
        Thread staleThread = entry.getKey();

        log.log(Level.INFO, "Interrupting thread due timeout limit: {0}", staleThread);

        staleThread.interrupt();
      }
    }
  }

  @Override
  public void taskStarted(long timeout, TimeUnit unit)
  {
    Date timeoutMark = new Date(System.currentTimeMillis() + unit.toMillis(timeout));

    watchedThreads.put(Thread.currentThread(), timeoutMark);
  }

  @Override
  public void taskStopped()
  {
    watchedThreads.remove(Thread.currentThread());
  }

  @Override
  public Runnable wrapRunnable(final Runnable task, final long timeout, final TimeUnit unit)
  {
    return new Runnable()
    {
      @Override
      public void run()
      {
        taskStarted(timeout, unit);

        try
        {
          task.run();
        }
        finally
        {
          taskStopped();
        }
      }
    };
  }
}
