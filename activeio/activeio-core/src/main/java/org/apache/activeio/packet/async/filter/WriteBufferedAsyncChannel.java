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
package org.apache.activeio.packet.async.filter;

import java.io.IOException;

import org.apache.activeio.packet.ByteArrayPacket;
import org.apache.activeio.packet.Packet;
import org.apache.activeio.packet.async.AsyncChannel;
import org.apache.activeio.packet.async.FilterAsyncChannel;

/**
 */
public class WriteBufferedAsyncChannel extends FilterAsyncChannel {

    private static final int DEFAULT_BUFFER_SIZE = 1024*64;
    private final Packet buffer;
    private final boolean enableDirectWrites;
    
    public WriteBufferedAsyncChannel(AsyncChannel channel) {
        this(channel, new ByteArrayPacket(new byte[DEFAULT_BUFFER_SIZE]));
    }
    
    public WriteBufferedAsyncChannel(AsyncChannel channel, Packet buffer) {
        this(channel, buffer, true);
    }

    public WriteBufferedAsyncChannel(AsyncChannel channel, Packet buffer, boolean enableDirectWrites) {
        super(channel);
        this.buffer = buffer;
        this.enableDirectWrites = enableDirectWrites;
    }

    public void write(Packet packet) throws IOException {
        
        while( packet.hasRemaining() ) {
	        packet.read(buffer);
	        if( !buffer.hasRemaining() ) {
	            flush();
	            
	            // Should we just direct write the rest?
	            if( enableDirectWrites && packet.remaining() > buffer.capacity()) {
	                getNext().write(packet);
	                return;
	            }
	        }
        }
        
    }
    
    public void flush() throws IOException {
        buffer.flip();
        getNext().write(buffer);
        buffer.clear();
    }    
}
