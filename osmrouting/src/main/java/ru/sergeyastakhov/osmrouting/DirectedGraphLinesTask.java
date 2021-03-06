/**
 * $Id$
 * <p>
 * Copyright (C) 2012-2017 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmrouting;

import org.openstreetmap.osmosis.core.container.v0_6.*;
import org.openstreetmap.osmosis.core.domain.v0_6.*;
import org.openstreetmap.osmosis.core.lifecycle.ReleasableIterator;
import org.openstreetmap.osmosis.core.store.SimpleObjectStore;
import org.openstreetmap.osmosis.core.store.SingleClassObjectSerializationFactory;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.core.task.v0_6.SinkSource;

import java.util.*;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class DirectedGraphLinesTask implements SinkSource, EntityProcessor
{
  private static final String TAG_ONEWAY = "oneway";
  private static final String TAG_VEHICLE = "vehicle";

  private Sink sink;

  private SimpleObjectStore<RelationContainer> allRelations;

  private Map<Long, IdReplacement> fromWayIdReplacement = new HashMap<>();
  private Map<Long, IdReplacement> toWayIdReplacement = new HashMap<>();

  private IdFactory wayIdFactory = EntityIdFactory.wayIdFactory;

  private AccessMode graphMinimumAccess;

  public DirectedGraphLinesTask(boolean vehicleOnly)
  {
    graphMinimumAccess = vehicleOnly ? AccessMode.VEHICLE : AccessMode.ACCESS;
  }

  @Override
  public void initialize(Map<String, Object> metaData)
  {
    allRelations = new SimpleObjectStore<>(
        new SingleClassObjectSerializationFactory(RelationContainer.class), "dglt_rl", true);

    sink.initialize(metaData);
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
    ReleasableIterator<RelationContainer> relationIterator = allRelations.iterate();

    while( relationIterator.hasNext() )
    {
      RelationContainer relationContainer = relationIterator.next();

      Relation relation = relationContainer.getEntity();

      // Replace way id in restrictions

      RelationMember viaNode = null;

      for( RelationMember member : relation.getMembers() )
      {
        String role = member.getMemberRole();

        if( role.equals("via") && member.getMemberType() == EntityType.Node )
          viaNode = member;
      }

      if( viaNode != null )
      {
        long viaNodeId = viaNode.getMemberId();

        if( fromWayIdReplacement.containsKey(viaNodeId) || toWayIdReplacement.containsKey(viaNodeId) )
        {
          relation = relation.getWriteableInstance();

          List<RelationMember> members = relation.getMembers();

          for( int i = 0; i < members.size(); i++ )
          {
            RelationMember member = members.get(i);

            if( member.getMemberType() == EntityType.Way )
            {
              String role = member.getMemberRole();

              IdReplacement idReplacement = null;

              if( role.equals("from") )
              {
                idReplacement = fromWayIdReplacement.get(viaNodeId);
              }
              else
              if( role.equals("to") )
              {
                idReplacement = toWayIdReplacement.get(viaNodeId);
              }

              if( idReplacement!=null && member.getMemberId() == idReplacement.originalId )
                members.set(i, new RelationMember(idReplacement.newId, member.getMemberType(), role));
            }
          }

          relationContainer = new RelationContainer(relation);
        }
      }

      sink.process(relationContainer);
    }

    relationIterator.close();

    allRelations.complete();

    sink.complete();
  }

  @Override
  public void close()
  {
    allRelations.close();

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

    Map<AccessMode, String> wayAccessMap = new EnumMap<>(AccessMode.class);

    Map<AccessMode, Boolean> directOnewayAccessMap = new EnumMap<>(AccessMode.class);
    Map<AccessMode, Boolean> returnOnewayAccessMap = new EnumMap<>(AccessMode.class);

    Map<String, String> directNonAccessTagsMap = new HashMap<>();
    Map<String, String> returnNonAccessTagsMap = new HashMap<>();

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
        AccessMode accessMode = AccessMode.fromString(tagKey);
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

    Map<AccessMode, String> directAccessMap = AccessMode.restrictTags(wayAccessMap, directOnewayAccessMap);

    if( graphMinimumAccess.hasAnySubModeAccess(directAccessMap) )
    {
      Map<String, String> directLineTagsMap = new HashMap<>();

      for( Map.Entry<AccessMode, String> entry : directAccessMap.entrySet() )
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

    Map<AccessMode, String> returnAccessMap = AccessMode.restrictTags(wayAccessMap, returnOnewayAccessMap);

    if( graphMinimumAccess.hasAnySubModeAccess(returnAccessMap) )
    {
      Map<String, String> returnLineTagsMap = new HashMap<>();

      for( Map.Entry<AccessMode, String> entry : returnAccessMap.entrySet() )
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

      IdReplacement idReplacement = new IdReplacement(directLine.getId(), returnLine.getId());

      WayNode firstNode = wayNodeList.get(0);
      toWayIdReplacement.put(firstNode.getNodeId(), idReplacement);

      WayNode lastNode = wayNodeList.get(wayNodeList.size()-1);
      fromWayIdReplacement.put(lastNode.getNodeId(), idReplacement);
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
