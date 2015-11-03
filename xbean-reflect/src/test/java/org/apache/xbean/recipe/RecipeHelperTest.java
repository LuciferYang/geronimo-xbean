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
package org.apache.xbean.recipe;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RecipeHelperTest {
    @Test
    public void stringCharArrayIsConvertable() {
        assertTrue(RecipeHelper.isConvertable(char[].class, "foo"));
        assertTrue(RecipeHelper.isConvertable(String.class, "foo".toCharArray()));
    }

    @Test
    public void stringCharArrayConvert() {
        assertArrayEquals("foo".toCharArray(), char[].class.cast(RecipeHelper.convert(char[].class, "foo", false)));
        assertEquals("foo", RecipeHelper.convert(String.class, "foo".toCharArray(), false));
    }
}
