/**
 * XMLTagRulesHandler.java
 * <p>
 *  Copyright (C) 2017 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmareatag.rules;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class XMLTagRulesHandler extends DefaultHandler
{
  private TagProcessing tagProcessing;

  private AreaRule areaRule;
  private TransformRule transformRule;

  private MatchOwner matchOwner;
  private MatchRule matchRule;

  private OutputRule outputRule;

  private StringBuilder currentText = new StringBuilder();

  public TagProcessing getTagProcessing()
  {
    return tagProcessing;
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
  {
    switch( localName )
    {
      case "tag-processing":
        tagProcessing = new TagProcessing();
        break;
      case "area":
      {
        String id = attributes.getValue("id");
        String cacheFile = attributes.getValue("cache-file");
        matchOwner = areaRule = new AreaRule(id, cacheFile);
      }
      break;
      case "transform":
        matchOwner = transformRule = new TransformRule();
        break;
      case "match":
        if( matchOwner != null )
        {
          String type = attributes.getValue("type");
          matchRule = new MatchRule(type);
        }
        break;
      case "tag":
        if( matchRule != null )
        {
          String key = attributes.getValue("k");
          String value = attributes.getValue("v");
          matchRule.add(new TagRule(key, value));
        }
        break;
      case "inside":
        if( matchRule != null )
        {
          String area = attributes.getValue("area");
          matchRule.add(new InsideRule(area));
        }
        break;
      case "outside":
        if( matchRule != null )
        {
          String area = attributes.getValue("area");
          matchRule.add(new OutsideRule(area));
        }
        break;
      case "name":
        if( transformRule != null )
        {
          currentText = new StringBuilder();
        }
        break;
      case "output":
        if( transformRule != null )
        {
          outputRule = new OutputRule();
        }
        break;
      case "add-tag":
        if( outputRule != null )
        {
          String key = attributes.getValue("k");
          String value = attributes.getValue("v");
          String contextArea = attributes.getValue("context-area");
          outputRule.add(new AddTagRule(key, value, contextArea));
        }
        break;
      case "set-tag":
        if( outputRule != null )
        {
          String key = attributes.getValue("k");
          String value = attributes.getValue("v");
          String contextArea = attributes.getValue("context-area");
          outputRule.add(new SetTagRule(key, value, contextArea));
        }
        break;
    }
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException
  {
    if( currentText != null ) currentText.append(ch, start, length);
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException
  {
    switch( localName )
    {
      case "area":
        tagProcessing.add(areaRule);
        matchOwner = areaRule = null;
        break;
      case "transform":
        tagProcessing.add(transformRule);
        matchOwner = transformRule = null;
        break;
      case "match":
        matchOwner.setMatchRule(matchRule);
        matchRule = null;
        break;
      case "name":
        if( transformRule != null )
        {
          transformRule.setName(currentText.toString());
        }
        break;
      case "output":
        if( transformRule != null )
        {
          transformRule.setOutputRule(outputRule);
        }
        break;
    }
  }
}
