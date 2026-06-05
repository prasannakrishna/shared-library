package com.bhagwat.scm.api;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RedisClient {

    /* String Operations */

    void set(String key, Object value);

    void set(String key, Object value, Duration timeout);

    Object get(String key);

    boolean hasKey(String key);

    void delete(String key);

    void delete(Collection<String> keys);


    /* Hash Operations */

    void hset(String key, String hashKey, Object value);

    Object hget(String key, String hashKey);

    Map<Object, Object> hgetAll(String key);

    void hdelete(String key, Object... hashKeys);


    /* List Operations */

    void lpush(String key, Object value);

    void rpush(String key, Object value);

    List<Object> lrange(String key, long start, long end);

    Object lpop(String key);

    Object rpop(String key);


    /* Set Operations */

    void sadd(String key, Object... values);

    Set<Object> smembers(String key);

    boolean sismember(String key, Object value);

    void sremove(String key, Object... values);


    /* Sorted Set Operations */

    void zadd(String key, Object value, double score);

    Set<Object> zrange(String key, long start, long end);

    Set<Object> zrangeByScore(String key, double min, double max);

    void zremove(String key, Object... values);
}
