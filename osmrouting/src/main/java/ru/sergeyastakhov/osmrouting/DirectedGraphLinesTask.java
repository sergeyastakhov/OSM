/**
 * $Id$
 *
 * Copyright (C) 2012 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmrouting;

import java.util.*;

import org.openstreetmap.osmosis.core.container.v0_6.*;
import org.openstreetmap.osmosis.core.domain.v0_6.*;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.core.task.v0_6.SinkSource;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class DirectedGraphLinesTask implements SinkSource, EntityProcessor
{
  private static final String TAG_ONEWAY = "oneway";
  private static final String TAG_VEHICLE = "vehicle";

  private Sink sink;

  private IdFactory wayIdFactory = EntityIdFactory.wayIdFactory;

  private AccessMode graphMinimumAccess;

  public DirectedGraphLinesTask(boolean vehicleOnly)
  {
    graphMinimumAccess = vehicleOnly ? AccessMode.VEHICLE : AccessMode.ACCESS;
  }

  @Override
  public void setSink(Sink _sink)
  {
    sink = _sink;
  }

  @Override
  public void process(EntityContainer entityContainer)
  {
    entityContainer.process(this);
  }

  @Override
  public void complete()
  {
    sink.complete();
  }

  @Override
  public void release()
  {
    sink.release();
  }

  @Override
  public void process(BoundContainer bound)
  {
    sink.process(bound);
  }

  @Override
  public void process(NodeContainer node)
  {
    sink.process(node);
  }

  @Override
  public void process(WayContainer wayContainer)
  {
    wayContainer = wayContainer.getWriteableInstance();

    Way directLine = wayContainer.getEntity();

    Map<AccessMode, String> wayAccessMap = new EnumMap<AccessMode, String>(AccessMode.class);

    Map<AccessMode, Boolean> directOnewayAccessMap = new EnumMap<AccessMode, Boolean>(AccessMode.class);
    Map<AccessMode, Boolean> returnOnewayAccessMap = new EnumMap<AccessMode, Boolean>(AccessMode.class);

    Map<String, String> nonAccessTagsMap = new HashMap<String, String>();

    HighwayType highwayType = HighwayType.getWayType(directLine);

    // motorway односторонние по умолчанию
    if( highwayType != null && highwayType.isOneway() )
    {
      returnOnewayAccessMap.put(AccessMode.VEHICLE, false);
    }

    // Generate access tags for lines from oneway tag
    for( Tag tag : directLine.getTags() )
    {
      String tagKey = tag.getKey();
      String tagValue = tag.getValue();

      if( tagKey.contains(TAG_ONEWAY) )
      {
        AccessMode accessMode = AccessMode.VEHICLE;

        int keyLength = tagKey.length();
        if( keyLength > 7 )
        {
          if( tagKey.endsWith(TAG_ONEWAY) )
          {
            // xxx:oneway tag
            accessMode = AccessMode.fromString(tagKey.substring(0, keyLength - 7));
          }
          else if( tagKey.startsWith(TAG_ONEWAY) )
          {
            // oneway:xxx tag
            accessMode = AccessMode.fromString(tagKey.substring(7, keyLength));
          }
          else
          {
            accessMode = null;
          }
        }

        if( accessMode != null )
        {
          if( tagValue.equals("yes") )
          {
//            directOnewayAccessMap.put(accessMode, true);
            returnOnewayAccessMap.put(accessMode, false);
          }
          else if( tagValue.equals("-1") )
          {
            directOnewayAccessMap.put(accessMode, false);
            returnOnewayAccessMap.remove(accessMode);
          }
          else if( tagValue.equals("no") )
          {
            directOnewayAccessMap.remove(accessMode);
            returnOnewayAccessMap.remove(accessMode);
          }

          continue;
        }
      }
      else
      {
        AccessMode accessMode = AccessMode.fromString(tagKey);
        if( accessMode != null )
        {
          wayAccessMap.put(accessMode, tagValue);
          continue;
        }
      }

      nonAccessTagsMap.put(tagKey, tagValue);
    }

    // Build and send direct line

    Map<AccessMode, String> directAccessMap = AccessMode.restrictTags(wayAccessMap, directOnewayAccessMap);

    if( graphMinimumAccess.hasAnySubModeAccess(directAccessMap) )
    {
      Map<String, String> directLineTagsMap = new HashMap<String, String>();

      for( Map.Entry<AccessMode, String> entry : directAccessMap.entrySet() )
      {
        directLineTagsMap.put(entry.getKey().toString(), entry.getValue());
      }

      // Add nonaccess tags
      directLineTagsMap.putAll(nonAccessTagsMap);

      fillTagsFromMap(directLine.getTags(), directLineTagsMap);

      // Send direct line
      sink.process(new WayContainer(directLine));
    }

    // Build and send return line

    Map<AccessMode, String> returnAccessMap = AccessMode.restrictTags(wayAccessMap, returnOnewayAccessMap);

    if( graphMinimumAccess.hasAnySubModeAccess(returnAccessMap) )
    {
      Map<String, String> returnLineTagsMap = new HashMap<String, String>();

      for( Map.Entry<AccessMode, String> entry : returnAccessMap.entrySet() )
      {
        returnLineTagsMap.put(entry.getKey().toString(), entry.getValue());
      }

      // Add nonaccess tags
      returnLineTagsMap.putAll(nonAccessTagsMap);

      if( !returnLineTagsMap.containsKey("original_id") )
      {
        returnLineTagsMap.put("original_id", String.valueOf(directLine.getId()));
      }

      CommonEntityData entityData = new CommonEntityData
         (wayIdFactory.nextId(), 1, directLine.getTimestamp(), directLine.getUser(), 0);

      List<WayNode> wayNodeList = new ArrayList<WayNode>(directLine.getWayNodes());
      Collections.reverse(wayNodeList);

      Way returnLine = new Way(entityData, wayNodeList);

      fillTagsFromMap(returnLine.getTags(), returnLineTagsMap);

      sink.process(new WayContainer(returnLine));
    }
  }

  private static void fillTagsFromMap(Collection<Tag> tags, Map<String, String> tagsMap)
  {
    tags.clear();

    for( Map.Entry<String, String> entry : tagsMap.entrySet() )
    {
      tags.add(new Tag(entry.getKey(), entry.getValue()));
    }
  }

  @Override
  public void process(RelationContainer relation)
  {
    sink.process(relation);
  }
}
