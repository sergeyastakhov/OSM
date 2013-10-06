/**
 * MapConverter.java
 *
 * Copyright (C) 2013 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osm2dcm;

import java.io.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class MapConverter
{
  private static final Logger log = Logger.getLogger("ru.sergeyastakhov.osm2dcm.MapConverter");

  private List<MapConversionTask> mapTaskList = new ArrayList<MapConversionTask>();

  private File historyFile;
  private File historyLogDir;
  private String encoding;

  private File sourceDir;
  private File processLogDir;

  private MapListWriter mapListWriter;

  public void setHistoryFile(File _historyFile)
  {
    historyFile = _historyFile;
  }

  public void setHistoryLogDir(File _historyLogDir)
  {
    historyLogDir = _historyLogDir;
  }

  public void setEncoding(String _encoding)
  {
    encoding = _encoding;
  }

  public void setSourceDir(File _sourceDir)
  {
    sourceDir = _sourceDir;
  }

  public void setProcessLogDir(File _processLogDir)
  {
    processLogDir = _processLogDir;
  }

  public void setMapListWriter(MapListWriter _mapListWriter)
  {
    mapListWriter = _mapListWriter;
  }

  public List<Callable<Object>> getTaskList(final ConversionListener conversionListener)
  {
    // Сортировка списка
    List<MapConversionTask> sortedTaskList = new ArrayList<MapConversionTask>(mapTaskList);

    Collections.sort(sortedTaskList, MapConversionTask.PRIORITY_SORT);

    // Формирование списка задач на выполнение
    List<Callable<Object>> taskList = new ArrayList<Callable<Object>>();

    for( final MapConversionTask task : sortedTaskList )
    {
      taskList.add(new Callable<Object>()
      {
        @Override
        public Object call() throws Exception
        {
          boolean conversionSuccess = task.convertMap(processLogDir, sourceDir);

          try
          {
            // Сохранение списка в отдельном каталоге
            String fileName = MessageFormat.format("history.txt-{0,time,yyyyMMddHHmm}", task.getLastTryDate());

            File historyLogFile = new File(historyLogDir, fileName);

            saveTaskList(historyLogFile);

            saveTaskList(historyFile);
          }
          catch( Exception e )
          {
            log.log(Level.WARNING, "Error saving history", e);
          }

          if( conversionSuccess )
          {
            mapListWriter.saveMapList(mapTaskList);
          }

          conversionListener.conversionCompleted(task);
          return null;
        }
      });
    }

    return taskList;
  }

  public synchronized void loadTaskList() throws IOException
  {
    mapTaskList.clear();

    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(historyFile), encoding));

    try
    {
      String line;

      while( (line = br.readLine()) != null )
      {
        line = line.trim();
        if( line.length() == 0 || line.startsWith("#") )
          continue;

        mapTaskList.add(new MapConversionTask(line));
      }
    }
    finally
    {
      br.close();
    }
  }

  private synchronized void saveTaskList(File file) throws IOException
  {
    PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), encoding));

    try
    {
      for( MapConversionTask task : mapTaskList )
      {
        pw.println(task.toTextLine());
      }
    }
    finally
    {
      pw.close();
    }
  }
}
