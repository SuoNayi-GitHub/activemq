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
package org.apache.activeio.packet.async.aio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.activeio.adapter.SyncToAsyncChannelServer;
import org.apache.activeio.packet.ByteBufferPacket;
import org.apache.activeio.packet.async.AsyncChannel;
import org.apache.activeio.packet.async.AsyncChannelFactory;
import org.apache.activeio.packet.async.AsyncChannelServer;
import org.apache.activeio.packet.async.filter.WriteBufferedAsyncChannel;
import org.apache.activeio.util.URISupport;

import com.ibm.io.async.AsyncServerSocketChannel;
import com.ibm.io.async.AsyncSocketChannel;

/**
 * A TcpAsyncChannelFactory creates {@see org.apache.activeio.net.TcpAsyncChannel}
 * and {@see org.apache.activeio.net.TcpAsyncChannelServer} objects.
 * 
 * @version $Revision$
 */
public class AIOAsyncChannelFactory implements AsyncChannelFactory {

    protected static final int DEFAULT_BACKLOG = 500;
    private int backlog = DEFAULT_BACKLOG;
        
    /**
     * Uses the {@param location}'s host and port to create a tcp connection to a remote host.
     * 
     * @see org.apache.activeio.AsyncChannelFactory#openAsyncChannel(java.net.URI)
     */
    public AsyncChannel openAsyncChannel(URI location) throws IOException {
        AsyncSocketChannel channel = AsyncSocketChannel.open();
        channel.connect(new InetSocketAddress(location.getHost(), location.getPort()));
        return createAsyncChannel(channel);
    }

    /**
     * @param channel
     * @return
     * @throws IOException
     */
    protected AsyncChannel createAsyncChannel(AsyncSocketChannel socketChannel) throws IOException {
        AsyncChannel channel = new AIOAsyncChannel(socketChannel);
        channel = new WriteBufferedAsyncChannel(channel, ByteBufferPacket.createDefaultBuffer(true), false);
        return channel;
    }

    /**
     * Binds a server socket a the {@param location}'s port. 
     * 
     * @see org.apache.activeio.AsyncChannelFactory#bindAsyncChannel(java.net.URI)
     */
    public AsyncChannelServer bindAsyncChannel(URI bindURI) throws IOException {
        
        String host = bindURI.getHost();
        InetSocketAddress address;
        if( host == null || host.length() == 0 || host.equals("localhost") || host.equals("0.0.0.0") || InetAddress.getLocalHost().getHostName().equals(host) ) {            
            address = new InetSocketAddress(bindURI.getPort());
        } else {
            address = new InetSocketAddress(bindURI.getHost(), bindURI.getPort());
        }
        
        AsyncServerSocketChannel serverSocketChannel = AsyncServerSocketChannel.open();
        serverSocketChannel.socket().bind(address,backlog);
        
        URI connectURI = bindURI;
        try {
//            connectURI = URISupport.changeHost(connectURI, InetAddress.getLocalHost().getHostName());
            connectURI = URISupport.changePort(connectURI, serverSocketChannel.socket().getLocalPort());
        } catch (URISyntaxException e) {
            throw (IOException)new IOException("Could not build connect URI: "+e).initCause(e);
        }
        
        return SyncToAsyncChannelServer.adapt( 
                new AIOSyncChannelServer(serverSocketChannel, bindURI, connectURI));
    }
    
    /**
     * @return Returns the backlog.
     */
    public int getBacklog() {
        return backlog;
    }

    /**
     * @param backlog
     *            The backlog to set.
     */
    public void setBacklog(int backlog) {
        this.backlog = backlog;
    }


}
