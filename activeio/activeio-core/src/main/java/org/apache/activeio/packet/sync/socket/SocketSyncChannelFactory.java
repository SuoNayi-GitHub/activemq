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
package org.apache.activeio.packet.sync.socket;

import java.io.IOException;
import java.net.URI;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

import org.apache.activeio.packet.sync.SyncChannel;
import org.apache.activeio.packet.sync.SyncChannelFactory;
import org.apache.activeio.packet.sync.SyncChannelServer;
import org.apache.activeio.stream.sync.StreamChannelServer;
import org.apache.activeio.stream.sync.socket.SocketStreamChannel;
import org.apache.activeio.stream.sync.socket.SocketStreamChannelFactory;
import org.apache.activeio.stream.sync.socket.SocketStreamChannelServer;

/**
 * A TcpSynchChannelFactory creates {@see org.apache.activeio.net.TcpSynchChannel}
 * and {@see org.apache.activeio.net.TcpSynchChannelServer} objects.
 * 
 * @version $Revision$
 */
public class SocketSyncChannelFactory implements SyncChannelFactory {

    SocketStreamChannelFactory factory;
    
    public SocketSyncChannelFactory() {
        this(SocketFactory.getDefault(), ServerSocketFactory.getDefault());
    }

    public SocketSyncChannelFactory(SocketFactory socketFactory, ServerSocketFactory serverSocketFactory) {
        factory = new SocketStreamChannelFactory(socketFactory, serverSocketFactory);
    }
        
    /**
     * Uses the {@param location}'s host and port to create a tcp connection to a remote host.
     * 
     * @see org.apache.activeio.SyncChannelFactory#openSyncChannel(java.net.URI)
     */
    public SyncChannel openSyncChannel(URI location) throws IOException {
        SocketStreamChannel channel = (SocketStreamChannel) factory.openStreamChannel(location);
        return createSynchChannel(channel);
    }

    /**
     * @param channel
     * @return
     * @throws IOException
     */
    protected SyncChannel createSynchChannel(SocketStreamChannel channel) throws IOException {
        return new SocketSyncChannel(channel);
    }

    /**
     * Binds a server socket a the {@param bindURI}'s port.
     * 
     * @see org.apache.activeio.SyncChannelFactory#bindSyncChannel(java.net.URI)
     */
    public SyncChannelServer bindSyncChannel(URI bindURI) throws IOException {
        StreamChannelServer server = factory.bindStreamChannel(bindURI);
        return new SocketSyncChannelServer((SocketStreamChannelServer) server);
    }
    
    /**
     * @return Returns the backlog.
     */
    public int getBacklog() {
        return factory.getBacklog();
    }

    /**
     * @param backlog
     *            The backlog to set.
     */
    public void setBacklog(int backlog) {
        factory.setBacklog(backlog);
    }


}
