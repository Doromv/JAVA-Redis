package com.hmdp.utils;

/**
 * @author Doromv
 * @create 2022-05-18-9:47
 */
public interface ILock {
    boolean tryLock(long timeoutSec);
    void unlock();
}
