package org.cmxyzx.push.heartbeat;

import org.cmxyzx.push.message.Message;
import org.cmxyzx.push.message.MessagePool;
import org.cmxyzx.push.push.PushQueue;
import org.cmxyzx.push.service.ServiceBase;
import org.cmxyzx.push.util.LogUtil;
import org.cmxyzx.push.util.PropertiesUtil;
import org.cmxyzx.push.util.TextUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.function.BiConsumer;

public class HeartBeatService extends ServiceBase {
    private static HeartBeatService mService = new HeartBeatService();
    private SocketPool mSocketPool;
    private PushQueue mPushQueue;
    //private ScheduledExecutorService mExecutor;
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
        //mExecutor = Executors.newScheduledThreadPool(HeartBeatExecutorThreadNum);
        mHeartBeatInterval = properties.getHeartBeatInterval();
    }

    @Override
    public void run() {
        Thread heartBeat = new Thread(new HeartBeatWorker(), "PUSH_SERVER_HEARTBEAT_WORKER");
        heartBeat.setDaemon(true);
        heartBeat.start();

        while (mServiceRunning) {
            ServerMessage msg = mPushQueue.deQueueMsg();
            if (msg == null) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    LogUtil.logE("InterruptedException", e);
                }
            } else {
                String UUID = msg.getMsg().getUUID();
                if (TextUtil.checkUUID(UUID)) {
                    Socket socket = mSocketPool.getSocket(UUID);
                    if (socket != null && socket.isConnected()) {
                        try {
                            OutputStream os = socket.getOutputStream();
                            //os.write(Message.createHeatBeatMsg(UUID));
                            os.write(msg.getMsg().getData());
                            os.flush();
                            msg.setLastPushMsg(System.currentTimeMillis());
                        } catch (IOException e) {
                            LogUtil.logE("IOException", e);
                        }
                    } else {
                        //// TODO: 2015/8/9 when msg in PushQueue didn't have corresponding socket
                        //put msg back to push queue ?
                        LogUtil.logI("msg in PushQueue didn't have corresponding socket");
                    }
                }
            }
        }
    }

    class HeartBeatWorker implements Runnable {

        @Override
        public void run() {
            BiConsumer<String, Socket> consumer = new SocketConsumer();
            long lastRun = System.currentTimeMillis();
            long now;
            while (mServiceRunning) {
                mSocketPool.forEach(consumer);
                now = System.currentTimeMillis();
                if ((now - lastRun) < 30 * 1000) {
                    try {
                        Thread.sleep(30 * 1000 - (now - lastRun));
                    } catch (InterruptedException e) {
                        LogUtil.logE("InterruptedException", e);
                    }
                } else if ((now - lastRun) > mHeartBeatInterval) {
                    //// TODO: 2015/8/10 HeartBeatWork Overload
                    LogUtil.logI("HeartBeatWork Overload ");
                }
                lastRun = now;
            }
        }
    }

    class SocketConsumer implements BiConsumer<String, Socket> {
        private MessagePool mPool;

        public SocketConsumer() {
            mPool = MessagePool.getInstance();
        }

        @Override
        public void accept(java.lang.String string, Socket socket) {
            //mExecutor.scheduleAtFixedRate(new HeartBeatWorker(string, socket),0,mHeartBeatInterval, TimeUnit.SECONDS);
            if (TextUtil.checkUUID(string)) {
                ServerMessage msg = mPool.getMsg(string);
                long now = System.currentTimeMillis();
                if (msg != null && ((now - msg.getLastHeartBeat()) > mHeartBeatInterval)) {
                    try {
                        if (socket != null && socket.isConnected()) {
                            OutputStream os = socket.getOutputStream();
                            os.write(Message.createHeatBeatMsg(string));
                            os.flush();
                            msg.setLastHeartBeat(now);
                        }
                    } catch (IOException e) {
                        LogUtil.logE("IOException", e);
                    }
                }
            }
        }
    }

}
