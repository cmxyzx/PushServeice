package org.cmxyzx.push.message;

import org.cmxyzx.push.util.LRUCacheMap;
import org.cmxyzx.push.util.PropertiesUtil;
import org.cmxyzx.push.util.TextUtil;

public class MessagePool {
    private static MessagePool mPoolInstance = new MessagePool();
    private LRUCacheMap<String, Message> mPool;

    private MessagePool() {
        mPool = new LRUCacheMap<>(PropertiesUtil.getInstance().getMessagePoolCapacity());
    }

    public static MessagePool getInstance() {
        return mPoolInstance;
    }

    public Message getMsg(String uuid) {
        Message msg = null;
        if (TextUtil.checkString(uuid)) {
            msg = mPool.get(uuid);
        }
        return msg;
    }

    public void putMsg(Message msg) {
        if (msg != null && TextUtil.checkString(msg.getUUID()))
            mPool.put(msg.getUUID(), msg);
    }

    public void removeUUID(Message msg) {
        if (msg != null)
            removeUUID(msg.getUUID());
    }

    public void removeUUID(String UUID) {
        if (TextUtil.checkString(UUID)) {
            mPool.remove(UUID);
        }
    }


}
