package org.cmxyzx.push.push;

import org.cmxyzx.push.message.Message;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Anthony on 2015/8/9.
 */
public class PushQueue {
    private static PushQueue mInstance = new PushQueue();

    private ConcurrentLinkedQueue<Message> mMsgQueue;

    private PushQueue() {
        mMsgQueue = new ConcurrentLinkedQueue<>();
    }

    public static PushQueue getInstance() {
        return mInstance;
    }

    public Message deQueueMsg() {
        Message msg = null;
        if (!mMsgQueue.isEmpty()) {
            msg = mMsgQueue.poll();
        }
        return msg;
    }

    public void enQueueMsg(Message msg) {
        if (msg != null) {
            mMsgQueue.offer(msg);
        }
    }


}
