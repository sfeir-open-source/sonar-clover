/*
 * Sonar Clover Plugin
 * Copyright (C) 2008 SonarSource
 * sonarqube@googlegroups.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.clover;

import com.ctc.wstx.stax.WstxInputFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

class StaxParser {

    private SMInputFactory inf;
    private XmlStreamHandler streamHandler;

    StaxParser(XmlStreamHandler streamHandler) {
        this.streamHandler = streamHandler;
        XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
        if (xmlFactory instanceof WstxInputFactory) {
            WstxInputFactory wstxInputfactory = (WstxInputFactory) xmlFactory;
            wstxInputfactory.configureForLowMemUsage();
            wstxInputfactory.getConfig().setUndeclaredEntityResolver(new UndeclaredEntitiesXMLResolver());
        }
        xmlFactory.setProperty(XMLInputFactory.IS_VALIDATING, false);
        xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        xmlFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
        inf = new SMInputFactory(xmlFactory);
    }

    void parse(File xmlFile) throws XMLStreamException {
        try (FileInputStream input = new FileInputStream(xmlFile)) {
            parse(input);
            IOUtils.closeQuietly(input);
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    private void parse(InputStream xmlInput) throws XMLStreamException {
        parse(inf.rootElementCursor(xmlInput));
    }

    private void parse(SMHierarchicCursor rootCursor) throws XMLStreamException {
        try {
            streamHandler.stream(rootCursor);
        } finally {
            rootCursor.getStreamReader().closeCompletely();
        }
    }

    private static class UndeclaredEntitiesXMLResolver implements XMLResolver {
        @Override
        public Object resolveEntity(String arg0, String arg1, String fileName, String undeclaredEntity) {
            // avoid problems with XML docs containing undeclared entities.. return the entity under its raw form if not an unicode expression
            if (StringUtils.startsWithIgnoreCase(undeclaredEntity, "u") && undeclaredEntity.length() == 5) {
                int unicodeCharHexValue = Integer.parseInt(undeclaredEntity.substring(1), 16);
                if (Character.isDefined(unicodeCharHexValue)) {
                    undeclaredEntity = new String(new char[] { (char) unicodeCharHexValue });
                }
            }
            return undeclaredEntity;
        }
    }

    /**
     * Simple interface for handling XML stream to parse
     */
    public interface XmlStreamHandler {
        void stream(SMHierarchicCursor rootCursor) throws XMLStreamException;
    }
}
