package org.cmxyzx.push;

import org.cmxyzx.push.heartbeat.HeartBeatService;
import org.cmxyzx.push.message.MessageService;
import org.cmxyzx.push.push.PushService;
import org.cmxyzx.push.util.PropertiesUtil;

import java.io.IOException;

public class PushServer {

    private static PushServer mServer = new PushServer();

    private MessageService mMsgService;
    private PushService mPushService;
    private HeartBeatService mHeartBeatService;

    private PushServer() {

    }

    public static PushServer getInstance() {
        return mServer;
    }

    public void initServer() throws IOException {
        initServer(PropertiesUtil.DEFAULT_CONFIG);
    }

    public void initServer(String config) throws IOException {
        PropertiesUtil properties = PropertiesUtil.getInstance(config);
        boolean heartbeatMode = properties.getBoolProperty("HeartBeatMode");

        mMsgService = MessageService.getInstance();
        mMsgService.init(properties);

        mPushService = PushService.getInstance();
        mPushService.init(properties);

        if (heartbeatMode) {
            mHeartBeatService = HeartBeatService.getInstance();
            mHeartBeatService.init(properties);
        }
    }

    public void startServer() {
        if (mMsgService != null) {
            Thread msgServiceThread = new Thread(mMsgService, "PUSH_SERVER_MSG_SERVICE");
            //msgServiceThread.setDaemon(true);
            msgServiceThread.start();
        }
        if (mPushService != null) {
            Thread pushServiceThread = new Thread(mPushService, "PUSH_SERVER_PUSH_SERVICE");
            //pushServiceThread.setDaemon(true);
            pushServiceThread.start();
        }
        if (mHeartBeatService != null) {
            Thread heartbeatServiceThread = new Thread(mHeartBeatService, "PUSH_SERVER_HEARTBEAT_SERVICE");
            heartbeatServiceThread.setDaemon(true);
            heartbeatServiceThread.start();
        }
    }

    public void stopServer() {
        if (mMsgService != null) {
            mMsgService.stopService();
        }
        if (mPushService != null) {
            mPushService.stopService();
        }
        if (mHeartBeatService != null) {
            mHeartBeatService.stopService();
        }
    }


    public static void main(String[] args) {
        PushServer server = PushServer.getInstance();
        try {
            server.initServer();
            server.startServer();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
