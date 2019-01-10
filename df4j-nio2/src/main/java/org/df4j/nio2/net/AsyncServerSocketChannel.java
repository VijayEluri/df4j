/*
 * Copyright 2013 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.nio2.net;

import org.df4j.core.boundconnector.messagescalar.ScalarPublisher;
import org.df4j.core.boundconnector.messagescalar.ScalarSubscriber;
import org.df4j.core.boundconnector.messagestream.StreamInput;
import org.df4j.core.tasknode.AsyncAction;
import org.df4j.core.util.Logger;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 * Accepts incoming connections, pushes them pu subscribers
 *
 * though it extends AsyncAction, it is effectively an Actor1&lt;ScalarSubscriber&gt;
 *
 *  its sole input is a stream of requests of type ServerConnection
 *  for each ServerConnection, AsyncServerSocketChannel accepts an incoming connection requests
 *  from a client and passes it to the ServerConnection
 *
 *  when there are no ServerConnection on the input, client requests are not accepted.
 *
 */
public class AsyncServerSocketChannel
        extends AsyncAction
        implements ScalarPublisher<AsynchronousSocketChannel>,
        CompletionHandler<AsynchronousSocketChannel, ScalarSubscriber<? super AsynchronousSocketChannel>>
{
    protected final Logger LOG = Logger.getLogger(AsyncServerSocketChannel.class.getName());

    /** place for demands */
    private StreamInput<ScalarSubscriber<? super AsynchronousSocketChannel>> requests = new StreamInput<>(this);

    protected volatile AsynchronousServerSocketChannel assc;

    public AsyncServerSocketChannel(SocketAddress addr) throws IOException {
        if (addr == null) {
            throw new NullPointerException();
        }
        assc = AsynchronousServerSocketChannel.open();
        assc.bind(addr);
        this.start(directExec);
        LOG.config("AsyncServerSocketChannel("+addr+") created");
    }
    
    @Override
    public Subscription subscribe(ScalarSubscriber<AsynchronousSocketChannel> subscriber) {
        requests.onNext(subscriber);
        return null;
    }

    public synchronized void close() {
        if (assc == null) {
            return;
        }
        AsynchronousServerSocketChannel asscLock = assc;
        assc = null;
        try {
            asscLock.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    //====================== Dataflow backend

    @Override
    protected void runAction() throws Exception {
        ScalarSubscriber<? super AsynchronousSocketChannel> arg = requests.next();
        try {
            assc.accept(arg, this);
        } catch (Exception e) {
            close();
        }
        // no start() at this point, it will be called later in the handler
    }

    //====================== CompletionHandler's backend

    @Override
    public void completed(AsynchronousSocketChannel result, ScalarSubscriber<? super AsynchronousSocketChannel> connection) {
        LOG.finest("AsynchronousServerSocketChannel: request accepted");
        connection.onNext(result);
        this.start(); // allow  next assc.accpt()
    }

    /**
     * new client connection failed
     * TODO count failures, do not retry if many
     */
    @Override
    public void failed(Throwable exc, ScalarSubscriber<? super AsynchronousSocketChannel> connection) {
        connection.onError(exc);
        if (exc instanceof AsynchronousCloseException) {
            // channel closed.
            close();
        } else {
            this.start(); // TODO deside if we should allow next call to assc.accept() after failure?
        }
    }

}
