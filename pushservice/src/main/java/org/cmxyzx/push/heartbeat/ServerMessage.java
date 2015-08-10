package org.cmxyzx.push.heartbeat;

import org.cmxyzx.push.message.Message;

/**
 * Created by Anthony on 2015/8/10.
 * ServerMessage VO to indicate key time event with corresponding Message
 */
public class ServerMessage {
    private Message msg;
    private long lastHeartBeat;
    private long lastReadMsg;
    private long lastPushMsg;
    private long lastProvideMsg;

    public ServerMessage(Message msg, long lastHeartBeat, long lastReadMsg, long lastPushMsg, long lastProvideMsg) {
        this.msg = msg;
        this.lastHeartBeat = lastHeartBeat;
        this.lastReadMsg = lastReadMsg;
        this.lastPushMsg = lastPushMsg;
        this.lastProvideMsg = lastProvideMsg;
    }

    public Message getMsg() {
        return msg;
    }

    public void setMsg(Message msg) {
        this.msg = msg;
    }

    public long getLastHeartBeat() {
        return lastHeartBeat;
    }

    public void setLastHeartBeat(long lastHeartBeat) {
        this.lastHeartBeat = lastHeartBeat;
    }

    public long getLastReadMsg() {
        return lastReadMsg;
    }

    public void setLastReadMsg(long lastReadMsg) {
        this.lastReadMsg = lastReadMsg;
    }

    public long getLastPushMsg() {
        return lastPushMsg;
    }

    public void setLastPushMsg(long lastPushMsg) {
        this.lastPushMsg = lastPushMsg;
    }

    public long getLastProvideMsg() {
        return lastProvideMsg;
    }

    public void setLastProvideMsg(long lastProvideMsg) {
        this.lastProvideMsg = lastProvideMsg;
    }

    private boolean haveUnreadMsg() {
        return false;
    }

}
