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
package org.apache.activeio.adapter;

import java.io.IOException;
import java.io.InterruptedIOException;

import org.apache.activeio.packet.Packet;
import org.apache.activeio.packet.async.AsyncChannel;
import org.apache.activeio.packet.async.AsyncChannelListener;
import org.apache.activeio.packet.sync.SyncChannel;

import edu.emory.mathcs.backport.java.util.concurrent.BlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

/**
 * Adapts a {@see org.apache.activeio.AsyncChannel} so that it provides an 
 * {@see org.apache.activeio.SynchChannel} interface.  
 * 
 * This object buffers asynchronous messages from the {@see org.apache.activeio.AsyncChannel} 
 * and buffers them in a {@see edu.emory.mathcs.backport.java.util.concurrent.Channel} util the client receives them.
 * 
 * @version $Revision$
 */
final public class AsyncToSyncChannel implements SyncChannel, AsyncChannelListener {

    private final AsyncChannel asyncChannel;    
    private final BlockingQueue buffer;

    static public SyncChannel adapt(org.apache.activeio.Channel channel) {
        return adapt(channel, new LinkedBlockingQueue());
    }

    static public SyncChannel adapt(org.apache.activeio.Channel channel, BlockingQueue upPacketChannel) {

        // It might not need adapting
        if( channel instanceof SyncChannel ) {
            return (SyncChannel) channel;
        }

        // Can we just just undo the adaptor
        if( channel.getClass() == SyncToAsyncChannel.class ) {
            return ((SyncToAsyncChannel)channel).getSynchChannel();
        }
        
        return new AsyncToSyncChannel((AsyncChannel)channel, upPacketChannel);        
    }
    
    /**
     * @deprecated {@see #adapt(AsyncChannel)}
     */
    public AsyncToSyncChannel(AsyncChannel asyncChannel) {
        this(asyncChannel, new LinkedBlockingQueue());
    }
    
    /**
     * @deprecated {@see #adapt(AsyncChannel, Channel)}
     */
    public AsyncToSyncChannel(AsyncChannel asyncChannel, BlockingQueue upPacketChannel){
        this.asyncChannel = asyncChannel;
        this.asyncChannel.setAsyncChannelListener(this);
        this.buffer=upPacketChannel;
    }

    /**
     * @see org.apache.activeio.Channel#write(org.apache.activeio.packet.Packet)
     */
    public void write(org.apache.activeio.packet.Packet packet) throws IOException {
        asyncChannel.write(packet);
    }

    /**
     * @see org.apache.activeio.Channel#flush()
     */
    public void flush() throws IOException {
        asyncChannel.flush();
    }

    /**
     * @see org.apache.activeio.packet.sync.SyncChannel#read(long)
     */
    public Packet read(long timeout) throws IOException {
        try {
            
            Object o;
            if( timeout == NO_WAIT_TIMEOUT ) {
                o = buffer.poll(0, TimeUnit.MILLISECONDS);
            } else if( timeout == WAIT_FOREVER_TIMEOUT ) {
                o = buffer.take();            
            } else {
                o = buffer.poll(timeout, TimeUnit.MILLISECONDS);                        
            }
            
            if( o == null )
                return null;
            
            if( o instanceof Packet )
                return (Packet)o;     
            
            Throwable e = (Throwable)o;
            throw (IOException)new IOException("Async error occurred: "+e).initCause(e);
            
        } catch (InterruptedException e) {
            throw new InterruptedIOException(e.getMessage());
        }
    }

    /**
     * @see org.apache.activeio.Disposable#dispose()
     */
    public void dispose() {
        asyncChannel.dispose();
    }

    /**
     * @see org.apache.activeio.Service#start()
     */
    public void start() throws IOException {
        asyncChannel.start();
    }

    /**
     * @see org.apache.activeio.Service#stop()
     */
    public void stop() throws IOException {
        asyncChannel.stop();
    }

    /**
     * @see org.apache.activeio.packet.async.AsyncChannelListener#onPacket(org.apache.activeio.packet.Packet)
     */
    public void onPacket(Packet packet) {
        try {
            buffer.put(packet);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }        
    }

    /**
     * @see org.apache.activeio.packet.async.AsyncChannelListener#onPacketError(org.apache.activeio.ChannelException)
     */
    public void onPacketError(IOException error) {
        try {
            buffer.put(error);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }        
    }

    public Object getAdapter(Class target) {
        if( target.isAssignableFrom(getClass()) ) {
            return this;
        }
        return asyncChannel.getAdapter(target);
    }    

    public AsyncChannel getAsyncChannel() {
        return asyncChannel;
    }
    
    public String toString() {
        return asyncChannel.toString();
    }
}