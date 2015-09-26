package org.cmxyzx.push.api;

import org.cmxyzx.push.message.Message;
import org.cmxyzx.push.message.MessageState;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;

/**
 * Created by Anthony on 2015/8/12.
 * PUSH API ServiceInstance
 */
public class ServiceInstance {
    private static ServiceInstance mInstance;
    private InetSocketAddress mAddress;
    private NIOClient mClient;

    private ServiceInstance(InetSocketAddress address) {
        mAddress = address;
    }

    public static ServiceInstance createInstance(InetSocketAddress address) {
        mInstance = new ServiceInstance(address);
        return mInstance;
    }

    public void init() {
        mClient = new NIOClient(mAddress);
        try {
            mClient.init();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addUUID(UUID uuid) throws IOException, InterruptedException, NotInitException {
        addUUID(uuid.toString());
    }

    public void addUUID(String UUID) throws IOException, InterruptedException, NotInitException {
        if (mClient != null) {
            Message msg = Message.createEmptyMsg(UUID, MessageState.CMD_ADD_UUID_SERVER);
            if (msg != null) {
                mClient.sendMessage(msg);
            }
        }
    }

    public void deleteUUID(UUID uuid) throws IOException, InterruptedException, NotInitException {
        deleteUUID(uuid.toString());
    }

    public void deleteUUID(String UUID) throws IOException, InterruptedException, NotInitException {
        if (mClient != null) {
            Message msg = Message.createEmptyMsg(UUID, MessageState.CMD_DEL_UUID_SERVER);
            if (msg != null) {
                mClient.sendMessage(msg);
            }
        }
    }

    public void sendMessage(Message msg) throws IOException, InterruptedException, NotInitException {
        if (mClient != null) {
            mClient.sendMessage(msg);
        }
    }

    public void sendMessageList(List<Message> list) throws IOException, InterruptedException, NotInitException {
        if (mClient != null) {
            mClient.sendMessageList(list);
        }
    }

    public void close() {
        if (mClient != null) {
            mClient.closeConnection();
        }
    }

}
