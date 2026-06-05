package com.bhagwat.scm.api;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.*;

public class RedisClientImpl implements RedisClient {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisClientImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /* String */

    @Override
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    @Override
    public void set(String key, Object value, Duration timeout) {
        redisTemplate.opsForValue().set(key, value, timeout);
    }

    @Override
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public boolean hasKey(String key) {
        Boolean result = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public void delete(Collection<String> keys) {
        redisTemplate.delete(keys);
    }


    /* Hash */

    @Override
    public void hset(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    @Override
    public Object hget(String key, String hashKey) {
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    @Override
    public Map<Object, Object> hgetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    @Override
    public void hdelete(String key, Object... hashKeys) {
        redisTemplate.opsForHash().delete(key, hashKeys);
    }


    /* List */

    @Override
    public void lpush(String key, Object value) {
        redisTemplate.opsForList().leftPush(key, value);
    }

    @Override
    public void rpush(String key, Object value) {
        redisTemplate.opsForList().rightPush(key, value);
    }

    @Override
    public List<Object> lrange(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    @Override
    public Object lpop(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    @Override
    public Object rpop(String key) {
        return redisTemplate.opsForList().rightPop(key);
    }


    /* Set */

    @Override
    public void sadd(String key, Object... values) {
        redisTemplate.opsForSet().add(key, values);
    }

    @Override
    public Set<Object> smembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    @Override
    public boolean sismember(String key, Object value) {
        Boolean result = redisTemplate.opsForSet().isMember(key, value);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public void sremove(String key, Object... values) {
        redisTemplate.opsForSet().remove(key, values);
    }


    /* Sorted Set */

    @Override
    public void zadd(String key, Object value, double score) {
        redisTemplate.opsForZSet().add(key, value, score);
    }

    @Override
    public Set<Object> zrange(String key, long start, long end) {
        return redisTemplate.opsForZSet().range(key, start, end);
    }

    @Override
    public Set<Object> zrangeByScore(String key, double min, double max) {
        return redisTemplate.opsForZSet().rangeByScore(key, min, max);
    }

    @Override
    public void zremove(String key, Object... values) {
        redisTemplate.opsForZSet().remove(key, values);
    }
}