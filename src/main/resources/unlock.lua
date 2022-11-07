-- 锁的key
-- local key = KEYS[1] --"lock:order:5"
-- 获取线程标识
-- local threadId = ARGV[1] --"efasdfsdf-33"
-- 获取锁中的线程标识
-- local id = redis.call('GET', KEYS[1])

-- 比较线程标识 与锁中标识是否一致
if (redis.call('GET', KEYS[1]) == ARGV[1]) then
    return redis.call('DEL', KEYS[1])
end
return 0