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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.apache.activeio.packet.ByteArrayPacket;
import org.apache.activeio.packet.ByteSequence;
import org.apache.activeio.packet.EOSPacket;
import org.apache.activeio.packet.EmptyPacket;
import org.apache.activeio.packet.Packet;
import org.apache.activeio.packet.sync.SyncChannel;
import org.apache.activeio.packet.sync.SyncChannelServer;
import org.apache.activeio.stream.sync.socket.SocketStreamChannel;

/**
 * A {@see org.apache.activeio.SynchChannel} implementation that uses a {@see java.net.Socket}
 *  to talk to the network.
 * 
 * @version $Revision$
 */
public class SocketSyncChannel implements SyncChannel {

    protected static final int DEFAULT_BUFFER_SIZE = 64 * 1024;
    private final SocketStreamChannel channel;
    private Packet inputPacket;
    private final OutputStream os;
    private final InputStream is;
    
    protected SocketSyncChannel(Socket socket) throws IOException {
        this(new SocketStreamChannel(socket));
    }

    public SocketSyncChannel(SocketStreamChannel channel) throws IOException {
        this.channel = channel;
        os = channel.getOutputStream();
        is = channel.getInputStream();
        channel.setReceiveBufferSize(DEFAULT_BUFFER_SIZE);
        channel.setSendBufferSize(DEFAULT_BUFFER_SIZE);
    }

    /**
     * @see org.apache.activeio.SynchChannel#read(long)
     */
    synchronized public org.apache.activeio.packet.Packet read(long timeout) throws IOException {
        try {
            
            if( timeout==SyncChannelServer.WAIT_FOREVER_TIMEOUT )
                channel.setSoTimeout( 0 );
            else if( timeout==SyncChannelServer.NO_WAIT_TIMEOUT )
                channel.setSoTimeout( 1 );
            else 
                channel.setSoTimeout( (int)timeout );

            if( inputPacket==null || !inputPacket.hasRemaining() ) {
                inputPacket = allocatePacket();
            }
            
            ByteSequence sequence = inputPacket.asByteSequence();
            int size = is.read(sequence.getData(), sequence.getOffset(), sequence.getLength());
            if( size == -1 )
                return EOSPacket.EOS_PACKET;
            if( size == 0 )
                return EmptyPacket.EMPTY_PACKET;
            inputPacket.position(size);
            
            Packet remaining = inputPacket.slice();
            
            inputPacket.flip();
            Packet data = inputPacket.slice();

            // Keep the remaining buffer around to fill with data.
            inputPacket = remaining;            
            return data;
            
        } catch (SocketTimeoutException e) {
            return null;
        }
    }

    private Packet allocatePacket() {
        byte[] data = new byte[DEFAULT_BUFFER_SIZE];
        return new ByteArrayPacket(data);
    }

    
    /**
     * @see org.apache.activeio.Channel#write(org.apache.activeio.packet.Packet)
     */
    public void write(Packet packet) throws IOException {
        packet.writeTo(os);
    }

    /**
     * @see org.apache.activeio.Channel#flush()
     */
    public void flush() throws IOException {
        os.flush();
    }

    /**
     * @see org.apache.activeio.Disposable#dispose()
     */
    public void dispose() {
        channel.dispose();
    }

    public void start() throws IOException {
        channel.start();
    }
    public void stop() throws IOException {
        channel.stop();
    }
        
    public Object getAdapter(Class target) {
        if( target.isAssignableFrom(getClass()) ) {
            return this;
        }
        return channel.getAdapter(target);
    }

    public String toString() {
        return channel.toString();
    }
}