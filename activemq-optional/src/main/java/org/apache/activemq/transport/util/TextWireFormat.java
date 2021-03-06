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
package org.apache.activemq.transport.util;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.Reader;

import org.apache.activeio.command.WireFormat;
import org.apache.activemq.command.Command;

/**
 * Adds the extra methods available to text based wire format implementations
 * 
 * @version $Revision: 1.1 $
 */
public abstract class TextWireFormat implements WireFormat {

    public abstract Command readCommand(String text);
    
    public abstract Command readCommand(Reader reader);

    public abstract String toString(Command command);

    public Command readCommand(DataInputStream in) throws IOException {
        String text = in.readUTF();
        return readCommand(text);
    }
}
