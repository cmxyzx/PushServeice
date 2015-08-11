package org.cmxyzx.push.message;

import org.cmxyzx.push.service.ServiceBase;
import org.cmxyzx.push.util.LogUtil;
import org.cmxyzx.push.util.PropertiesUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class MessageService extends ServiceBase {
    private static MessageService mService = new MessageService();

    private MessageService() {

    }

    public static MessageService getInstance() {
        return mService;
    }

    public void init(PropertiesUtil properties) throws IOException {
        ServerSocketChannel server = ServerSocketChannel.open();
        server.configureBlocking(false);
        int port = properties.getIntProgerty("MessageServicePort");
        mServiceRunning = true;
        server.socket().bind(new InetSocketAddress(port));
        mSelector = Selector.open();
        server.register(mSelector, SelectionKey.OP_ACCEPT);
    }

    @Override
    public void run() {
        if (mSelector != null) {
            try {
                // boolean closed = false;
                while (mServiceRunning) {

                    int readyNum;
                    //if (closed) {
                    //    readyNum = mSelector.selectNow();
                    //     closed = false;
                    // } else {
                        readyNum = mSelector.select();
                    // }

                    if (readyNum > 0) {
                        Set<SelectionKey> keySet = mSelector.selectedKeys();
                        Iterator<SelectionKey> keyIterator = keySet.iterator();
                        while (keyIterator.hasNext()) {
                            SelectionKey key = keyIterator.next();
                            keyIterator.remove();
                            if (key.isValid() && key.isAcceptable()) {
                                ServerSocketChannel server = (ServerSocketChannel) key.channel();
                                MessageState state = new MessageState();
                                SocketChannel client = server.accept();
                                client.configureBlocking(false);
                                client.register(mSelector, SelectionKey.OP_READ, state);
                                LogUtil.logV("MessageService Accepted:" + client.getRemoteAddress());
                            }
                            if (key.isValid() && key.isReadable()) {
                                SocketChannel client = (SocketChannel) key.channel();
                                MessageState state = (MessageState) key.attachment();
                                state.setChannel(client);
                                state.processRead();
                                //client.close();
                                //closed = true;
                                //key.cancel();
                            }
                        }
                    }
                }
            } catch (IOException e) {
                LogUtil.logE("IOException", e);
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
