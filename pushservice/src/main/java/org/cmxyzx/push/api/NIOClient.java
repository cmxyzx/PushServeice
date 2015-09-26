package org.cmxyzx.push.api;

import org.cmxyzx.push.message.Message;
import org.cmxyzx.push.message.MessageState;
import org.cmxyzx.push.util.LogUtil;
import org.cmxyzx.push.util.WriteQueue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by Anthony on 2015/8/12.
 * NIOClient only grant access to same package that using NIO connecting with Push server
 */
public class NIOClient {
    private static final int SELECTOR_TIME_OUT = 30;
    private static final int DEFAULT_BLOCK = 4096;
    private InetSocketAddress mAddress;
    private SocketChannel mSocket;
    private Selector mSelector;
    private volatile boolean mClientRunning = false;
    private WriteQueue mQueue;


    NIOClient(InetSocketAddress address) {
        mAddress = address;
    }

    void init() throws IOException {
        if (mAddress != null) {
            mClientRunning = true;
            mSocket = SocketChannel.open();
            mSocket.configureBlocking(false);
            mSelector = Selector.open();
            mSocket.register(mSelector, SelectionKey.OP_CONNECT);
            mSocket.connect(mAddress);

            mQueue = new WriteQueue();
            Thread client = new Thread(new ClientWork(), "PUSH_API_CLIENT_WORK");
            client.start();
        }
    }

    void sendMessage(Message msg) throws IOException, InterruptedException, NotInitException {
        List<Message> list = new ArrayList<>();
        list.add(msg);
        sendMessageList(list);
    }

    void sendMessageList(List<Message> msgList) throws IOException, InterruptedException, NotInitException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (Message msg : msgList
                ) {
            bos.write(msg.getData());
        }
        if (mQueue != null) {
            mQueue.enQueueBuffer(ByteBuffer.wrap(bos.toByteArray()));
        } else {
            throw new NotInitException("Client Used before init");
        }

    }

    void closeConnection() {
        mClientRunning = false;
    }

    private void processReadBuffer(ByteBuffer readBuffer) {
        if (readBuffer != null) {
            readBuffer.flip();
            Message msg = new Message(readBuffer.array());
            int command = msg.getCommand();
            if ((command & MessageState.CMD_OP_ACK_SERVER) == MessageState.CMD_OP_ACK_SERVER) {
                //// TODO: 2015/8/12 checked pushService returned success status
                LogUtil.logI("CMD_OP_ACK_SERVER");
            }
            if ((command & MessageState.CMD_OP_NCK_SERVER) == MessageState.CMD_OP_NCK_SERVER) {
                //// TODO: 2015/8/12 checked pushService returned failed status
                LogUtil.logI("CMD_OP_NCK_SERVER");
            }
        }
    }


    class ClientWork implements Runnable {

        @Override
        public void run() {
            try {
                while (mClientRunning) {
                    if (mSelector.select(SELECTOR_TIME_OUT * 1000) > 0) {
                        Set<SelectionKey> keySet = mSelector.selectedKeys();
                        Iterator<SelectionKey> keyIterator = keySet.iterator();
                        while (keyIterator.hasNext()) {
                            SelectionKey key = keyIterator.next();
                            keyIterator.remove();
                            if (key.isConnectable()) {
                                SocketChannel channel = (SocketChannel) key.channel();
                                if (channel.isConnectionPending()) {
                                    channel.finishConnect();
                                    channel.register(mSelector, SelectionKey.OP_WRITE);
                                }
                            }
                            if (key.isWritable()) {
                                if (mQueue != null) {
                                    ByteBuffer sendBuffer = mQueue.deQueueBuffer();
                                    if (sendBuffer != null) {//check if timeout return null object
                                        SocketChannel channel = (SocketChannel) key.channel();
                                        if (channel.isConnected()) {
                                            //mSendBuffer.flip();
                                            channel.write(sendBuffer);
                                            LogUtil.logD("ClientWriting:" + new String(sendBuffer.array()));
                                            channel.register(mSelector, SelectionKey.OP_READ);
                                        }
                                    }
                                }


                            } else if (key.isReadable()) {
                                SocketChannel channel = (SocketChannel) key.channel();
                                ByteBuffer readBuffer = ByteBuffer.allocate(DEFAULT_BLOCK);
                                channel.read(readBuffer);
                                processReadBuffer(readBuffer);
                                channel.register(mSelector, SelectionKey.OP_WRITE);
                            }
                        }
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                if (mSelector != null) {
                    try {
                        mSelector.wakeup();
                        mSelector.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
