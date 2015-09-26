package org.cmxyzx.push.test;

import org.cmxyzx.push.api.NotInitException;
import org.cmxyzx.push.api.ServiceInstance;
import org.cmxyzx.push.message.Message;
import org.cmxyzx.push.message.MessageState;
import org.cmxyzx.push.util.LogUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * Created by Anthony on 15/9/26.
 * simulate app server to test push server
 */
public class ServerTest {

    public static void main(String[] args) {

        ServiceInstance service = ServiceInstance.createInstance(new InetSocketAddress("localhost", 10005));
        service.init();
        try {
            UUID uuid = UUID.randomUUID();
            service.addUUID(uuid);
            Thread.sleep(1000);
            byte[] payload = "Title:您有新的消息阿斯达克姐夫卡就是的返利卡回来卡的很疯狂手阿红是会计法很快就|Content:哈哈哈发送了一条消息按考试分开就阿阿阿文化交流空间阿黑哥斯达克法兰克里海底捞|Subtext:啦啦啦啦啦啦啦啦啦啦奥斯奥斯卡的缴费和垃圾的卡的机会发了空间大八嘎老师都好了|uri:http:255.255.255.255:99999/xxxx/xx/xxxx/xxxxx/xxx/xx.html".getBytes();
            Message msg = Message.createMsg(uuid.toString(), MessageState.CMD_SEND_MSG_SERVER, payload.length);
            if (msg != null) {
                msg.setAppCode(111);
                msg.setVersion(11);
                msg.setPayload(payload);
                service.sendMessage(msg);
            }
        } catch (IOException | InterruptedException | NotInitException e) {
            LogUtil.logE("Exception", e);
        }
        //service.close();
    }

}
