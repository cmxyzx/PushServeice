package org.cmxyzx.push.api;

import org.cmxyzx.push.message.Message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

/**
 * Created by Anthony on 2015/8/12.
 * PUSH API ServiceInstance
 */
public class ServiceInstance {
    private static ServiceInstance mInstance;
    private InetSocketAddress mAddress;

    private ServiceInstance(InetSocketAddress address) {
        mAddress = address;
    }

    public static ServiceInstance createInstance(InetSocketAddress address) {
        mInstance = new ServiceInstance(address);
        return mInstance;
    }

    public void init() {
        NIOClient client = new NIOClient(mAddress);
        try {
            client.init();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addUUID(String UUID) {

    }

    public void deleteUUID(String UUID) {
    }

    public void sendMessage(Message msg) {
    }

    public void sendMessageList(List<Message> list) {
    }

}
