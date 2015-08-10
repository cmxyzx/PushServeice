package org.cmxyzx.push.push;

import org.cmxyzx.push.heartbeat.ServerMessage;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Anthony on 2015/8/9.
 * PushQueue wrap the ConcurrentLinkedQueue for Message to push
 */
public class PushQueue {
    private static PushQueue mInstance = new PushQueue();

    private ConcurrentLinkedQueue<ServerMessage> mMsgQueue;

    private PushQueue() {
        mMsgQueue = new ConcurrentLinkedQueue<>();
    }

    public static PushQueue getInstance() {
        return mInstance;
    }

    public ServerMessage deQueueMsg() {
        ServerMessage msg = null;
        if (!mMsgQueue.isEmpty()) {
            msg = mMsgQueue.poll();
        }
        return msg;
    }

    public void enQueueMsg(ServerMessage msg) {
        if (msg != null) {
            mMsgQueue.offer(msg);
        }
    }


}
