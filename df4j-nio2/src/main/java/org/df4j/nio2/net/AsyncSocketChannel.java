/*
 * Copyright 2011-2012 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.df4j.nio2.net;

import org.df4j.core.connector.messagescalar.ScalarCollector;
import org.df4j.core.connector.messagescalar.ScalarSubscriber;
import org.df4j.core.util.Logger;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;

/**
 * Wrapper over {@link AsynchronousSocketChannel}.
 * Simplifies input-output, handling queues of I/O requests.
 * 
 * For client-side connections, instatntiate and call connect(addr).
 * For server-side connections, new instances are generated by
 * {@link AsyncServerSocketChannel}.
 *  
 * Internally, manages 2 input queues: one for reading requests and one for writing requests.
 * After request is served, it is sent to the port denoted by <code>replyTo</code>
 * property in the request.
 * 
 * IO requests can be posted immediately, but will be executed
 * only after connection completes.
 * If interested in the moment when connection is established,
 * add a listener to connEvent.
 */
public class AsyncSocketChannel implements ScalarSubscriber<AsynchronousSocketChannel>
{
    protected static final Logger LOG = Logger.getLogger(AsyncSocketChannel.class.getName());

    private final ScalarCollector<AsyncSocketChannel> backPort;

	/** read requests queue */
	public final Reader reader = new Reader();
	/** write requests queue */
	public final Writer writer = new Writer();

    protected volatile AsynchronousSocketChannel channel;

    public String name;

    public AsyncSocketChannel(ScalarCollector<AsyncSocketChannel> backPort) {
        this.backPort = backPort;
        LOG.config(getClass().getName()+" created");
    }

    public AsyncSocketChannel() {
        this(null);
    }

    public void setTcpNoDelay(boolean on) throws IOException {
        channel.setOption(StandardSocketOptions.TCP_NODELAY, on);
    }

    public void post(AsynchronousSocketChannel channel) {
        LOG.info("conn "+name+": init()");
        this.channel=channel;
        reader.start();
        writer.start();
    }

    public void postFailure(Throwable ex) {
        LOG.info("conn "+name+": postFailure()");
    }

    /** disallows subsequent posts of requests; already posted requests
     * would be processed.
     * @throws IOException 
     */
    public synchronized void close() {
        AsynchronousSocketChannel locchannel;
        synchronized (this) {
            locchannel = channel;
            channel=null;
        }
    	if (locchannel!=null) {
            try {
                locchannel.close();
            } catch (IOException e) {
            }
    	}
    	if (backPort != null) {
            backPort.post(this);
        }
    }

    public synchronized boolean isClosed() {
        return channel==null;
    }

    //===================== inner classes
    
    /**
     * callback for connection completion
     * works both in client-side and server-side modes
     */
    
    public class Reader extends BuffProcessor implements CompletionHandler<Integer, ByteBuffer> {
        public Reader() {
            super(AsyncSocketChannel.this);
        }

        protected void doIO(ByteBuffer buffer) {
            LOG.info("conn "+name+": read() started");
            if (timeout>0) {
                channel.read(buffer, timeout, TimeUnit.MILLISECONDS, buffer, this);
            } else {
                channel.read(buffer, buffer, this);
            }
        }

    }
    
    public class Writer extends BuffProcessor implements CompletionHandler<Integer, ByteBuffer> {

        public Writer() {
            super(AsyncSocketChannel.this);
        }

        protected void doIO(ByteBuffer buffer) {
            LOG.info("conn "+name+": write() started.");
            if (timeout>0) {
                channel.write(buffer, timeout, TimeUnit.MILLISECONDS, buffer, this);
            } else {
                channel.write(buffer, buffer, this);
            }
        }
    }

}
