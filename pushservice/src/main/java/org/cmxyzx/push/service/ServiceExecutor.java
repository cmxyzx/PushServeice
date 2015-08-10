package org.cmxyzx.push.service;

import org.cmxyzx.push.util.PropertiesUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ServiceExecutor {
    private static ServiceExecutor mExecutor = new ServiceExecutor();
    private ExecutorService mService;

    private ServiceExecutor() {
        PropertiesUtil properties = PropertiesUtil.getInstance();
        int threadNum = properties.getServerExecutorThreadNum();
        if (mService == null) {
            mService = Executors.newFixedThreadPool(threadNum);
        }
    }

    public static ServiceExecutor getInstance() {
        return mExecutor;
    }

    public Future<?> execute(Runnable r) {
        if (mService != null) {
            return mService.submit(r);
        }
        return null;
    }

}
