package org.cmxyzx.push.message;

import org.cmxyzx.push.heartbeat.SocketPool;
import org.cmxyzx.push.push.PushQueue;
import org.cmxyzx.push.service.ServiceExecutor;
import org.cmxyzx.push.util.LogUtil;
import org.cmxyzx.push.util.TextUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class MessageState {
    public static final int CMD_READ_MSG_CLIENT = 1 << 0;
    public static final int CMD_HEARTBEAT_CLIENT = 1 << 1;
    public static final int CMD_PUSH_MSG_CLIENT = 1 << 2;
    public static final int CMD_SEND_MSG_SERVER = 1 << 3;
    public static final int CMD_ADD_UUID_SERVER = 1 << 4;
    public static final int CMD_DEL_UUID_SERVER = 1 << 5;

    private static final int PKG_HEAD_LENGTH = 42;

    private static SocketChannel mChannel;

    public MessageState() {
    }

    public void setChannel(SocketChannel sc) {
        this.mChannel = sc;
    }

    public void processRead() {
        ServiceExecutor executor = ServiceExecutor.getInstance();
        executor.execute(new Reading());
    }

    public void processClientRead() {
        ServiceExecutor executor = ServiceExecutor.getInstance();
        executor.execute(new ClientReading());
    }

    class Reading implements Runnable {
        private MessagePool mPool;
        private PushQueue mQueue;

        public Reading() {
            mPool = MessagePool.getInstance();
            mQueue = PushQueue.getInstance();
        }

        @Override
        public void run() {
            if (mChannel != null) {
                byte[] head = new byte[PKG_HEAD_LENGTH];
                ByteBuffer headBuffer = ByteBuffer.wrap(head);
                int pkgLength;
                try {
                    int readNum = mChannel.read(headBuffer);
                    if (readNum == PKG_HEAD_LENGTH) {
                        pkgLength = ((head[40] & 0xFF) << 8) | (head[41] & 0xFF);
                        ByteBuffer readBuffer = ByteBuffer.allocate(pkgLength);
                        mChannel.read(readBuffer);
                        ByteArrayOutputStream bos = new ByteArrayOutputStream(PKG_HEAD_LENGTH + pkgLength);
                        bos.write(head);
                        bos.write(readBuffer.array());
                        Message msg = new Message(bos.toByteArray());
                        processMsg(msg);
                    } else {
                        protocolError();
                    }

                } catch (IOException e) {
                    LogUtil.logE("IOException during MessageState Reading", e);
                }
            }

        }

        private void processMsg(Message msg) {
            //possible same msg with addUUID & msg payload, doing add UUID first
            int command = msg.getCommand();
            if ((command & CMD_ADD_UUID_SERVER) == CMD_ADD_UUID_SERVER) {
                mPool.putMsg(msg);
            }
            if ((command & CMD_SEND_MSG_SERVER) == CMD_SEND_MSG_SERVER) {
                Message msgOrig = mPool.getMsg(msg.getUUID());
                if (msgOrig != null) {
                    //only accept message for exist UUID
                    mPool.putMsg(msg);
                    mQueue.enQueueMsg(msg);
                } else {
                    //TODO UUID not exist
                }
            }
            if ((command & CMD_DEL_UUID_SERVER) == CMD_DEL_UUID_SERVER) {
                mPool.removeUUID(msg.getUUID());
            }

        }

    }

    class ClientReading implements Runnable {
        private MessagePool mPool;
        //private PushQueue mQueue;

        public ClientReading() {
            mPool = MessagePool.getInstance();
            // mQueue = PushQueue.getInstance();
        }

        @Override
        public void run() {
            if (mChannel != null) {
                byte[] head = new byte[PKG_HEAD_LENGTH];
                ByteBuffer headBuffer = ByteBuffer.wrap(head);
                try {
                    int readNum = mChannel.read(headBuffer);
                    if (readNum == PKG_HEAD_LENGTH) {
                        byte[] back = processCommand(head);
                        if (back != null) {
                            ByteBuffer sendBuffer = ByteBuffer.wrap(back);
                            sendBuffer.flip();
                            mChannel.write(sendBuffer);
                        }
                    } else {
                        protocolError();
                    }

                } catch (IOException e) {
                    LogUtil.logE("IOException during MessageState ClientReading", e);
                }
            }

        }

        private byte[] processCommand(byte[] head) {
            Message msg = new Message(head);
            String UUID = msg.getUUID();
            //add socket to SocketPool with Provided UUID
            if (TextUtil.checkUUID(UUID)) {
                if (mPool.getMsg(UUID) != null) {
                    //only add socket to pool when UUID exist (AppServer added to Service)
                    SocketPool socketPool = SocketPool.getInstance();
                    Socket socket = mChannel.socket();

                    socketPool.putSocket(msg.getUUID(), socket);
                }
            }

            int command = msg.getCommand();
            if ((command & CMD_READ_MSG_CLIENT) == CMD_READ_MSG_CLIENT) {
                Message msgBack = mPool.getMsg(msg.getUUID());
                msgBack.setCommand(CMD_PUSH_MSG_CLIENT);
                return msgBack.getData();
            }

            return null;
        }


    }

    private void protocolError() {//TODO protocol error?}

    }
}
