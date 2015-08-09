package org.cmxyzx.push.push;

import org.cmxyzx.push.message.MessageState;
import org.cmxyzx.push.service.ServiceBase;
import org.cmxyzx.push.util.LogUtil;
import org.cmxyzx.push.util.PropertiesUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketOptions;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class PushService extends ServiceBase {

    private static PushService mPush = new PushService();


    private PushService() {

    }

    public static PushService getInstance() {
        return mPush;
    }

    public void init(PropertiesUtil properties) throws IOException {
        ServerSocketChannel server = ServerSocketChannel.open();
        server.configureBlocking(false);
        int port = properties.getIntProgerty("PushServicePort");
        mServiceRunning = true;

        server.socket().bind(new InetSocketAddress(port));
        mSelector = Selector.open();
        server.register(mSelector, SelectionKey.OP_ACCEPT);

    }

    @Override
    public void run() {
        if (mSelector != null) {
            try {
                while (mServiceRunning) {

                    int readyNum = mSelector.select();
                    if (readyNum > 0) {
                        Set<SelectionKey> keySet = mSelector.selectedKeys();
                        Iterator<SelectionKey> keyIterator = keySet.iterator();
                        while (keyIterator.hasNext()) {
                            SelectionKey key = keyIterator.next();
                            keyIterator.remove();
                            if (key.isAcceptable()) {
                                ServerSocketChannel server = (ServerSocketChannel) key.channel();
                                MessageState state = new MessageState();
                                SocketChannel client = server.accept();
                                client.configureBlocking(false);
                                client.register(mSelector, SelectionKey.OP_READ, state);
                                //client.socket().setKeepAlive(true);
                                //client.socket().setTcpNoDelay(true);
                                client.setOption(StandardSocketOptions.TCP_NODELAY,true);
                                client.setOption(StandardSocketOptions.SO_KEEPALIVE,true);
                                LogUtil.logV("PushService Accepted:" + client.getRemoteAddress());
                            }
                            if (key.isReadable()) {
                                SocketChannel client = (SocketChannel) key.channel();
                                MessageState state = (MessageState) key.attachment();
                                state.setChannel(client);
                                state.processClientRead();
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (mSelector != null) {
                        mSelector.wakeup();
                        if (mSelector.isOpen())
                            mSelector.close();
                    }
                } catch (IOException e) {
                    LogUtil.logE("IOException", e);
                }
            }
        }

    }

}
