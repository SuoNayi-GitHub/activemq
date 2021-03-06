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
package org.apache.activemq.transport.http;

import org.apache.activemq.transport.TransportThreadSupport;
import org.apache.activemq.transport.util.TextWireFormat;

import java.net.URI;

/**
 * A useful base class for HTTP Transport implementations.
 * 
 * @version $Revision: 1.1 $
 */
public abstract class HttpTransportSupport extends TransportThreadSupport {
    private TextWireFormat textWireFormat;
    private URI remoteUrl;

    public HttpTransportSupport(TextWireFormat textWireFormat, URI remoteUrl) {
        this.textWireFormat = textWireFormat;
        this.remoteUrl = remoteUrl;
    }

    public String toString() {
        return "HTTP Reader " + getRemoteUrl();
    }

    // Properties
    // -------------------------------------------------------------------------
    public URI getRemoteUrl() {
        return remoteUrl;
    }

    public TextWireFormat getTextWireFormat() {
        return textWireFormat;
    }

    public void setTextWireFormat(TextWireFormat textWireFormat) {
        this.textWireFormat = textWireFormat;
    }
}
