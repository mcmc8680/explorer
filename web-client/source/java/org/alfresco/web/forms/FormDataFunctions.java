/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>. */
package org.alfresco.web.forms;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.alfresco.model.WCMAppModel;
import org.alfresco.repo.domain.PropertyValue;
import org.alfresco.service.cmr.avm.AVMNodeDescriptor;
import org.alfresco.service.cmr.remote.AVMRemote;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Common implementation of functions called in the context of FormDataRenderers.
 * This uses AVMRemote rather than AVMService so that in can be used in the context
 * of both the alfresco webapp and the virtualization server.
 *
 * @author Ariel Backenroth
 * @author Arseny Kovalchuk (Fix for the ETWOONE-241 and ETWOTWO-504 issues)
 */
public class FormDataFunctions
{
   private static final Log LOGGER = LogFactory.getLog(FormDataFunctions.class);

   private final AVMRemote avmRemote;
   
   private ThreadLocal<DocumentBuilderFactory> dbf = new ThreadLocal<DocumentBuilderFactory>();

   public FormDataFunctions(final AVMRemote avmRemote)
   {
      this.avmRemote = avmRemote;
   }

   /**
    * Loads and parses an xml document at the specified path using avm remote.
    *
    * @param avmPath a path within the avm repository.
    * @return the parsed document.
    */
   public Document parseXMLDocument(final String avmPath)
      throws IOException,
      SAXException
   {
      final InputStream istream = this.avmRemote.getFileInputStream(-1, avmPath); 
      try 
      {
         return parseXML(istream);
      }
      finally
      {
         istream.close();
      }
   }

   /**
    * Loads and parses all xml documents at the specified path generated by the
    * specified form using avm remote.
    *
    * @param formName a form name
    * @param avmPath a path within the avm repository.
    * @return the parsed document.
    */
   public Map<String, Document> parseXMLDocuments(final String formName, final String avmPath)
      throws IOException,
      SAXException
   {
      final Map<String, AVMNodeDescriptor> entries = 
         this.avmRemote.getDirectoryListing(-1, avmPath);
      final Map<String, Document> result = new HashMap<String, Document>();
      for (Map.Entry<String, AVMNodeDescriptor> entry : entries.entrySet())
      {
         final String entryName = entry.getKey();
         AVMNodeDescriptor entryNode = entry.getValue();
         if (entryNode.isFile())
         {
            PropertyValue pv = 
               this.avmRemote.getNodeProperty(-1, 
                                              avmPath + '/' + entryName,
                                              WCMAppModel.PROP_PARENT_FORM_NAME);
            if (pv == null || 
                pv.getStringValue() == null || 
                !((String)pv.getStringValue()).equals(formName))
            {
               // it's not generated by the same template type
               continue;
            }

            pv = this.avmRemote.getNodeProperty(-1, 
                                                avmPath + '/' + entryName,
                                                WCMAppModel.PROP_PARENT_RENDERING_ENGINE_TEMPLATE);
            
            if (pv != null)
            {
               // it's generated by a rendering engine (it's not the xml file)
               continue;
            }
            final InputStream istream = this.avmRemote.getFileInputStream(-1, avmPath + '/' + entryName);
            try
            {
               // result.put(entryName, XMLUtil.parse(istream));
                result.put(entryName, parseXML(istream));
            }
            finally
            {
               istream.close();
            }
         }
      }
      return result;
   }
   
   /*
    *  We need an internal method for XML parsing with ThreadLocal DocumentBuilderFactory
    *  to avoid a multithread access to the parser in XMLUtils.
    *  Fix of the bug reported in https://issues.alfresco.com/jira/browse/ETWOONE-241 reported.
    */
   private Document parseXML(InputStream is) throws IOException, SAXException
   {
      Document result = null;
      try 
      {
         DocumentBuilderFactory localDbf = dbf.get();
         if (localDbf == null)
         {
            localDbf = DocumentBuilderFactory.newInstance();
         }
         localDbf.setNamespaceAware(true);
         localDbf.setValidating(false);
         dbf.set(localDbf);
         DocumentBuilder builder = localDbf.newDocumentBuilder();
         result = builder.parse(is);
      }
      catch (ParserConfigurationException pce)
      {
         LOGGER.error(pce);
      }
      
      return result;
   }
   
   
   /**
    * Encodes invalid HTML characters. (Fix for ETWOTWO-504 issue)
    * This code was adopted from WebDAVHelper.encodeHTML() method with some restrictions.
    * @see press-relese.xsl for pattern. 
    * 
    * @param text to encode
    * @return encoded text
    * @throws IOException
    * @throws SAXException
    */
   public String encodeQuotes(String text) throws IOException, SAXException
   {
       if (text == null)
       {
           return "";
       }
       
       StringBuilder sb = null;      //create on demand
       String enc;
       char c;
       for (int i = 0; i < text.length(); i++)
       {
           enc = null;
           c = text.charAt(i);
           switch (c)
           {
               case '"': enc = "&quot;"; break;    //"
               //case '&': enc = "&amp;"; break;     //&
               //case '<': enc = "&lt;"; break;      //<
               //case '>': enc = "&gt;"; break;      //>
               
               //german umlauts
               case '\u00E4' : enc = "&auml;";  break;
               case '\u00C4' : enc = "&Auml;";  break;
               case '\u00F6' : enc = "&ouml;";  break;
               case '\u00D6' : enc = "&Ouml;";  break;
               case '\u00FC' : enc = "&uuml;";  break;
               case '\u00DC' : enc = "&Uuml;";  break;
               case '\u00DF' : enc = "&szlig;"; break;
               
               //misc
               //case 0x80: enc = "&euro;"; break;  sometimes euro symbol is ascii 128, should we suport it?
               case '\u20AC': enc = "&euro;";  break;
               case '\u00AB': enc = "&laquo;"; break;
               case '\u00BB': enc = "&raquo;"; break;
               case '\u00A0': enc = "&nbsp;"; break;
               
               //case '': enc = "&trade"; break;
               
               default:
                   if (((int)c) >= 0x80)
                   {
                       //encode all non basic latin characters
                       enc = "&#" + ((int)c) + ";";
                   }
               break;
           }
           
           if (enc != null)
           {
               if (sb == null)
               {
                   String soFar = text.substring(0, i);
                   sb = new StringBuilder(i + 8);
                   sb.append(soFar);
               }
               sb.append(enc);
           }
           else
           {
               if (sb != null)
               {
                   sb.append(c);
               }
           }
       }
       
       if (sb == null)
       {
           return text;
       }
       else
       {
           return sb.toString();
       }
   }
}