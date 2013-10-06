/**
 * MapConversionTask.java
 *
 * Copyright (C) 2013 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osm2dcm;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class MapConversionTask
{
  private static final Logger log = Logger.getLogger("ru.sergeyastakhov.osm2dcm.MapConversionTask");

  public static final Comparator<? super MapConversionTask> PRIORITY_SORT = new Comparator<MapConversionTask>()
  {
    @Override
    public int compare(MapConversionTask m1, MapConversionTask m2)
    {
      int result = Integer.valueOf(m1.priority).compareTo(m2.priority);

      if( result == 0 )
      {
        long l1 = m1.lastTryDate != null ? m1.lastTryDate.getTime() : 0;
        long l2 = m2.lastTryDate != null ? m2.lastTryDate.getTime() : 0;

        result = Long.valueOf(l1).compareTo(l2);
      }

      if( result == 0 )
      {
        result = Integer.valueOf(m1.usedTime).compareTo(m2.usedTime);
      }

      return result;
    }
  };

  private String code;
  private String cgId;
  private int priority;
  private String locTitle;
  private String title;
  private String poly;
  private String source;
  private String qaMode;
  private String customKeys;
  private String viewPoint;
  private Date lastTryDate;
  private Date date;
  private int version;
  private int usedTime;

  public MapConversionTask(String line)
  {
    String[] fields = line.split("\\|");

    code = fields[0].trim();
    cgId = fields[1].trim();
    priority = Integer.parseInt(fields[2].trim());

    locTitle = fields[3].trim();
    title = fields[4].trim();

    poly = fields[5].trim();
    source = fields[6].trim();
    qaMode = fields[7].trim();
    customKeys = fields[8].trim();
    viewPoint = fields[9].trim();

    DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    try
    {
      lastTryDate = dateFormat.parse(fields[10].trim());
    }
    catch( ParseException ignore )
    {
    }

    try
    {
      date = dateFormat.parse(fields[11].trim());
    }
    catch( ParseException ignore )
    {
    }

    version = Integer.parseInt(fields[12].trim());
    usedTime = Integer.parseInt(fields[13].trim());
  }

  public synchronized String toTextLine()
  {
    DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    StringBuilder sb = new StringBuilder();

    sb.append(code).append('|');
    sb.append(cgId).append('|');
    sb.append(priority).append('|');
    sb.append(locTitle).append('|');
    sb.append(title).append('|');
    sb.append(poly).append('|');
    sb.append(source).append('|');
    sb.append(qaMode).append('|');
    sb.append(customKeys).append('|');
    sb.append(viewPoint).append('|');

    sb.append(lastTryDate != null ? dateFormat.format(lastTryDate) : "").append('|');
    sb.append(date != null ? dateFormat.format(date) : "").append('|');

    sb.append(version).append('|');
    sb.append(usedTime);

    return sb.toString();
  }

  public synchronized void writeTo(XMLStreamWriter writer, String downloadUrlTemplate) throws XMLStreamException
  {
    writer.writeStartElement("map");

    writer.writeStartElement("code");
    writer.writeCharacters(code);
    writer.writeEndElement();

    writer.writeStartElement("uid");
    writer.writeCharacters(cgId);
    writer.writeEndElement();

    writer.writeStartElement("name");
    writer.writeCharacters(title.length() > 0 ? title : locTitle);
    writer.writeEndElement();

    writer.writeStartElement("name_ru");
    writer.writeCharacters(locTitle);
    writer.writeEndElement();

    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    writer.writeStartElement("date");
    writer.writeCharacters(dateFormat.format(date));
    writer.writeEndElement();

    writer.writeStartElement("version");
    writer.writeCharacters("1." + version);
    writer.writeEndElement();

    writer.writeStartElement("url");
    writer.writeCharacters(MessageFormat.format(downloadUrlTemplate, code));
    writer.writeEndElement();

    if( code.equals("EU-OVRV") )
    {
      writer.writeStartElement("overview");
      writer.writeCharacters("1");
      writer.writeEndElement();
    }

    writer.writeEndElement();
  }

  public Date getLastTryDate()
  {
    return lastTryDate;
  }

  public boolean isHaveMapDate()
  {
    return date != null;
  }

  public boolean convertMap(File logDir, File sourceDir) throws Exception
  {
    String sourceFileName = source.length() != 0 ? source : code + ".osm";
    File sourceFile = new File(sourceDir, sourceFileName);
    File logFile = new File(logDir, code + ".log");

    Date sourceFileTime = sourceFile.exists() ? new Date(sourceFile.lastModified()) : null;

    log.log(Level.INFO, "Process map {0} {1} {2,date,short} {3,date,short} {4,date,short}",
            new Object[]{code, source, sourceFileTime, date, lastTryDate});

    Date currentTime = new Date();

    if( priority < 9 &&
        lastTryDate != null &&
        TimeUnit.MILLISECONDS.toDays(currentTime.getTime() - lastTryDate.getTime()) > 300 &&
        (sourceFileTime == null || TimeUnit.MILLISECONDS.toDays(currentTime.getTime() - sourceFileTime.getTime()) > 7) )
    {
      // Обновление исходного файла
      if( runUpdate(sourceFileName, logFile) )
      {
        sourceFileTime = sourceFile.exists() ? new Date(sourceFile.lastModified()) : null;
      }
    }

    if( priority != 0 )
    {
      if( sourceFileTime == null )
      {
        log.log(Level.WARNING, "Can''t convert map {0} - no source file {1}", new Object[]{code, sourceFile});
        return false;
      }

      if( !sourceFileTime.after(lastTryDate) )
      {
        log.log(Level.INFO, "Skipping map {0} - source file {1} is not updated since last try",
                new Object[]{code, sourceFile});
        return false;
      }
    }

    // Запуск конвертации

    return runConversion(sourceFileName, logFile);
  }

  private boolean runUpdate(String sourceFileName, File logFile) throws Exception
  {
    log.log(Level.INFO, "Trying to update source file {0}", new Object[]{sourceFileName});

    ProcessBuilder pb = new ProcessBuilder("update.bat", sourceFileName);

    pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
    pb.redirectError(ProcessBuilder.Redirect.INHERIT);

    Process process = pb.start();

    int result = process.waitFor();

    return result == 0;
  }

  private boolean runConversion(String sourceFileName, File logFile) throws Exception
  {
    log.log(Level.INFO, "Start conversion for map {0} {1} {2}", new Object[]{code, title, sourceFileName});

    String dcmTitle = title.length() != 0 ? title : locTitle;
    String polyFile = poly.length() != 0 ? poly : code;

    int newVersion = version + 1;

    ProcessBuilder pb = new ProcessBuilder
        ("make.bat",
         code, dcmTitle, polyFile, sourceFileName, qaMode, customKeys, viewPoint,
         Integer.toString(newVersion),
         cgId);

    pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
    pb.redirectError(ProcessBuilder.Redirect.INHERIT);

    lastTryDate = new Date();

    Process process = pb.start();

    int result = process.waitFor();

    long elapsedTime = System.currentTimeMillis() - lastTryDate.getTime();

    log.log(Level.INFO, "Conversion for map {0} completed. Result code = {1}", new Object[]{code, result});

    boolean conversionSuccess = result == 0;

    if( conversionSuccess )
    {
      // При успешной конвертации - обновление атрибутов карты
      synchronized( this )
      {
        version = newVersion;
        usedTime = (int) TimeUnit.MILLISECONDS.toMinutes(elapsedTime);
        date = lastTryDate;
        priority = 6;
      }
    }

    return conversionSuccess;
  }
}
