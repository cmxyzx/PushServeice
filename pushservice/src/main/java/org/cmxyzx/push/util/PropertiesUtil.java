package org.cmxyzx.push.util;

import java.util.Enumeration;
import java.util.Properties;
import java.util.ResourceBundle;

public class PropertiesUtil {
    public static final String DEFAULT_CONFIG = "push_config";
    private static final String EMPTY_STRING = "";
    private static PropertiesUtil mInstance;
    private static Object mLock = new Object();
    private String mConfig;
    private Properties mProperties;


    private PropertiesUtil(String config) {
        this.mConfig = config;
        init();
    }

    public static PropertiesUtil getInstance(String config) {
        synchronized (mLock) {
            if (mInstance == null)
                mInstance = new PropertiesUtil(config);
        }
        return mInstance;
    }

    public static PropertiesUtil getInstance() {
        return getInstance(DEFAULT_CONFIG);
    }

    private void init() {
        if (mConfig != null) {
            ResourceBundle bundle = ResourceBundle.getBundle(mConfig);
            mProperties = new Properties();
            Enumeration<String> eu = bundle.getKeys();
            while (eu.hasMoreElements()) {
                String key = eu.nextElement().trim();
                String value = bundle.getString(key).trim();
                try {
                    value = new String(value.getBytes("ISO8859-1"), "UTF-8");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mProperties.put(key.toUpperCase(), value);
            }
        }
    }

    public int getIntProgerty(String key) {
        int value = 0;
        try {
            value = Integer.parseInt(getProperty(key));
        } catch (NumberFormatException e) {
            e.printStackTrace();
            // System.exit(1);
        }
        return value;
    }

    public long getLongProperty(String key) {
        long value = 0;
        try {
            value = Long.parseLong(getProperty(key));
        } catch (NumberFormatException e) {
            e.printStackTrace();
            // System.exit(1);
        }
        return value;
    }

    public boolean getBoolProperty(String key) {
        boolean value = false;
        try {
            value = Boolean.parseBoolean(getProperty(key));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    public String getProperty(String key) {
        String value = EMPTY_STRING;
        if (mProperties != null) {
            value = mProperties.getProperty(key.toUpperCase());
            if (value == null) {
                return EMPTY_STRING;
            }
        }
        return value;
    }

    public int getExecutorThreadNum() {
        return getIntProgerty("ExecutorThreadNum");
    }

    public int getMessagePoolCapacity() {
        return getIntProgerty("MessagePoolCapacity");
    }

    public int getSocketPoolCapacity() {
        return getIntProgerty("SocketPoolCapacity");
    }
}
