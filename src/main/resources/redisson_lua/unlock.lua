local key = KEYS[1]; -- 锁的key
local threadId = ARGV[1]; -- 线程唯一标识
local releaseTime = ARGV[2]; -- 锁的自动释放时间
if (redis.call("hexists", key, threadId) == 0) then
    return nil; -- 如果不是自己, 则说明锁已经释放,直接返回空
end
-- 是自己的锁, 重入次数-1
local count = redis.call('hincrby', key, threadId, -1);
-- 判断重入次数是否已经为0
if (count > 0) then
    redis.call('expire', key, releaseTime);
    return nil;
else
    redis.call('del', key);
    return nil;
end