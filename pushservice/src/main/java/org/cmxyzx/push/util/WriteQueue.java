package org.cmxyzx.push.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by Anthony on 15/9/26.
 * Write queue for selector to write when msg ready to write
 */
public class WriteQueue {
    private static final int DEFAULT_QUEUE_SIZE = 128;
    private static final int DEFAULT_QUEUE_TIMEOUT = 10;
    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;
    private static final Object QueueLock = new Object();

    private final LinkedBlockingQueue<ByteBuffer> mQueue;


    public WriteQueue() {
        this(DEFAULT_QUEUE_SIZE);
    }

    public WriteQueue(int capacity) {
        mQueue = new LinkedBlockingQueue<>(capacity);
    }


    public boolean enQueueBuffer(ByteBuffer buffer) throws InterruptedException {
        return enQueueBuffer(buffer, DEFAULT_QUEUE_TIMEOUT, DEFAULT_TIME_UNIT);
    }

    public boolean enQueueBuffer(ByteBuffer buffer, long timeout, TimeUnit unit) throws InterruptedException {
        boolean ret = false;
        if (buffer != null) {
            if (unit == null)
                unit = DEFAULT_TIME_UNIT;
            synchronized (QueueLock) {
                ret = mQueue.offer(buffer, timeout, unit);
            }
        }
        return ret;
    }

    public ByteBuffer deQueueBuffer() throws InterruptedException {
        // set timeout to make sure selector not stuck at some point and can not process other kind of request
        return deQueueBuffer(DEFAULT_QUEUE_TIMEOUT, DEFAULT_TIME_UNIT);
    }

    public ByteBuffer deQueueBuffer(long timeout, TimeUnit unit) throws InterruptedException {
        ByteBuffer res;
        if (unit == null) {
            unit = DEFAULT_TIME_UNIT;
        }
        synchronized (QueueLock) {
            res = mQueue.poll(timeout, unit);
        }
        return res;
    }

    public ByteBuffer deQueueAll() throws IOException {
        Iterator<ByteBuffer> iterator = mQueue.iterator();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        while (iterator.hasNext()) {
            ByteBuffer buffer = iterator.next();
            bos.write(buffer.array());
            iterator.remove();
        }
        return ByteBuffer.wrap(bos.toByteArray());
    }

}
