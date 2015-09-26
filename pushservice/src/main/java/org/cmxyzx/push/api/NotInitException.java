package org.cmxyzx.push.api;

/**
 * Created by Anthony on 15/9/26.
 * Exception when client is used before init
 */
public class NotInitException extends Exception {
    public NotInitException(String str) {
        super(str);
    }
}
