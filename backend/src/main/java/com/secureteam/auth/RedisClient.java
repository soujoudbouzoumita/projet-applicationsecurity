package com.secureteam.auth;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@ApplicationScoped
public class RedisClient {

    private JedisPool jedisPool;

<<<<<<< HEAD
    @jakarta.inject.Inject
    @org.eclipse.microprofile.config.inject.ConfigProperty(name = "REDIS_HOST", defaultValue = "localhost")
    String host;

    @jakarta.inject.Inject
    @org.eclipse.microprofile.config.inject.ConfigProperty(name = "REDIS_PORT", defaultValue = "6379")
    int port;

=======
>>>>>>> origin/main
    @PostConstruct
    public void init() {
        try {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(128);
<<<<<<< HEAD
            this.jedisPool = new JedisPool(poolConfig, host, port);
=======
            this.jedisPool = new JedisPool(poolConfig, "127.0.0.1", 6379);
>>>>>>> origin/main
        } catch (Exception e) {
            // Log but don't crash
            java.util.logging.Logger.getLogger(RedisClient.class.getName())
                    .severe("Redis init failed: " + e.getMessage());
        }
    }

    public boolean exists(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.exists(key);
        }
    }

    public void setEx(String key, int seconds, String value) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(key, seconds, value);
        }
    }

    public String get(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get(key);
        }
    }

<<<<<<< HEAD
=======
    public void delete(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key);
        }
    }

>>>>>>> origin/main
    @PreDestroy
    public void close() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }
}
