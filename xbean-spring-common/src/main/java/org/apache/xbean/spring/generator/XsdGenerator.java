/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.xbean.spring.generator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

/**
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class XsdGenerator implements GeneratorPlugin {
    private final File destFile;
    private LogFacade log;

    public XsdGenerator(File destFile) {
        this.destFile = destFile;
    }

    public void generate(NamespaceMapping namespaceMapping) throws IOException {
        // TODO can only handle 1 schema document so far...
        File file = destFile;
        log.log("Generating XSD file: " + file + " for namespace: " + namespaceMapping.getNamespace());
        PrintWriter out = new PrintWriter(new FileWriter(file));
        try {
            generateSchema(out, namespaceMapping);
        } finally {
            out.close();
        }
    }

    private void generateSchema(PrintWriter out, NamespaceMapping namespaceMapping) {
        out.println("<?xml version='1.0'?>");
        out.println("<!-- NOTE: this file is autogenerated by Apache XBean -->");
        out.println();
        out.println("<xs:schema elementFormDefault='qualified'");
        out.println("           targetNamespace='" + namespaceMapping.getNamespace() + "'");
        out.println("           xmlns:xs='http://www.w3.org/2001/XMLSchema'");
        out.println("           xmlns:tns='" + namespaceMapping.getNamespace() + "'>");

        for (Iterator iter = namespaceMapping.getElements().iterator(); iter.hasNext();) {
            ElementMapping element = (ElementMapping) iter.next();
            generateElementMapping(out, namespaceMapping, element);
        }

        out.println();
        out.println("</xs:schema>");
    }

    private void generateElementMapping(PrintWriter out, NamespaceMapping namespaceMapping, ElementMapping element) {
        out.println();
        out.println("  <!-- element for type: " + element.getClassName() + " -->");

        String localName = element.getElementName();

        out.println("  <xs:element name='" + localName + "'>");

        if (!isEmptyString(element.getDescription())) {
            out.println("    <xs:annotation>");
            out.println("      <xs:documentation><![CDATA[");
            out.println("        " + element.getDescription());
            out.println("      ]]></xs:documentation>");
            out.println("    </xs:annotation>");
        }

        out.println("    <xs:complexType>");

        int complexCount = 0;
        for (Iterator iterator = element.getAttributes().iterator(); iterator.hasNext();) {
            AttributeMapping attributeMapping = (AttributeMapping) iterator.next();
            if (!namespaceMapping.isSimpleType(attributeMapping.getType())) {
                complexCount++;
            }
        }
        if (complexCount > 0) {
            out.println("      <xs:sequence>");
            for (Iterator iterator = element.getAttributes().iterator(); iterator.hasNext();) {
                AttributeMapping attributeMapping = (AttributeMapping) iterator.next();
                if (!namespaceMapping.isSimpleType(attributeMapping.getType())) {
                    generateElementMappingComplexProperty(out, namespaceMapping, attributeMapping);
                }
            }
            out.println("        <xs:any namespace='##other'/>");
            out.println("      </xs:sequence>");
        }

        for (Iterator iterator = element.getAttributes().iterator(); iterator.hasNext();) {
            AttributeMapping attributeMapping = (AttributeMapping) iterator.next();
            if (namespaceMapping.isSimpleType(attributeMapping.getType())) {
                generateElementMappingSimpleProperty(out, attributeMapping);
            } else if (!attributeMapping.getType().isCollection()) {
                generateElementMappingComplexPropertyAsRef(out, attributeMapping);
            }
        }
        generateIDAttributeMapping(out, namespaceMapping, element);

        out.println("      <xs:anyAttribute namespace='##other' processContents='lax'/>");
        out.println("    </xs:complexType>");
        out.println("  </xs:element>");
        out.println();
    }

    private boolean isEmptyString(String str) {
        if (str == null) {
            return true;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private void generateIDAttributeMapping(PrintWriter out, NamespaceMapping namespaceMapping, ElementMapping element) {
        for (Iterator iterator = element.getAttributes().iterator(); iterator.hasNext();) {
            AttributeMapping attributeMapping = (AttributeMapping) iterator.next();
            if ("id".equals(attributeMapping.getAttributeName())) {
                return;
            }
        }
        out.println("      <xs:attribute name='id' type='xs:ID'/>");
    }

    private void generateElementMappingSimpleProperty(PrintWriter out, AttributeMapping attributeMapping) {
        if (!isEmptyString(attributeMapping.getDescription())) {
            out.println("      <xs:attribute name='" + attributeMapping.getAttributeName() + "' type='" + Utils.getXsdType(attributeMapping.getType()) + "'>");
            out.println("        <xs:annotation>");
            out.println("          <xs:documentation><![CDATA[");
            out.println("            " + attributeMapping.getDescription());
            out.println("          ]]></xs:documentation>");
            out.println("        </xs:annotation>");
            out.println("      </xs:attribute>");
        } else {
            out.println("      <xs:attribute name='" + attributeMapping.getAttributeName() + "' type='" + Utils.getXsdType(attributeMapping.getType()) + "'/>");
        }
    }

    private void generateElementMappingComplexPropertyAsRef(PrintWriter out, AttributeMapping attributeMapping) {
        if (!isEmptyString(attributeMapping.getDescription())) {
            out.println("      <xs:attribute name='" + attributeMapping.getAttributeName() + "' type='xs:string'>");
            out.println("        <xs:annotation>");
            out.println("          <xs:documentation><![CDATA[");
            out.println("            " + attributeMapping.getDescription());
            out.println("          ]]></xs:documentation>");
            out.println("        </xs:annotation>");
            out.println("      </xs:attribute>");
        } else {
            out.println("      <xs:attribute name='" + attributeMapping.getAttributeName() + "' type='xs:string'/>");
        }
    }

    private void generateElementMappingComplexProperty(PrintWriter out, NamespaceMapping namespaceMapping, AttributeMapping attributeMapping) {
        Type type = attributeMapping.getType();
        List types;
        if (type.isCollection()) {
            types = Utils.findImplementationsOf(namespaceMapping, type.getNestedType());
        } else {
            types = Utils.findImplementationsOf(namespaceMapping, type);
        }
        String maxOccurs = type.isCollection() ? "unbounded" : "1";

        out.println("        <xs:element name='" + attributeMapping.getAttributeName() + "' minOccurs='0' maxOccurs='1'>");
        if (!isEmptyString(attributeMapping.getDescription())) {
            out.println("          <xs:annotation>");
            out.println("            <xs:documentation><![CDATA[");
            out.println("              " + attributeMapping.getDescription());
            out.println("            ]]></xs:documentation>");
            out.println("          </xs:annotation>");
        }
        out.println("          <xs:complexType>");
        if (types.isEmpty()) {
            out.println("            <xs:sequence minOccurs='0' maxOccurs='" + maxOccurs + "'><xs:any namespace='##other'/></xs:sequence>");
        } else {
            out.println("            <xs:choice minOccurs='0' maxOccurs='" + maxOccurs + "'>");
            for (Iterator iterator = types.iterator(); iterator.hasNext();) {
                ElementMapping element = (ElementMapping) iterator.next();
                out.println("              <xs:element ref='tns:" + element.getElementName() + "'/>");
            }
            out.println("              <xs:any namespace='##other'/>");
            out.println("            </xs:choice>");
        }
        out.println("          </xs:complexType>");
        out.println("        </xs:element>");
    }

    public LogFacade getLog() {
        return log;
    }

    public void setLog(LogFacade log) {
        this.log = log;
    }
}
