package org.cmxyzx.push.heartbeat;

import org.cmxyzx.push.util.LRUCacheMap;
import org.cmxyzx.push.util.PropertiesUtil;

import java.net.Socket;

/**
 * Created by Anthony on 2015/8/9.
 */
public class SocketPool {
    private static SocketPool mInstance = new SocketPool();
    private LRUCacheMap<String, Socket> mPool;

    private SocketPool() {
        mPool = new LRUCacheMap<>(PropertiesUtil.getInstance().getSocketPoolCapacity());
    }

    public static SocketPool getInstance() {
        return mInstance;
    }

    public void putSocket(String UUID, Socket socket) {
        mPool.put(UUID, socket);
    }

    public Socket getSocket(String UUID) {
        return mPool.get(UUID);
    }

    public void forEach(java.util.function.BiConsumer<? super String, ? super Socket> action){
        mPool.forEach(action);
    }

}
