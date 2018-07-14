package org.df4j.nio2.net;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class ClientConnection extends AsyncSocketChannel
        implements CompletionHandler<Void,AsynchronousSocketChannel>
{
    /**
     * Starts connection to a server. IO requests can be queued immediately,
     * but will be executed only after connection completes.
     *
     * @throws IOException
     */
    public ClientConnection(SocketAddress addr) throws IOException, InterruptedException {
        AsynchronousSocketChannel channel =	AsynchronousSocketChannel.open();
        channel.connect(addr, channel, this);
        name = "ClientConnection";
    }

    //=====================  CompletionHandler callbacks

    @Override
    public void completed(Void result, AsynchronousSocketChannel channel) {
        super.post(channel);
    }

    @Override
    public void failed(Throwable exc, AsynchronousSocketChannel channel) {
        super.postFailure(exc);
    }

}
