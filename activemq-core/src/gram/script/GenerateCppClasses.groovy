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
import org.apache.activemq.openwire.tool.OpenWireCppClassesScript

/**
 * Generates the C++ commands for the Open Wire Format
 *
 * @version $Revision$
 */
class GenerateCppClasses extends OpenWireCppClassesScript {

	void generateFile(PrintWriter out) {
                out << """/*
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
#include "command/${className}.hpp"

using namespace apache::activemq::client::command;

/*
 *
 *  Marshalling code for Open Wire Format for ${className}
 *
 *
 *  NOTE!: This file is autogenerated - do not modify!
 *         if you need to make a change, please see the Groovy scripts in the
 *         activemq-core module
 *
 */
${className}::${className}()
{"""
    for (property in properties) {
        def value = toCppDefaultValue(property.type)
        def propertyName = property.simpleName
        def parameterName = decapitalize(propertyName)
        out << """
    this->${parameterName} = ${value} ;"""
    }
    out << """
}

${className}::~${className}()
{
}
"""
    for (property in properties) {
        def type = toCppType(property.type)
        def propertyName = property.simpleName
        def parameterName = decapitalize(propertyName)
        out << """
        
${type} ${className}::get${propertyName}()
{
    return ${parameterName} ;
}

void ${className}::set${propertyName}(${type} ${parameterName})
{
    this->${parameterName} = ${parameterName} ;
}
"""

    }
    }
}
