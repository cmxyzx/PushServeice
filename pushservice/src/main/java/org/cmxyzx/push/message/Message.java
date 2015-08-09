package org.cmxyzx.push.message;

import java.util.Arrays;
import java.util.UUID;

public class Message {
    public Message(byte[] data) {
        this.data = Arrays.copyOf(data, data.length);
    }

    private byte[] data;

    public int getVersion() {
        return (data[0] & 0xFF);
    }

    public int getAppCode() {
        return (data[1] & 0xFF);
    }

    public String getUUID() {
        return new String(Arrays.copyOfRange(data, 2, 38));
    }

    public int getCommand() {
        return ((data[38] & 0xFF) << 8) | (data[39] & 0xFF);
    }

    public int getPayloadLength() {
        return ((data[40] & 0xFF) << 8) | (data[41] & 0xFF);
    }

    public byte[] getPayload() {
        return Arrays.copyOfRange(data, 42, 42 + getPayloadLength());
    }

    public byte[] getData() {
        return data;
    }

    public void setVersion(int version) {
        data[0] = (byte) (version & 0xFF);
    }

    public void setAppCode(int appCode) {
        data[1] = (byte) (appCode & 0xFF);
    }

    public void setCommand(int command) {
        data[38] = (byte) ((command & 0xFF00) >> 8);//command high
        data[39] = (byte) (command & 0xFF);// command low
    }

    public static byte[] createHeatBeatMsg(String uuid){
        int command = MessageState.CMD_HEARTBEAT_CLIENT;
        int payloadLength = 0;
        byte[] heartbeat = new byte[42];
        heartbeat[0] = (byte) 0x1;// version
        heartbeat[1] = (byte) 0x0;// AppCode
        System.arraycopy(uuid.getBytes(), 0, heartbeat, 2, 36);//uuid
        heartbeat[38] = (byte) ((command & 0xFF00) >> 8);//command high
        heartbeat[39] = (byte) (command & 0xFF);// command low
        heartbeat[40] = (byte) ((payloadLength & 0xFF00) >> 8);// length high
        heartbeat[41] = (byte) (payloadLength & 0xFF);// length low

        return heartbeat;
    }

    private static void main(String[] args) {
        UUID uuid = UUID.randomUUID();
        System.out.println("UUID length:" + uuid.toString().length());
        byte[] uuidbytes = uuid.toString().getBytes();
        System.out.println("uuid:" + new String(uuidbytes) + " length:" + uuidbytes.length);
        byte[] payloadbytes = "Title:您有新的消息阿斯达克姐夫卡就是的返利卡回来卡的很疯狂手阿红是会计法很快就|Content:哈哈哈发送了一条消息按考试分开就阿阿阿文化交流空间阿黑哥斯达克法兰克里海底捞|Subtext:啦啦啦啦啦啦啦啦啦啦奥斯奥斯卡的缴费和垃圾的卡的机会发了空间大八嘎老师都好了|uri:http:255.255.255.255:99999/xxxx/xx/xxxx/xxxxx/xxx/xx.html".getBytes();
        System.out.println("payload:" + new String(payloadbytes) + " length:" + payloadbytes.length);
        int command = 1024;
        int payloadLength = payloadbytes.length;

        byte[] test = new byte[42 + payloadLength];
        test[0] = (byte) 0xac;// version
        test[1] = (byte) 0xbf;// AppCode
        System.arraycopy(uuidbytes, 0, test, 2, 36);//uuid
        test[38] = (byte) ((command & 0xFF00) >> 8);//command high
        test[39] = (byte) (command & 0xFF);// command low
        test[40] = (byte) ((payloadLength & 0xFF00) >> 8);// length high
        test[41] = (byte) (payloadLength & 0xFF);// length low
        System.arraycopy(payloadbytes, 0, test, 42, payloadLength);//paylload
        //System.out.println(Arrays.toString(test));

        Message msg = new Message(test);
        System.out.println("version:" + msg.getVersion());
        System.out.println("AppCode:" + msg.getAppCode());
        System.out.println("UUID:" + msg.getUUID());
        System.out.println("Command:" + msg.getCommand());
        System.out.println("PayloadLength:" + msg.getPayloadLength());
        System.out.println("Payload:" + new String(msg.getPayload()));

        command = MessageState.CMD_READ_MSG_CLIENT;
        payloadLength = 0;
        byte[] test1 = new byte[42];
        test1[0] = (byte) 0xac;// version
        test1[1] = (byte) 0xbf;// AppCode
        System.arraycopy(uuidbytes, 0, test1, 2, 36);//uuid
        test1[38] = (byte) ((command & 0xFF00) >> 8);//command high
        test1[39] = (byte) (command & 0xFF);// command low
        test1[40] = (byte) ((payloadLength & 0xFF00) >> 8);// length high
        test1[41] = (byte) (payloadLength & 0xFF);// length low
        System.arraycopy(payloadbytes, 0, test, 42, payloadLength);//paylload

        Message msg1 = new Message(test1);
        System.out.println("version:" + msg1.getVersion());
        System.out.println("AppCode:" + msg1.getAppCode());
        System.out.println("UUID:" + msg1.getUUID());
        System.out.println("Command:" + msg1.getCommand());
        System.out.println("PayloadLength:" + msg1.getPayloadLength());
        System.out.println("Payload:" + new String(msg1.getPayload()));
    }

}
