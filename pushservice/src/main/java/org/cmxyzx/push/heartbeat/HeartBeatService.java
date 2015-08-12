package org.cmxyzx.push.heartbeat;

import org.cmxyzx.push.message.Message;
import org.cmxyzx.push.message.MessagePool;
import org.cmxyzx.push.message.MessageState;
import org.cmxyzx.push.push.PushQueue;
import org.cmxyzx.push.service.ServiceBase;
import org.cmxyzx.push.util.LogUtil;
import org.cmxyzx.push.util.PropertiesUtil;
import org.cmxyzx.push.util.TextUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class HeartBeatService extends ServiceBase {
    private static HeartBeatService mService = new HeartBeatService();
    private SocketPool mSocketPool;
    private PushQueue mPushQueue;
    private ScheduledExecutorService mExecutor;
    private int mHeartBeatInterval = 60;

    private HeartBeatService() {
    }

    public static HeartBeatService getInstance() {
        return mService;
    }

    @Override
    public void init(PropertiesUtil properties) throws IOException {
        mSocketPool = SocketPool.getInstance();
        mPushQueue = PushQueue.getInstance();
        int HeartBeatExecutorThreadNum = properties.getHeartBeatExecutorThreadNum();
        mExecutor = Executors.newScheduledThreadPool(HeartBeatExecutorThreadNum);
        mHeartBeatInterval = properties.getHeartBeatInterval();
    }

    @Override
    public void run() {
        int threadSleep = 10;
        int threadSleepMax = 500;
        int sleepStep = 10;

        //Thread heartBeat = new Thread(new HeartBeatWorker(), "PUSH_SERVER_HEARTBEAT_WORKER");
        //heartBeat.setDaemon(true);
        // heartBeat.start();

        BiConsumer<String, Socket> consumer = new SocketConsumer();
        mSocketPool.forEach(consumer);// only first time iterate the socket already exist in the socketPool to add HeartBeat thread.
        //after that need to call addHeartBeat() add HeartBeat manually

        while (mServiceRunning) {
            ServerMessage msg = mPushQueue.deQueueMsg();

            if (msg == null) {
                try {
                    Thread.sleep(threadSleep);
                    if (threadSleep < threadSleepMax) {
                        threadSleep += sleepStep; //dynamic adding sleep time when push queue have no msg, save cpu time
                    }
                } catch (InterruptedException e) {
                    LogUtil.logE("InterruptedException", e);
                }
            } else {
                threadSleep = sleepStep;// reset sleep time to minimum value for lower latency
                String UUID = msg.getMsg().getUUID();
                if (TextUtil.checkUUID(UUID)) {
                    Socket socket = mSocketPool.getSocket(UUID);
                    if (socket != null && socket.isConnected()) {
                        try {
                            OutputStream os = socket.getOutputStream();
                            //os.write(Message.createHeatBeatMsg(UUID));
                            Message send = msg.getMsg();
                            send.setCommand(send.getCommand() | MessageState.CMD_PUSH_MSG_CLIENT);
                            os.write(send.getData());
                            os.flush();
                            msg.setLastPushMsg(System.currentTimeMillis());
                        } catch (IOException e) {
                            LogUtil.logE("IOException", e);
                        }
                    } else {
                        //when msg in PushQueue didn't have corresponding socket, no need to put msg back to push queue, msg pool have this copy, wait for client check unread msg with service
                        // just ignore this msg from push queue
                        LogUtil.logI("msg in PushQueue didn't have corresponding socket");
                    }
                }
            }
        }
    }

    public void addHeartBeat(String uuid, Socket socket) {
        mExecutor.scheduleAtFixedRate(new HeartBeatWorker(uuid, socket), 0, mHeartBeatInterval, TimeUnit.SECONDS);
    }


    class HeartBeatWorker implements Runnable {
        private String mUuid;
        private Socket mSocket;
        private MessagePool mPool;

        public HeartBeatWorker(String uuid, Socket socket) {
            mUuid = uuid;
            mSocket = socket;
            mPool = MessagePool.getInstance();
        }

        @Override
        public void run() {
            if (TextUtil.checkUUID(mUuid)) {
                ServerMessage msg = mPool.getMsg(mUuid);
                long now = System.currentTimeMillis();
                if (msg != null && ((now - msg.getLastHeartBeat()) > mHeartBeatInterval)) {
                    try {
                        if (mSocket != null && mSocket.isConnected()) {
                            OutputStream os = mSocket.getOutputStream();
                            os.write(Message.createHeatBeatMsg(mUuid, msg.haveUnreadMsg()));
                            os.flush();
                            msg.setLastHeartBeat(now);
                        }
                    } catch (IOException e) {
                        LogUtil.logE("IOException", e);
                    }
                }
            }



/*
            long lastRun = System.currentTimeMillis();
            long now;
            while (mServiceRunning) {

                now = System.currentTimeMillis();
                if ((now - lastRun) < (mHeartBeatInterval*1000)) {
                    try {
                        Thread.sleep((mHeartBeatInterval*1000) - (now - lastRun));
                    } catch (InterruptedException e) {
                        LogUtil.logE("InterruptedException", e);
                    }
                } else if ((now - lastRun) > (mHeartBeatInterval*1000)) {
                    LogUtil.logI("HeartBeatWork Overload ");
                }
                lastRun = now;
            }
            */
        }
    }

    class SocketConsumer implements BiConsumer<String, Socket> {

        @Override
        public void accept(java.lang.String string, Socket socket) {
            mExecutor.scheduleAtFixedRate(new HeartBeatWorker(string, socket), 0, mHeartBeatInterval, TimeUnit.SECONDS);
        }
    }

}
