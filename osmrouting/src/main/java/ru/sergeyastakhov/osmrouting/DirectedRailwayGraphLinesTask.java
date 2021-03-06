/**
 * $Id$
 * <p>
 * Copyright (C) 2012-2017 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmrouting;

import org.openstreetmap.osmosis.core.container.v0_6.*;
import org.openstreetmap.osmosis.core.domain.v0_6.CommonEntityData;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.core.task.v0_6.SinkSource;

import java.util.*;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class DirectedRailwayGraphLinesTask implements SinkSource, EntityProcessor
{
  private static final String TAG_ONEWAY = "oneway";

  private Sink sink;

  private IdFactory wayIdFactory = EntityIdFactory.wayIdFactory;

  private boolean anyDirection;

  public DirectedRailwayGraphLinesTask(boolean _anyDirection)
  {
    anyDirection = _anyDirection;
  }

  @Override
  public void initialize(Map<String, Object> metaData) { sink.initialize(metaData); }

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
  public void close()
  {
    sink.close();
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

    Map<RailwayAccessMode, String> wayAccessMap = new EnumMap<>(RailwayAccessMode.class);

    Map<RailwayAccessMode, Boolean> directOnewayAccessMap = new EnumMap<>(RailwayAccessMode.class);
    Map<RailwayAccessMode, Boolean> returnOnewayAccessMap = new EnumMap<>(RailwayAccessMode.class);

    Map<String, String> directNonAccessTagsMap = new HashMap<>();
    Map<String, String> returnNonAccessTagsMap = new HashMap<>();

    RailwayType railwayType = RailwayType.getWayType(directLine);

    if( railwayType == null )
      return;

    RailwayAccessMode graphMinimumAccess = anyDirection ? RailwayAccessMode.ACCESS : railwayType.getDefaultAccessMode();

    // Generate access tags for lines from oneway tag
    for( Tag tag : directLine.getTags() )
    {
      String tagKey = tag.getKey();
      String tagValue = tag.getValue();

      if( tagKey.contains(TAG_ONEWAY) )
      {
        RailwayAccessMode accessMode = railwayType.getDefaultAccessMode();

        int keyLength = tagKey.length();
        if( keyLength > 7 )
        {
          if( tagKey.endsWith(TAG_ONEWAY) )
          {
            // xxx:oneway tag
            accessMode = RailwayAccessMode.fromString(tagKey.substring(0, keyLength - 7));
          }
          else if( tagKey.startsWith(TAG_ONEWAY) )
          {
            // oneway:xxx tag
            accessMode = RailwayAccessMode.fromString(tagKey.substring(7, keyLength));
          }
          else
          {
            accessMode = null;
          }
        }

        if( accessMode != null )
        {
          switch( tagValue )
          {
            case "yes":
//            directOnewayAccessMap.put(accessMode, true);
              returnOnewayAccessMap.put(accessMode, false);
              break;
            case "-1":
              directOnewayAccessMap.put(accessMode, false);
              returnOnewayAccessMap.remove(accessMode);
              break;
            case "no":
              directOnewayAccessMap.remove(accessMode);
              returnOnewayAccessMap.remove(accessMode);
              break;
          }

          continue;
        }
      }
      else
      {
        RailwayAccessMode accessMode = RailwayAccessMode.fromString(tagKey);
        if( accessMode != null )
        {
          wayAccessMap.put(accessMode, tagValue);
          continue;
        }
      }

      directNonAccessTagsMap.put(tagKey, tagValue);
      returnNonAccessTagsMap.put(tagKey, tagValue);
    }

    // Атрибуты с суффиксами :forward/:backward перекрывают
    // эквивалентные теги без суффиксов в соответствующем направлении.

    for( String tagKey : new HashSet<>(directNonAccessTagsMap.keySet()) )
    {
      if( tagKey.endsWith(":forward") )
      {
        returnNonAccessTagsMap.remove(tagKey);

        String tagValue = directNonAccessTagsMap.get(tagKey);
        directNonAccessTagsMap.put(tagKey.substring(0, tagKey.lastIndexOf(':')), tagValue);
      }
      else if( tagKey.endsWith(":backward") )
      {
        directNonAccessTagsMap.remove(tagKey);

        String tagValue = returnNonAccessTagsMap.get(tagKey);
        returnNonAccessTagsMap.put(tagKey.substring(0, tagKey.lastIndexOf(':')), tagValue);
      }
    }

    // Build and send direct line

    Map<RailwayAccessMode, String> directAccessMap = RailwayAccessMode.restrictTags(wayAccessMap, directOnewayAccessMap);

    if( graphMinimumAccess.hasAnySubModeAccess(directAccessMap) )
    {
      Map<String, String> directLineTagsMap = new HashMap<>();

      for( Map.Entry<RailwayAccessMode, String> entry : directAccessMap.entrySet() )
      {
        directLineTagsMap.put(entry.getKey().toString(), entry.getValue());
      }

      // Add nonaccess tags
      directLineTagsMap.putAll(directNonAccessTagsMap);

      fillTagsFromMap(directLine.getTags(), directLineTagsMap);

      // Send direct line
      sink.process(new WayContainer(directLine));
    }

    // Build and send return line

    Map<RailwayAccessMode, String> returnAccessMap = RailwayAccessMode.restrictTags(wayAccessMap, returnOnewayAccessMap);

    if( graphMinimumAccess.hasAnySubModeAccess(returnAccessMap) )
    {
      Map<String, String> returnLineTagsMap = new HashMap<>();

      for( Map.Entry<RailwayAccessMode, String> entry : returnAccessMap.entrySet() )
      {
        returnLineTagsMap.put(entry.getKey().toString(), entry.getValue());
      }

      // Add nonaccess tags
      returnLineTagsMap.putAll(returnNonAccessTagsMap);

      if( !returnLineTagsMap.containsKey("original_id") )
      {
        returnLineTagsMap.put("original_id", String.valueOf(directLine.getId()));
      }

      CommonEntityData entityData = new CommonEntityData
          (wayIdFactory.nextId(), 1, directLine.getTimestamp(), directLine.getUser(), 0);

      List<WayNode> wayNodeList = new ArrayList<>(directLine.getWayNodes());
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
