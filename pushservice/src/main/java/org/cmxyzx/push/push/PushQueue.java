package org.cmxyzx.push.push;

import org.cmxyzx.push.heartbeat.ServerMessage;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by Anthony on 2015/8/9.
 * PushQueue wrap the ConcurrentLinkedQueue for Message to push
 */
public class PushQueue {
    private static final int DEFAULT_QUEUE_SIZE = 512;
    private static final int DEFAULT_TIME_OUT = 10;
    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;
    private static final Object mLock = new Object();
    private static PushQueue mInstance = new PushQueue();

    private final LinkedBlockingQueue<ServerMessage> mMsgQueue;

    private PushQueue() {
        this(DEFAULT_QUEUE_SIZE);
    }

    private PushQueue(int capacity) {
        mMsgQueue = new LinkedBlockingQueue<>(capacity);
    }

    public static PushQueue getInstance() {
        return mInstance;
    }

    public ServerMessage deQueueMsg() throws InterruptedException {
        return deQueueMsg(DEFAULT_TIME_OUT, DEFAULT_TIME_UNIT);
    }

    public ServerMessage deQueueMsg(long timeout, TimeUnit unit) throws InterruptedException {
        ServerMessage msg;
        if (unit == null)
            unit = DEFAULT_TIME_UNIT;
        synchronized (mLock) {
            msg = mMsgQueue.poll(timeout, unit);
        }
        return msg;
    }

    public boolean enQueueMsg(ServerMessage msg) throws InterruptedException {
        return enQueueMsg(msg, DEFAULT_TIME_OUT, DEFAULT_TIME_UNIT);
    }

    public boolean enQueueMsg(ServerMessage msg, long timeout, TimeUnit unit) throws InterruptedException {
        boolean ret = false;
        if (msg != null) {
            if (unit == null)
                unit = DEFAULT_TIME_UNIT;
            synchronized (mLock) {
                ret = mMsgQueue.offer(msg, timeout, unit);
            }
        }
        return ret;
    }


}
