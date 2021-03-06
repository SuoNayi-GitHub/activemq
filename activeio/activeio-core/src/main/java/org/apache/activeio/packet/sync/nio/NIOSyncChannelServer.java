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

package org.apache.activeio.packet.sync.nio;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.ServerSocketChannel;

import org.apache.activeio.Channel;
import org.apache.activeio.packet.ByteBufferPacket;
import org.apache.activeio.packet.sync.SyncChannel;
import org.apache.activeio.packet.sync.filter.WriteBufferedSyncChannel;
import org.apache.activeio.packet.sync.socket.SocketSyncChannelServer;
import org.apache.activeio.stream.sync.socket.SocketStreamChannel;

/**
 * A SynchChannelServer that creates
 * {@see org.apache.activeio.net.TcpSynchChannel}objects from accepted
 * tcp socket connections.
 * 
 * @version $Revision$
 */
public class NIOSyncChannelServer extends SocketSyncChannelServer {

    private final boolean createWriteBufferedChannels;
	private final boolean useDirectBuffers;

    public NIOSyncChannelServer(ServerSocketChannel socketChannel, URI bindURI, URI connectURI, boolean createWriteBufferedChannels, boolean useDirectBuffers) {
        super(socketChannel.socket(), bindURI, connectURI);
        this.createWriteBufferedChannels = createWriteBufferedChannels;
		this.useDirectBuffers = useDirectBuffers;
    }
    
    protected Channel createChannel(SocketStreamChannel c) throws IOException {
        SyncChannel channel = new NIOSyncChannel(c.getSocket().getChannel());
        if( createWriteBufferedChannels ) {
            channel = new WriteBufferedSyncChannel(channel, ByteBufferPacket.createDefaultBuffer(useDirectBuffers));
        }
        return channel;
    }
}