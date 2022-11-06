package com.cjun.utils;

public interface ILock {

    /**
     * 尝试获取锁
     * @param timeoutSec ex
     * @return ture代表获取锁成功
     */
    boolean tryLock(long timeoutSec);

    void unlock();
}
