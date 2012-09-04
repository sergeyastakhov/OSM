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

    Map<String, String> directLineTagsMap = new HashMap<String, String>();
    Map<String, String> returnLineTagsMap = new HashMap<String, String>();

    Map<String, String> passedTagsMap = new HashMap<String, String>();

    // Generate access tags for lines from oneway tag
    for( Tag tag : directLine.getTags() )
    {
      String tagKey = tag.getKey();
      String tagValue = tag.getValue();

      if( tagKey.endsWith(TAG_ONEWAY) )
      {
        String accessMode = TAG_VEHICLE;

        int keyLength = tagKey.length();
        if( keyLength > 7 )
        {
         // xxx:oneway tag
          accessMode = tagKey.substring(0, keyLength - 7);
        }

        if( tagValue.equals("yes") )
        {
          directLineTagsMap.put(accessMode, "yes");
          returnLineTagsMap.put(accessMode, "no");
        }
        else if( tagValue.equals("-1") )
        {
          directLineTagsMap.put(accessMode, "no");
          returnLineTagsMap.put(accessMode, "yes");
        }
        else if( tagValue.equals("no") )
        {
          directLineTagsMap.put(accessMode, "yes");
          returnLineTagsMap.put(accessMode, "yes");
        }
      }
      else
      {
        passedTagsMap.put(tagKey, tagValue);
      }
    }

    // Put other tags over generated one
    directLineTagsMap.putAll(passedTagsMap);
    returnLineTagsMap.putAll(passedTagsMap);

    fillTagsFromMap(directLine.getTags(), directLineTagsMap);

    // Send direct line
    sink.process(new WayContainer(directLine));

    // Build and send return line
    CommonEntityData entityData = new CommonEntityData
      (wayIdFactory.nextId(), 1, directLine.getTimestamp(), directLine.getUser(), 0);

    List<WayNode> wayNodeList = new ArrayList<WayNode>(directLine.getWayNodes());
    Collections.reverse(wayNodeList);
    Way returnLine = new Way(entityData, wayNodeList);

    fillTagsFromMap(returnLine.getTags(), returnLineTagsMap);

    sink.process(new WayContainer(returnLine));
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
