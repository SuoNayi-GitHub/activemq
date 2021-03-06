/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.management;

import org.apache.activemq.management.BoundedRangeStatisticImpl;


/**
 * @version $Revision: 1.2 $
 */
public class BoundedRangeStatisticTest extends RangeStatisticTest {

    /**
     * Use case for BoundedRangeStatisticImpl class.
     * @throws Exception
     */
    public void testStatistic() throws Exception {
        BoundedRangeStatisticImpl stat = new BoundedRangeStatisticImpl("myRange", "millis", "myDescription", 10, 3000);
        assertStatistic(stat, "myRange", "millis", "myDescription");
        assertEquals(10, stat.getLowerBound());
        assertEquals(3000, stat.getUpperBound());

        assertRangeStatistic(stat);
    }
}
