package org.cmxyzx.push.message;

import org.cmxyzx.push.heartbeat.ServerMessage;
import org.cmxyzx.push.util.LRUCacheMap;
import org.cmxyzx.push.util.PropertiesUtil;
import org.cmxyzx.push.util.TextUtil;

public class MessagePool {
    private static MessagePool mPoolInstance = new MessagePool();
    private LRUCacheMap<String, ServerMessage> mPool;

    private MessagePool() {
        mPool = new LRUCacheMap<>(PropertiesUtil.getInstance().getMessagePoolCapacity());
    }

    public static MessagePool getInstance() {
        return mPoolInstance;
    }

    public ServerMessage getMsg(String uuid) {
        ServerMessage msg = null;
        if (TextUtil.checkString(uuid)) {
            msg = mPool.get(uuid);
        }
        return msg;
    }

    public void putMsg(ServerMessage msg) {
        if (msg != null && TextUtil.checkString(msg.getMsg().getUUID()))
            mPool.put(msg.getMsg().getUUID(), msg);
    }

    public void removeUUID(ServerMessage msg) {
        if (msg != null)
            removeUUID(msg.getMsg().getUUID());
    }

    public void removeUUID(String UUID) {
        if (TextUtil.checkString(UUID)) {
            mPool.remove(UUID);
        }
    }


}
