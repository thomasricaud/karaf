/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.sandbox.scrplugin.xml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.felix.sandbox.scrplugin.om.metatype.AttributeDefinition;
import org.apache.felix.sandbox.scrplugin.om.metatype.Designate;
import org.apache.felix.sandbox.scrplugin.om.metatype.MTObject;
import org.apache.felix.sandbox.scrplugin.om.metatype.MetaData;
import org.apache.felix.sandbox.scrplugin.om.metatype.OCD;
import org.apache.maven.plugin.MojoExecutionException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;


/**
 * <code>MetaType</code>
 *
 * is a helper class to read and write meta type service files.
 *
 */
public class MetaTypeIO {

    private static final SAXTransformerFactory FACTORY = (SAXTransformerFactory) TransformerFactory.newInstance();

    public static final String NAMESPACE_URI = "http://www.osgi.org/xmlns/metatype/v1.0.0";

    public static final String PREFIX = "metatype";

    protected static final String METADATA_ELEMENT = "MetaData";
    protected static final String METADATA_ELEMENT_QNAME = PREFIX + ':' + METADATA_ELEMENT;

    protected static final String OCD_ELEMENT = "OCD";
    protected static final String OCD_ELEMENT_QNAME = PREFIX + ':' + OCD_ELEMENT;

    protected static final String DESIGNATE_ELEMENT = "Designate";
    protected static final String DESIGNATE_ELEMENT_QNAME = PREFIX + ':' + DESIGNATE_ELEMENT;

    protected static final String OBJECT_ELEMENT = "Object";
    protected static final String OBJECT_ELEMENT_QNAME = PREFIX + ':' + OBJECT_ELEMENT;

    protected static final String AD_ELEMENT = "AD";
    protected static final String AD_ELEMENT_QNAME = PREFIX + ':' + AD_ELEMENT;

    public static void write(MetaData metaData, File file)
    throws MojoExecutionException {
        try {
            FileWriter writer = new FileWriter(file);
            final TransformerHandler transformerHandler = FACTORY.newTransformerHandler();
            final Transformer transformer = transformerHandler.getTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformerHandler.setResult(new StreamResult(writer));

            generateXML(metaData, transformerHandler);
        } catch (TransformerException e) {
            throw new MojoExecutionException("Unable to write xml to " + file, e);
        } catch (SAXException e) {
            throw new MojoExecutionException("Unable to generate xml for " + file, e);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to write xml to " + file, e);
        }
    }

    /**
     * Generate the xml top level element and start streaming
     * the meta data.
     * @param metaData
     * @param contentHandler
     * @throws SAXException
     */
    protected static void generateXML(MetaData metaData, ContentHandler contentHandler)
    throws SAXException {
        contentHandler.startDocument();
        contentHandler.startPrefixMapping(PREFIX, NAMESPACE_URI);

        final AttributesImpl ai = new AttributesImpl();
        addAttribute(ai, "localization", metaData.getLocalization());

        contentHandler.startElement(NAMESPACE_URI, METADATA_ELEMENT, METADATA_ELEMENT_QNAME, ai);

        final Iterator i = metaData.getDescriptors().iterator();
        while ( i.hasNext() ) {
            final Object obj = i.next();
            if ( obj instanceof OCD ) {
                generateXML((OCD)obj, contentHandler);
            } else {
                generateXML((Designate)obj, contentHandler);
            }
        }
        // end wrapper element
        contentHandler.endElement(NAMESPACE_URI, METADATA_ELEMENT, METADATA_ELEMENT_QNAME);
        contentHandler.endPrefixMapping(PREFIX);
        contentHandler.endDocument();
    }

    protected static void generateXML(OCD ocd, ContentHandler contentHandler)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        addAttribute(ai, "id", ocd.getId());
        addAttribute(ai, "name", ocd.getName());
        addAttribute(ai, "description", ocd.getDescription());
        contentHandler.startElement(NAMESPACE_URI, OCD_ELEMENT, OCD_ELEMENT_QNAME, ai);

        final Iterator i = ocd.getProperties().iterator();
        while ( i.hasNext() ) {
            final AttributeDefinition ad = (AttributeDefinition) i.next();
            generateXML(ad, contentHandler);
        }

        contentHandler.endElement(NAMESPACE_URI, OCD_ELEMENT, OCD_ELEMENT_QNAME);
    }

    protected static void generateXML(AttributeDefinition ad, ContentHandler contentHandler)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        addAttribute(ai, "id", ad.getId());
        addAttribute(ai, "type", ad.getType());
        if ( ad.getDefaultMultiValue() != null ) {
            final StringBuffer buf = new StringBuffer();
            for(int i=0; i<ad.getDefaultMultiValue().length; i++) {
                if ( i > 0 ) {
                    buf.append(',');
                }
                buf.append(ad.getDefaultMultiValue()[i]);
            }
            addAttribute(ai, "default", buf);
        } else {
            addAttribute(ai, "default", ad.getDefaultValue());
        }
        addAttribute(ai, "name", ad.getName());
        addAttribute(ai, "description", ad.getDescription());
        addAttribute(ai, "cardinality", ad.getCardinality());
        contentHandler.startElement(NAMESPACE_URI, AD_ELEMENT, AD_ELEMENT_QNAME, ai);

        if (ad.getOptions() != null) {
            for (Iterator oi=ad.getOptions().entrySet().iterator(); oi.hasNext(); ) {
                final Map.Entry entry = (Map.Entry) oi.next();
                ai.clear();
                addAttribute(ai, "value", String.valueOf(entry.getKey()));
                addAttribute(ai, "label", String.valueOf(entry.getValue()));
                contentHandler.startElement(NAMESPACE_URI, "Option", PREFIX + ':' + "Option", ai);
            }
        }

        contentHandler.endElement(NAMESPACE_URI, AD_ELEMENT, AD_ELEMENT_QNAME);
    }

    protected static void generateXML(Designate designate, ContentHandler contentHandler)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        addAttribute(ai, "pid", designate.getPid());
        contentHandler.startElement(NAMESPACE_URI, DESIGNATE_ELEMENT, DESIGNATE_ELEMENT_QNAME, ai);

        generateXML(designate.getObject(), contentHandler);

        contentHandler.endElement(NAMESPACE_URI, DESIGNATE_ELEMENT, DESIGNATE_ELEMENT_QNAME);
    }

    protected static void generateXML(MTObject obj, ContentHandler contentHandler)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        addAttribute(ai, "ocdref", obj.getOcdref());
        contentHandler.startElement(NAMESPACE_URI, OBJECT_ELEMENT, OBJECT_ELEMENT_QNAME, ai);
        contentHandler.endElement(NAMESPACE_URI, OBJECT_ELEMENT, OBJECT_ELEMENT_QNAME);
    }

    /**
     * Helper method to add an attribute.
     * This implementation adds a new attribute with the given name
     * and value. Before adding the value is checked for non-null.
     * @param ai    The attributes impl receiving the additional attribute.
     * @param name  The name of the attribute.
     * @param value The value of the attribute.
     */
    protected static void addAttribute(AttributesImpl ai, String name, Object value) {
        if ( value != null ) {
            ai.addAttribute("", name, name, "CDATA", value.toString());
        }
    }

}
