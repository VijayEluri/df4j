package org.df4j.nio2.net.echo;

import org.df4j.core.scalar.Semafor;
import org.df4j.core.util.invoker.Action;
import org.df4j.core.scalar.ext.AsyncAction;
import org.df4j.nio2.net.AsyncServerSocketChannel;
import org.df4j.nio2.net.ServerConnection;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.function.Consumer;

/**
 * generates {@link ServerConnection}s and passes them to AsyncServerSocketChannel to initialize
 *
 */
public class ConnectionManager extends AsyncAction {
    private final AsyncServerSocketChannel assc;
    Semafor allowedConnections = new Semafor(this);
    int serialnum=0;

    Consumer<ServerConnection> backport = (asyncSocketChannel) -> {
        allowedConnections.release();
    };

    public ConnectionManager(SocketAddress addr, int connCount) throws IOException {
        assc = new AsyncServerSocketChannel(addr);
        allowedConnections.release(connCount);
    }

    @Action
    protected void act() {
        ServerConnection conn = new EchoServer(backport);
        conn.name = "EchoServerConnection"+(serialnum++);
        assc.subscribe(conn);
    }

    public void close() {
        assc.close();
        stop();
    }
}
