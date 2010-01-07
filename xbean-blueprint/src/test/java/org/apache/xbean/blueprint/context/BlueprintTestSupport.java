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
package org.apache.xbean.blueprint.context;

import junit.framework.TestCase;
import org.apache.aries.blueprint.NamespaceHandler;
import org.apache.aries.blueprint.reflect.BeanMetadataImpl;
import org.apache.aries.blueprint.container.NamespaceHandlerRegistry;
import org.apache.aries.blueprint.container.Parser;
import org.apache.aries.blueprint.namespace.ComponentDefinitionRegistryImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xbean.blueprint.context.impl.XBeanNamespaceHandler;
import org.xml.sax.SAXException;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.ValueMetadata;
import org.osgi.service.blueprint.reflect.BeanArgument;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.BeanMetadata;

import javax.xml.validation.Schema;
import javax.xml.namespace.QName;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.List;
import java.util.Map;

/**
 * A useful base class for testing spring based utilities.
 *
 * @author James Strachan
 * @version $Id$
 * @since 2.0
 */
public abstract class BlueprintTestSupport extends TestCase {
    protected Log log = LogFactory.getLog(getClass());
    private static final URI NAMESPACE_URI = URI.create("http://xbean.apache.org/schemas/pizza");

    protected ComponentDefinitionRegistryImpl reg;

    protected void setUp() throws Exception {
        reg = parse(getPlan());
    }

    protected static ComponentDefinitionRegistryImpl parse(String plan) throws Exception {
        String schema = "META-INF/services/org/apache/xbean/blueprint/http/xbean.apache.org/schemas/pizza";
        return parse(plan, schema);
    }

    protected static ComponentDefinitionRegistryImpl parse(String plan, String schema) throws Exception {
        Properties properties = new Properties();
        URL propUrl = BlueprintTestSupport.class.getClassLoader().getResource(schema);
        InputStream in = propUrl.openStream();
        try {
            properties.load(in);
        } finally {
            in.close();
        }

        Set<Class> classes = new HashSet<Class>();
        ClassLoader cl = BlueprintTestSupport.class.getClassLoader();
        for (Map.Entry entry : properties.entrySet()) {
            String key = (String) entry.getKey();
            if (!key.contains(".")) {
                String className = (String) entry.getValue();
                Class clazz = cl.loadClass(className);
                classes.add(clazz);
            }
        }
        classes.add(QName.class);

        final NamespaceHandler xbeanHandler = new XBeanNamespaceHandler(NAMESPACE_URI.toString(), BlueprintTestSupport.class.getClassLoader().getResource("restaurant.xsd"), classes, properties);
        NamespaceHandlerRegistry.NamespaceHandlerSet handlers = new NamespaceHandlerRegistry.NamespaceHandlerSet() {
            public Set<URI> getNamespaces() {
                return Collections.singleton(NAMESPACE_URI);
            }

            public NamespaceHandler getNamespaceHandler(URI namespace) {
                return xbeanHandler;
            }

            public void removeListener(NamespaceHandlerRegistry.Listener listener) {
            }

            public Schema getSchema() throws SAXException, IOException {
                return null;
            }

            public boolean isComplete() {
                return false;
            }

            public void addListener(NamespaceHandlerRegistry.Listener listener) {
            }

            public void destroy() {
            }
        };
        return parse(plan, handlers);
    }

    // from aries blueprint core AbstractBlueprintTest
    protected static ComponentDefinitionRegistryImpl parse(String plan, NamespaceHandlerRegistry.NamespaceHandlerSet handlers) throws Exception {
        ComponentDefinitionRegistryImpl registry = new ComponentDefinitionRegistryImpl();
        Parser parser = new Parser();
        parser.parse(Collections.singletonList(BlueprintTestSupport.class.getClassLoader().getResource(plan)));
        parser.populate(handlers, registry);
        return registry;
    }

    protected abstract String getPlan();

    protected static void checkPropertyValue(String name, Object expectedValued, BeanMetadataImpl meta) {
        BeanProperty prop = propertyByName(name, meta);
        assertEquals(expectedValued, ((ValueMetadata) prop.getValue()).getStringValue());
    }

    protected static BeanProperty propertyByName(String name, BeanMetadataImpl meta) {
        List<BeanProperty> props = meta.getProperties();
        for (BeanProperty prop : props) {
            if (name.equals(prop.getName())) {
                return prop;
            }
        }
        throw new RuntimeException("No such property: " + name + " in metadata: " + meta);

    }

    protected static void checkArgumentValue(int index, String expectedValued, BeanMetadataImpl meta, boolean allowNesting) {
        List<BeanArgument> props = meta.getArguments();
        Metadata metadata = props.get(index).getValue();
        if (allowNesting && metadata instanceof BeanMetadata) {
            metadata = ((BeanMetadata) metadata).getArguments().get(0).getValue();
        }
        assertEquals(expectedValued, ((ValueMetadata) metadata).getStringValue());
    }

}