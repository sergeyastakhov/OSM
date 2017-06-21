/**
 * TagAreaContentTask.java
 * <p>
 * Copyright (C) 2017 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmareatag;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.index.strtree.STRtree;
import org.openstreetmap.osmosis.core.container.v0_6.*;
import org.openstreetmap.osmosis.core.domain.v0_6.*;
import org.openstreetmap.osmosis.core.filter.common.IdTracker;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerFactory;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerType;
import org.openstreetmap.osmosis.core.lifecycle.ReleasableIterator;
import org.openstreetmap.osmosis.core.store.IndexedObjectStore;
import org.openstreetmap.osmosis.core.store.IndexedObjectStoreReader;
import org.openstreetmap.osmosis.core.store.SimpleObjectStore;
import org.openstreetmap.osmosis.core.store.SingleClassObjectSerializationFactory;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.core.task.v0_6.SinkSource;

import java.util.*;
import java.util.logging.Logger;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class TagRelationContentTask implements SinkSource, EntityProcessor
{
  private static final Logger log = Logger.getLogger(TagRelationContentTask.class.getName());

  private Sink sink;

  private String relationTagName;
  private Set<String> relationTagValues;

  private String markEntityTagName;
  private Set<String> markEntityTagValues;

  private Map<Long, Set<Relation>> wayIdToRelationsMap;

  private SimpleObjectStore<WayContainer> allWays;
  private SimpleObjectStore<RelationContainer> allRelations;

  private Tag insideTag;
  private Tag outsideTag;

  public TagRelationContentTask
      (String _relationTagName, String[] _relationTagValues,
       String _markEntityTagName, String[] _markEntityTagValues,
       Tag _insideTag, Tag _outsideTag)
  {
    relationTagName = _relationTagName;
    relationTagValues = new HashSet<>(_relationTagValues.length);
    relationTagValues.addAll(Arrays.asList(_relationTagValues));

    markEntityTagName = _markEntityTagName;
    markEntityTagValues = new HashSet<>(_markEntityTagValues.length);
    markEntityTagValues.addAll(Arrays.asList(_markEntityTagValues));

    insideTag = _insideTag;
    outsideTag = _outsideTag;
  }

  @Override
  public void initialize(Map<String, Object> metaData)
  {
    sink.initialize(metaData);

    wayIdToRelationsMap = new HashMap<>();

    allWays = new SimpleObjectStore<>(
        new SingleClassObjectSerializationFactory(WayContainer.class), "trct_awy", true);
    allRelations = new SimpleObjectStore<>(
        new SingleClassObjectSerializationFactory(RelationContainer.class), "trct_arl", true);
  }


  @Override
  public void process(BoundContainer bound) { sink.process(bound); }

  @Override
  public void process(NodeContainer nodeContainer)
  {
    sink.process(nodeContainer);
  }

  @Override
  public void process(WayContainer wayContainer)
  {
    allWays.add(wayContainer);
  }

  @Override
  public void process(RelationContainer relationContainer)
  {
    allRelations.add(relationContainer);

    Relation relation = relationContainer.getEntity();

    Map<String, String> tags = ((TagCollection) relation.getTags()).buildMap();

    String relationTagValue = tags.get(relationTagName);

    if( relationTagValue != null && relationTagValues.contains(relationTagValue) )
    {
      for( RelationMember relationMember : relation.getMembers() )
      {
        EntityType memberType = relationMember.getMemberType();
        if( memberType == EntityType.Way )
        {
          long memberId = relationMember.getMemberId();

          Set<Relation> relations = wayIdToRelationsMap.get(memberId);
          if( relations == null )
          {
            relations = new HashSet<>();
            wayIdToRelationsMap.put(memberId, relations);
          }

          relations.add(relation);
        }
      }
    }
  }

  @Override
  public void process(EntityContainer entityContainer)
  {
    entityContainer.process(this);
  }

  @Override
  public void complete()
  {
    allWays.complete();
    allRelations.complete();

    log.info("Send data");

    ReleasableIterator<WayContainer> wayIterator = allWays.iterate();
    while( wayIterator.hasNext() )
    {
      WayContainer wayContainer = wayIterator.next();
      Way way = wayContainer.getEntity();

      Map<String, String> tags = ((TagCollection) way.getTags()).buildMap();

      String tagValue = tags.get(markEntityTagName);

      if( tagValue != null && (markEntityTagValues.isEmpty() || markEntityTagValues.contains(tagValue)) )
      {
        wayContainer = markEntityRelation(wayContainer);
      }

      sink.process(wayContainer);
    }

    wayIterator.release();


    ReleasableIterator<RelationContainer> relationIterator = allRelations.iterate();
    while( relationIterator.hasNext() )
    {
      RelationContainer relationContainer = relationIterator.next();

      sink.process(relationContainer);
    }

    relationIterator.release();

    log.info("Sending complete.");

    sink.complete();
  }

  private WayContainer markEntityRelation(WayContainer entityContainer)
  {
    entityContainer = entityContainer.getWriteableInstance();

    Entity entity = entityContainer.getEntity();

    Set<Relation> relations = wayIdToRelationsMap.get(entity.getId());

    if( relations == null || relations.isEmpty() )
    {
      tagOutsideEntity(entity);
    }
    else
    {
      for( Relation relation : relations )
      {
        tagInsideEntity(entity, relation);
      }
    }

    return entityContainer;
  }

  private void tagInsideEntity(Entity entity, Relation relation)
  {
    if( insideTag == null )
      return;

    Map<String, String> relationTags = ((TagCollection) relation.getTags()).buildMap();

    KeywordSubst tagResolver = new KeywordSubst(relationTags);

    Tag markTag = new Tag(tagResolver.resolve(insideTag.getKey()), tagResolver.resolve(insideTag.getValue()));

    Collection<Tag> tags = entity.getTags();

    for( Tag tag : tags )
    {
      String tagName = tag.getKey();

      if( tagName.equals(markTag.getKey()) )
      {
        return;
      }
    }

    tags.add(markTag);
  }

  private void tagOutsideEntity(Entity entity)
  {
    if( outsideTag == null )
      return;

    Collection<Tag> tags = entity.getTags();

    for( Tag tag : tags )
    {
      String tagName = tag.getKey();

      if( tagName.equals(outsideTag.getKey()) )
      {
        return;
      }
    }

    tags.add(outsideTag);
  }

  @Override
  public void release()
  {
    sink.release();

    allWays.release();
    allRelations.release();

    wayIdToRelationsMap = null;
  }

  @Override
  public void setSink(Sink _sink) { sink = _sink; }
}
