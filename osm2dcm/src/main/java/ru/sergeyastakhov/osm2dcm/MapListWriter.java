/**
 * MapListWriter.java
 *
 * Copyright (C) 2013 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osm2dcm;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class MapListWriter
{
  private static final Logger log = Logger.getLogger("ru.sergeyastakhov.osm2dcm.MapListWriter");

  private String downloadUrlTemplate;
  private File mapListXMLFile;
  private String mapListXMLEncoding;

  public void setDownloadUrlTemplate(String _downloadUrlTemplate)
  {
    downloadUrlTemplate = _downloadUrlTemplate;
  }

  public void setMapListXMLFile(File _mapListXMLFile)
  {
    mapListXMLFile = _mapListXMLFile;
  }

  public void setMapListXMLEncoding(String _mapListXMLEncoding)
  {
    mapListXMLEncoding = _mapListXMLEncoding;
  }

  public void saveMapList(List<MapConversionTask> mapTaskList)
  {
    {
      try
      {
        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();

        OutputStream os = new FileOutputStream(mapListXMLFile);

        try
        {
          XMLStreamWriter writer = outputFactory.createXMLStreamWriter(os, mapListXMLEncoding);

          try
          {
            writer.writeStartDocument(mapListXMLEncoding, "1.0");

            writer.writeStartElement("maplist");

            for( MapConversionTask task : mapTaskList )
            {
              if( task.isHaveMapDate() )
                task.writeTo(writer, downloadUrlTemplate);
            }

            writer.writeEndElement();

            writer.writeEndDocument();
          }
          catch( Exception e )
          {
            log.severe("Error writing error xml file: " + e);
          }
          finally
          {
            writer.close();
          }
        }
        finally
        {
          os.close();
        }

      }
      catch( Exception e )
      {
        log.severe("Error creating error xml file: " + e);
      }

    }
  }
}
