package org.cmxyzx.push.service;

import org.cmxyzx.push.util.PropertiesUtil;

import java.io.IOException;
import java.nio.channels.Selector;

/**
 * Created by Anthony on 2015/8/9.
 */
public abstract class ServiceBase implements Runnable {
    protected boolean mServiceRunning = false;
    protected Selector mSelector;

    public abstract void init(PropertiesUtil properties) throws IOException;

    public void stopService() {
        this.mServiceRunning = false;
    }
}
