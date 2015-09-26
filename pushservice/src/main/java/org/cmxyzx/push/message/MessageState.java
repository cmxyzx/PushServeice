package org.cmxyzx.push.message;

import org.cmxyzx.push.heartbeat.HeartBeatService;
import org.cmxyzx.push.heartbeat.ServerMessage;
import org.cmxyzx.push.heartbeat.SocketPool;
import org.cmxyzx.push.push.PushQueue;
import org.cmxyzx.push.service.ServiceExecutor;
import org.cmxyzx.push.util.LogUtil;
import org.cmxyzx.push.util.TextUtil;
import org.cmxyzx.push.util.WriteQueue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class MessageState {
    public static final int CMD_READ_MSG_CLIENT = 1;
    public static final int CMD_HEARTBEAT_CLIENT = 1 << 1;
    public static final int CMD_PUSH_MSG_CLIENT = 1 << 2;
    public static final int CMD_SEND_MSG_SERVER = 1 << 3;
    public static final int CMD_ADD_UUID_SERVER = 1 << 4;
    public static final int CMD_DEL_UUID_SERVER = 1 << 5;
    public static final int CMD_UNREAD_MSG_CLIENT = 1 << 6;
    public static final int CMD_OP_ACK_SERVER = 1 << 7;
    public static final int CMD_OP_NCK_SERVER = 1 << 8;
    public static final int CMD_NO_MSG_CLIENT = 1 << 9;
    public static final int CMD_CMD_EMPTY = 1 << 10;

    private static final int PKG_HEAD_LENGTH = 42;
    private static final String BLANK_UUID = "a0000000-000a-000a-00aa-0a0a0a0aa0aa";

    private static final int WRITE_QUEUE_SIZE = 6;

    private final SocketChannel mChannel;
    private final WriteQueue mWriteQueue;

    private volatile boolean mChannelClosed = true;

    public MessageState(SocketChannel sc) {
        mChannel = sc;
        mWriteQueue = new WriteQueue(WRITE_QUEUE_SIZE);
        mChannelClosed = false;
    }


    public void processRead() {
        ServiceExecutor executor = ServiceExecutor.getInstance();
        executor.execute(new Reading());
    }

    public ByteBuffer processWrite() throws IOException, InterruptedException {
        return mWriteQueue.deQueueAll();
    }

    public void processClientRead() {
        ServiceExecutor executor = ServiceExecutor.getInstance();
        executor.execute(new ClientReading());
    }

    public boolean getChannelState() {
        return mChannelClosed;
    }

    private void closeChannel() throws IOException {
        if (!mChannelClosed) {
            mChannelClosed = true;
            LogUtil.logI("Disconnected:" + mChannel.getRemoteAddress());
            mChannel.close();

        }
    }

    private void protocolError(String uuid) {
        if (mChannel != null && mChannel.isConnected()) {
            try {
                mWriteQueue.enQueueBuffer(Message.createReplayMsg(uuid, CMD_OP_NCK_SERVER));
            } catch (InterruptedException e) {
                LogUtil.logE("InterruptedException", e);
            }
        }

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
            try {
                if (mChannel != null && mChannel.isOpen() && mChannel.isConnected()) {
                    byte[] head = new byte[PKG_HEAD_LENGTH];
                    ByteBuffer headBuffer = ByteBuffer.wrap(head);
                    int pkgLength;
                    int readNum = mChannel.read(headBuffer);

                    if (readNum == PKG_HEAD_LENGTH) {
                        pkgLength = ((head[40] & 0xFF) << 8) | (head[41] & 0xFF);
                        ByteBuffer readBuffer = ByteBuffer.allocate(pkgLength);
                        mChannel.read(readBuffer);
                        ByteArrayOutputStream bos = new ByteArrayOutputStream(PKG_HEAD_LENGTH + pkgLength);
                        bos.write(head);
                        bos.write(readBuffer.array());
                        Message msg = new Message(bos.toByteArray());
                        ServerMessage sMsg = new ServerMessage(msg, 0, 0, 0, System.currentTimeMillis());
                        int commandBack = processMsg(sMsg);
                        mWriteQueue.enQueueBuffer(Message.createReplayMsg(msg.getUUID(), commandBack));
                    } else if (readNum == -1) {
                        //connection is closed by pear
                        closeChannel();
                    }
                    //else{
                    //    protocolError(BLANK_UUID);
                    //}
                }
            } catch (IOException | InterruptedException e) {
                LogUtil.logE("IOException or InterruptedException during MessageState Reading", e);
            }
        }


        private int processMsg(ServerMessage msg) throws InterruptedException {
            //possible same msg with addUUID & msg payload, doing add UUID first
            int command = msg.getMsg().getCommand();
            int commandBack = CMD_OP_NCK_SERVER;
            if ((command & CMD_ADD_UUID_SERVER) == CMD_ADD_UUID_SERVER) {
                mPool.putMsg(msg);
                commandBack = CMD_OP_ACK_SERVER;
            }
            if ((command & CMD_SEND_MSG_SERVER) == CMD_SEND_MSG_SERVER) {
                ServerMessage msgOrig = mPool.getMsg(msg.getMsg().getUUID());
                if (msgOrig != null) {
                    //only accept message for exist UUID
                    mPool.putMsg(msg);
                    mQueue.enQueueMsg(msg);
                    commandBack = CMD_OP_ACK_SERVER;
                } else {
                    LogUtil.logI("UUID not exist");
                    commandBack = CMD_OP_NCK_SERVER;
                }
            }
            if ((command & CMD_DEL_UUID_SERVER) == CMD_DEL_UUID_SERVER) {
                mPool.removeUUID(msg.getMsg().getUUID());
                commandBack = CMD_OP_ACK_SERVER;
            }
            return commandBack;
        }

    }

    class ClientReading implements Runnable {
        private MessagePool mMsgPool;
        private SocketPool mSocketPool;
        private HeartBeatService mHeartBeat;
        //private PushQueue mQueue;

        public ClientReading() {
            mMsgPool = MessagePool.getInstance();
            mSocketPool = SocketPool.getInstance();
            mHeartBeat = HeartBeatService.getInstance();
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
                        Message msg = new Message(head);
                        byte[] back = processCommand(msg);
                        if (back != null) {
                            ByteBuffer sendBuffer = ByteBuffer.wrap(back);
                            sendBuffer.flip();
                            mWriteQueue.enQueueBuffer(sendBuffer);
                        } else {
                            if ((msg.getCommand() & CMD_READ_MSG_CLIENT) == CMD_READ_MSG_CLIENT) {
                                mWriteQueue.enQueueBuffer(Message.createReplayMsg(msg.getUUID(), CMD_NO_MSG_CLIENT));
                            } else {
                                protocolError(msg.getUUID());
                            }
                        }
                    } else if (readNum == -1) {
                        //connection is closed by pear
                        closeChannel();
                    }// else {
                    //   protocolError(BLANK_UUID);
                    //}

                } catch (IOException | InterruptedException e) {
                    LogUtil.logE("IOException or InterruptedException during MessageState ClientReading", e);
                }
            }

        }

        private byte[] processCommand(Message msg) {

            String UUID = msg.getUUID();
            //add socket to SocketPool with Provided UUID
            if (TextUtil.checkUUID(UUID)) {
                if (mMsgPool.getMsg(UUID) != null) {
                    //only add socket to pool when UUID exist (AppServer added to Service)
                    Socket socket = mChannel.socket();

                    mSocketPool.putSocket(msg.getUUID(), socket);
                    mHeartBeat.addHeartBeat(msg.getUUID(), socket);
                }
            }

            int command = msg.getCommand();
            if ((command & CMD_READ_MSG_CLIENT) == CMD_READ_MSG_CLIENT) {
                ServerMessage msgBack = mMsgPool.getMsg(msg.getUUID());
                if (msgBack != null && (msgBack.getMsg().getCommand() & CMD_ADD_UUID_SERVER) == 0) { //when msg have real payload then push it to client
                    msgBack.getMsg().setCommand(CMD_PUSH_MSG_CLIENT);
                    msgBack.setLastReadMsg(System.currentTimeMillis());
                    return msgBack.getMsg().getData();
                }
            }

            return null;
        }


    }
}
