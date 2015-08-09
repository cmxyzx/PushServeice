package org.cmxyzx.push.heartbeat;

import org.cmxyzx.push.message.Message;
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

    private HeartBeatService() {
    }

    public static HeartBeatService getInstance() {
        return mService;
    }

    @Override
    public void init(PropertiesUtil properties) throws IOException {
        mSocketPool = SocketPool.getInstance();
        mPushQueue = PushQueue.getInstance();
    }

    @Override
    public void run() {
        Thread heartBeat = new Thread(new HeartBeatWorker(),"PUSH_SERVER_HEARTBEAT_WORKER");
        heartBeat.setDaemon(true);
        heartBeat.start();
        while (mServiceRunning) {
            Message msg = mPushQueue.deQueueMsg();
            if (msg == null) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    LogUtil.logE("InterruptedException", e);
                }
            } else {
                String UUID = msg.getUUID();
                if (TextUtil.checkUUID(UUID)) {
                    Socket socket = mSocketPool.getSocket(UUID);
                    if (socket != null && socket.isConnected()) {
                        try {
                            OutputStream os = socket.getOutputStream();
                            //os.write(Message.createHeatBeatMsg(UUID));
                            os.write(msg.getData());
                            os.flush();
                        } catch (IOException e) {
                            LogUtil.logE("IOException", e);
                        }
                    } else {
                        //// TODO: 2015/8/9 when msg in PushQueue didn't have corresponding socket
                        //put msg back to push queue ?
                    }
                }
            }
        }
    }

    class HeartBeatWorker implements Runnable {

        @Override
        public void run() {
            BiConsumer<String, Socket> consumer = new SocketConsumer();
            while (mServiceRunning) {
                //// TODO: 2015/8/9 only heartbeat when time interval enough
                mSocketPool.forEach(consumer);
            }
        }
    }

    class SocketConsumer implements BiConsumer<String, Socket> {

        @Override
        public void accept(java.lang.String string, Socket socket) {
            if (socket != null && socket.isConnected() && TextUtil.checkUUID(string)) {
                try {
                    OutputStream os = socket.getOutputStream();
                    os.write(Message.createHeatBeatMsg(string));
                    os.flush();
                } catch (IOException e) {
                    LogUtil.logE("IOException", e);
                }
            }
        }
    }

}
