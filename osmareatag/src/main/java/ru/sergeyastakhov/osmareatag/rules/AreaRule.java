/**
 * AreaRule.java
 * <p>
 * Copyright (C) 2017 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmareatag.rules;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.index.strtree.STRtree;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.TagCollection;
import org.openstreetmap.osmosis.core.lifecycle.ReleasableIterator;
import org.openstreetmap.osmosis.core.store.GenericObjectSerializationFactory;
import org.openstreetmap.osmosis.core.store.SimpleObjectStore;
import org.openstreetmap.osmosis.core.task.v0_6.Initializable;
import ru.sergeyastakhov.osmareatag.EntityArea;
import ru.sergeyastakhov.osmareatag.EntityGeometryFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class AreaRule extends MatchOwner implements Initializable
{
  private static final Logger log = Logger.getLogger(AreaRule.class.getName());

  private String id;
  private File cacheFile;

  private SimpleObjectStore<EntityContainer> areaEntities;

  private STRtree areaIndex;
  private boolean indexLoaded;

  public AreaRule(String _id, String _cacheFile)
  {
    id = _id;
    cacheFile = _cacheFile != null ? new File(_cacheFile) : null;
  }

  public String getId() { return id; }

  @Override
  public void initialize(Map<String, Object> metaData)
  {
    areaEntities = new SimpleObjectStore<>(
        new GenericObjectSerializationFactory(), "area_entity", true);

    areaIndex = new STRtree();

    if( cacheFile != null && cacheFile.exists() )
    {
      log.info("Loading index cache file " + cacheFile);

      try (ObjectInputStream ois = new ObjectInputStream(
          new BufferedInputStream(new FileInputStream(cacheFile))))
      {
        areaIndex = (STRtree) ois.readObject();
        indexLoaded = true;
      }
      catch( Exception ex )
      {
        log.log(Level.WARNING, "Error loading index cache file " + cacheFile + " : " + ex, ex);
      }
    }
  }

  @Override
  public void complete()
  {
    areaEntities.complete();
  }

  @Override
  public void release()
  {
    areaEntities.release();
    areaIndex = null;
    indexLoaded = false;
  }

  public void prepareAreaRules(EntityContainer entityContainer, Map<String, String> tags)
  {
    if( indexLoaded )
      return;

    if( matchRule.match(entityContainer.getEntity().getType(), tags) )
    {
      areaEntities.add(entityContainer);
    }
  }

  public void buildIndexes(EntityGeometryFactory geometryFactory)
  {
    if( indexLoaded )
      return;

    ReleasableIterator<EntityContainer> areaEntityIterator = areaEntities.iterate();
    while( areaEntityIterator.hasNext() )
    {
      EntityContainer entityContainer = areaEntityIterator.next();

      Entity entity = entityContainer.getEntity();

      long entityId = entity.getId();
      String entityName = entity.getType() + "#" + entityId;

      try
      {
        Geometry geometry = geometryFactory.createAreaGeometry(entity, entityName);

        if( geometry != null )
        {
          Map<String, String> tags = ((TagCollection) entity.getTags()).buildMap();

          if( geometry instanceof GeometryCollection )
          {
            GeometryCollection collection = (GeometryCollection) geometry;

            int numGeometries = collection.getNumGeometries();

            log.info("Adding entity " + entity + " name=" + tags.get("name") + " (" + numGeometries + " components) to the index " + id);

            for( int i = 0; i < numGeometries; i++ )
            {
              Geometry geometryN = collection.getGeometryN(i);

              areaIndex.insert(geometryN.getEnvelopeInternal(), new EntityArea(geometryN, tags));
            }
          }
          else
          {
            log.fine("Adding entity " + entity + " name=" + tags.get("name") + " to the index " + id);

            areaIndex.insert(geometry.getEnvelopeInternal(), new EntityArea(geometry, tags));
          }
        }
        else
        {
          log.fine("Error processing " + entityName + " : Invalid geometry");
        }
      }
      catch( Exception ex )
      {
        log.warning("Error processing " + entityName + " : " + ex);
      }
    }

    areaEntityIterator.release();

    areaIndex.build();

    if( cacheFile != null )
    {
      log.info("Writing index cache file " + cacheFile);

      try (ObjectOutputStream oos = new ObjectOutputStream(
          new BufferedOutputStream(new FileOutputStream(cacheFile))))
      {
        oos.writeObject(areaIndex);
      }
      catch( IOException ex )
      {
        log.log(Level.SEVERE, "Error writing index cache file " + cacheFile + " : " + ex, ex);
      }
    }
  }

  public Collection<EntityArea> getAreas(Geometry geometry)
  {
    Collection<EntityArea> insideAreas = new ArrayList<>();

    List<EntityArea> areas = areaIndex.query(geometry.getEnvelopeInternal());

    for( EntityArea area : areas )
    {
      if( area.isInside(geometry) )
      {
        insideAreas.add(area);
      }
    }

    return insideAreas;
  }
}
