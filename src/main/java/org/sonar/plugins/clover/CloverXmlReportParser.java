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

import org.apache.commons.lang.StringUtils;
import org.codehaus.staxmate.in.SMEvent;
import org.codehaus.staxmate.in.SMFilterFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.codehaus.staxmate.in.SimpleFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.measures.CoverageMeasuresBuilder;
import org.sonar.api.measures.Measure;
import org.sonar.api.utils.ParsingUtils;
import org.sonar.api.utils.StaxParser;
import org.sonar.api.utils.XmlParserException;

import javax.annotation.Nullable;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.text.ParseException;

public class CloverXmlReportParser {

  private static final Logger LOG = LoggerFactory.getLogger(CloverXmlReportParser.class);
  private SensorContext context;
  private final InputFileProvider inputFileProvider;
  private int files;
  private int unmatchedFile;
  private String unmatchedFiles;
  final CoverageMeasuresBuilder fileMeasuresBuilder = CoverageMeasuresBuilder.create();

  CloverXmlReportParser(SensorContext context, InputFileProvider inputFileProvider) {
    this.context = context;
    this.inputFileProvider = inputFileProvider;
  }

  private static boolean reportExists(@Nullable File report) {
    return report != null && report.exists() && report.isFile();
  }

  protected void collect(File xmlFile) {
    try {
      if (reportExists(xmlFile)) {
        files = 0;
        unmatchedFile = 0;
        unmatchedFiles = "";
        LOG.info("Parsing " + xmlFile.getCanonicalPath());
        createStaxParser().parse(xmlFile);
        LOG.info("Matched files in report : {}", getMatchedPercentage());
        if (!unmatchedFiles.isEmpty()) {
          LOG.warn("{} files in clover report did not match any file in SonarQube Index : {}", unmatchedFile, unmatchedFiles);
        }
      }
    } catch (IllegalStateException e) {
      LOG.error("Format of clover report file is unexpected ", e);
      throw new XmlParserException(e);
    } catch (Exception e) {
      LOG.error("An error occured while parsing clover xml report : ", e);
      throw new XmlParserException(e);
    }
  }

  private StaxParser createStaxParser() {
    return new StaxParser(new StaxParser.XmlStreamHandler() {
      @Override
      public void stream(SMHierarchicCursor rootCursor) throws XMLStreamException {
        try {
          collectProjectMeasures(rootCursor.advance());
        } catch (ParseException e) {
          throw new XMLStreamException(e);
        }
      }
    });
  }

  private String getMatchedPercentage() {
    if (files == 0) {
      return "No files found in <project> section of report";
    }
    return (files - unmatchedFile) * 100 / files+"%";
  }

  private void collectProjectMeasures(SMInputCursor rootCursor) throws ParseException, XMLStreamException {
    SMInputCursor projectCursor = rootCursor.descendantElementCursor("project");
    SMInputCursor projectChildrenCursor = projectCursor.advance().childElementCursor();
    projectChildrenCursor.setFilter(new SimpleFilter(SMEvent.START_ELEMENT));
    //Skip the metrics tag.
    projectChildrenCursor.advance();
    collectPackageMeasures(projectChildrenCursor);
  }

  private void collectPackageMeasures(SMInputCursor packCursor) throws ParseException, XMLStreamException {
    while (packCursor.getNext() != null) {
      SMInputCursor packChildrenCursor = packCursor.descendantElementCursor();
      packChildrenCursor.setFilter(new SimpleFilter(SMEvent.START_ELEMENT));
      //Skip the metrics tag.
      packChildrenCursor.advance();
      collectFileMeasures(packChildrenCursor);
    }
  }

  private void collectFileMeasures(SMInputCursor fileCursor) throws ParseException, XMLStreamException {
    fileCursor.setFilter(SMFilterFactory.getElementOnlyFilter("file"));
    while (fileCursor.getNext() != null) {
      if (fileCursor.asEvent().isStartElement()) {
        String path = fileCursor.getAttrValue("path");
        if (path != null) {
          SMInputCursor fileChildrenCursor = fileCursor.childCursor(new SimpleFilter(SMEvent.START_ELEMENT));
          // cursor should be on the metrics element
          if (canBeIncludedInFileMetrics(fileChildrenCursor)) {
            // cursor should be now on the line cursor
            saveHitsData(getInputFile(path), fileChildrenCursor);
          }
        }
      }
    }
  }

  private InputFile getInputFile(String path) {
    files++;
    InputFile resource = inputFileProvider.fromPath(path);
    if (resource == null) {
      unmatchedFile++;
      LOG.warn("Resource " + path + " was not found.");
      unmatchedFiles += path + ", ";
    }
    return resource;
  }

  private void saveHitsData(InputFile resource, SMInputCursor lineCursor) throws ParseException, XMLStreamException {
    fileMeasuresBuilder.reset();

    while (lineCursor.getNext() != null) {
      // skip class elements on format 2_3_2
      if (isClass(lineCursor)) {
        continue;
      }
      final int lineId = Integer.parseInt(lineCursor.getAttrValue("num"));
      String count = lineCursor.getAttrValue("count");
      if (StringUtils.isNotBlank(count)) {
        fileMeasuresBuilder.setHits(lineId, Integer.parseInt(count));

      } else {
        int trueCount = (int) ParsingUtils.parseNumber(lineCursor.getAttrValue("truecount"));
        int falseCount = (int) ParsingUtils.parseNumber(lineCursor.getAttrValue("falsecount"));
        int coveredConditions = 0;
        if (trueCount > 0) {
          coveredConditions++;
        }
        if (falseCount > 0) {
          coveredConditions++;
        }
        fileMeasuresBuilder.setConditions(lineId, 2, coveredConditions);
      }
    }
    if (resource != null) {
      for (Measure measure : fileMeasuresBuilder.createMeasures()) {
        context.saveMeasure(resource, measure);
      }
    }
  }

  private static boolean canBeIncludedInFileMetrics(SMInputCursor metricsCursor) throws ParseException, XMLStreamException {
    while (metricsCursor.getNext() != null && isClass(metricsCursor)) {
      // skip class elements on 1.x xml format
    }
    return ParsingUtils.parseNumber(metricsCursor.getAttrValue("elements")) > 0;
  }

  private static boolean isClass(SMInputCursor cursor) throws XMLStreamException {
    return "class".equals(cursor.getLocalName());
  }
}
