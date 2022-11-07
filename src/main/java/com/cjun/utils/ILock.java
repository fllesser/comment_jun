package com.cjun.utils;

public interface ILock {

    /**
     * 尝试获取锁
     * @param timeoutSec ex
     * @return ture代表获取锁成功
     */
    boolean tryLock(long timeoutSec);

    void unlock();

    /**
     * 使用lua脚本, 保证原子性
     */
    void unlockWithLua();

    /**
     * 使用watch监视key, 防止key被修改, 删除别的线程的锁
     */
    void unlockWithWatch();
}
