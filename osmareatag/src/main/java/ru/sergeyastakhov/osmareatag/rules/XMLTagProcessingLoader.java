/**
 * XMLTagProcessingLoader.java
 * <p>
 *  Copyright (C) 2017 Sergey Astakhov. All Rights Reserved
 */
package ru.sergeyastakhov.osmareatag.rules;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;

/**
 * @author Sergey Astakhov
 * @version $Revision$
 */
public class XMLTagProcessingLoader
{
  public TagProcessing load(String file) throws SAXException, ParserConfigurationException, IOException
  {
    SAXParserFactory factory = SAXParserFactory.newInstance();

    SAXParser saxParser = factory.newSAXParser();
    XMLReader reader = saxParser.getXMLReader();
    reader.setFeature("http://xml.org/sax/features/namespaces", true);

    XMLTagRulesHandler handler = new XMLTagRulesHandler();
    reader.setContentHandler(handler);
    reader.parse(file);

    return handler.getTagProcessing();
  }
}
