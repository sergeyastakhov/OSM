/**
 * $Id$
 *
 * Copyright (C) 2010 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmfilter;

import org.openstreetmap.osmosis.core.container.v0_6.*;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.lifecycle.ReleasableIterator;
import org.openstreetmap.osmosis.core.store.SimpleObjectStore;
import org.openstreetmap.osmosis.core.store.SingleClassObjectSerializationFactory;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.core.task.v0_6.SinkSource;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class ConstructionWayFilter implements SinkSource, EntityProcessor
{
  public static final String KEY_HIGHWAY = "highway";
  public static final String KEY_OPENING_DATE = "opening_date";
  public static final String KEY_CHECK_DATE = "check_date";
  public static final String VALUE_CONSTRUCTION = "construction";

  private static final Logger log = Logger.getLogger(ConstructionWayFilter.class.getName());

  private Sink sink;

  private DateFormat dateFormat;

  private Date currentDate;
  private Date openiningDateLimit;
  private Date checkDateLimit;

  private String writeErrorXML;
  private SimpleObjectStore<Node> allNodes;

  private List<ElementError> errors;

  private ErrorsSummary errorsSummary = new ErrorsSummary();

  public ConstructionWayFilter(int daysBeforeOpening, int daysAfterChecking, String _writeErrorXML)
  {
    dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    Calendar currentDateCal = Calendar.getInstance();
    currentDateCal.clear(Calendar.HOUR_OF_DAY);
    currentDateCal.clear(Calendar.MINUTE);
    currentDateCal.clear(Calendar.SECOND);
    currentDateCal.clear(Calendar.MILLISECOND);

    currentDate = currentDateCal.getTime();

    if( daysBeforeOpening > 0 )
    {
      Calendar cal = (Calendar) currentDateCal.clone();
      cal.add(Calendar.DAY_OF_MONTH, daysBeforeOpening);

      openiningDateLimit = cal.getTime();
    }

    if( daysAfterChecking > 0 )
    {
      Calendar cal = (Calendar) currentDateCal.clone();
      cal.add(Calendar.DAY_OF_MONTH, -daysAfterChecking);

      checkDateLimit = cal.getTime();
    }

    writeErrorXML = _writeErrorXML;

    if( writeErrorXML != null )
    {
      allNodes = new SimpleObjectStore<Node>
          (new SingleClassObjectSerializationFactory(Node.class), "cwfnd", true);
    }
  }

  @Override
  public void initialize(Map<String, Object> metaData)
  {
  }

  public void process(EntityContainer entityContainer)
  {
    // Ask the entity container to invoke the appropriate processing method
    // for the entity type.
    entityContainer.process(this);
  }

  public void process(BoundContainer container)
  {
    // By default, pass it on unchanged
    sink.process(container);
  }


  public void process(NodeContainer container)
  {
    if( allNodes != null )
      allNodes.add(container.getEntity());

    sink.process(container);
  }

  public void process(WayContainer container)
  {
    Way way = container.getEntity();

    Tag highwayTag = null;
    boolean construction = false;
    String openingDateValue = null;
    String checkDateValue = null;

    for( Tag tag : way.getTags() )
    {
      String key = tag.getKey();
      String value = tag.getValue();

      if( key.equals(KEY_HIGHWAY) )
      {
        highwayTag = tag;
        construction = value.equals(VALUE_CONSTRUCTION);
      }
      else if( key.equals(KEY_OPENING_DATE) )
      {
        openingDateValue = value;
      }
      else if( key.equals(KEY_CHECK_DATE) )
      {
        checkDateValue = value;
      }
    }

    boolean openingMatched = openiningDateLimit == null;

    if( openingDateValue != null )
    {
      try
      {
        Date openingDate = dateFormat.parse(openingDateValue);

        if( openiningDateLimit != null )
          openingMatched = openingDate.before(openiningDateLimit);

        if( openingDate.before(currentDate) )
        {
          registerWayError(way, ElementErrorType.OPENING_DATE_PASSED, openingDateValue, checkDateValue);
        }
      }
      catch( ParseException e )
      {
        log.warning("Way #" + way.getId() + " opening_date parse error: " + e);

        registerWayError(way, ElementErrorType.OPENING_DATE_FORMAT_ERROR, openingDateValue, checkDateValue);
      }
    }

    boolean checkMatched = checkDateLimit == null;

    if( checkDateValue != null )
    {
      try
      {
        Date checkDate = dateFormat.parse(checkDateValue);

        if( checkDateLimit != null )
        {
          checkMatched = checkDate.after(checkDateLimit);

          if( !checkMatched )
          {
            registerWayError(way, ElementErrorType.CHECK_DATE_TOO_OLD, openingDateValue, checkDateValue);
          }
        }
      }
      catch( ParseException e )
      {
        log.warning("Way #" + way.getId() + " check_date parse error: " + e);

        registerWayError(way, ElementErrorType.CHECK_DATE_FORMAT_ERROR, openingDateValue, checkDateValue);
      }
    }

    if( construction && openingMatched && checkMatched )
    {
      container = container.getWriteableInstance();
      Collection<Tag> tags = container.getEntity().getTags();
      tags.remove(highwayTag);
      tags.add(new Tag(KEY_HIGHWAY, "checked_construction"));
    }

    sink.process(container);
  }

  private void registerWayError(Way way, ElementErrorType errorType, String openingDate, String checkDate)
  {
    if( errors == null )
      errors = new ArrayList<ElementError>();

    errors.add(new ElementError(way, errorType, openingDate, checkDate));

    errorsSummary.newError(errorType);
  }

  public void process(RelationContainer container)
  {
    sink.process(container);
  }


  public void complete()
  {
    if( writeErrorXML != null )
    {
      if( errors != null && !errors.isEmpty() )
      {
        // Формирование bbox ошибочных объектов
        if( allNodes != null )
        {
          ReleasableIterator<Node> nodeIterator = allNodes.iterate();
          while( nodeIterator.hasNext() )
          {
            Node node = nodeIterator.next();

            for( ElementError error : errors )
            {
              error.updateBound(node);
            }
          }

          nodeIterator.release();
        }
      }

      try
      {
        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
        OutputStream os = new FileOutputStream(writeErrorXML);

        try
        {
          XMLStreamWriter writer = outputFactory.createXMLStreamWriter(os, "UTF-8");

          try
          {
            writer.writeStartDocument();

            writer.writeStartElement("errors");

            errorsSummary.writeTo(writer);

            if( errors != null && !errors.isEmpty() )
            {
              writer.writeStartElement("error_list");

              for( ElementError error : errors )
              {
                error.writeTo(writer);
              }

              writer.writeEndElement();
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

    // done
    sink.complete();
  }

  public void release()
  {
    if( allNodes != null )
    {
      allNodes.release();
    }

    sink.release();
  }


  public void setSink(Sink sink)
  {
    this.sink = sink;
  }
}