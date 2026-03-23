package com.music.common.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${spring.redis.host:127.0.0.1}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    @Value("${spring.redis.database:0}")
    private int redisDatabase;

    /**
     * 手动创建 LettuceConnectionFactory，强制使用 IP 地址连接，
     * 避免 localhost DNS 解析失败（<unresolved>）导致 NOAUTH 错误。
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // 使用 IP 直连，跳过 DNS 解析
        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration();
        serverConfig.setHostName(redisHost);
        serverConfig.setPort(redisPort);
        serverConfig.setDatabase(redisDatabase);
        if (redisPassword != null && !redisPassword.isEmpty()) {
            serverConfig.setPassword(redisPassword);
        }

        // 连接池配置
        GenericObjectPoolConfig<Object> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(8);
        poolConfig.setMaxIdle(8);
        poolConfig.setMinIdle(2);
        poolConfig.setMaxWait(Duration.ofMillis(3000));
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(60));
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestWhileIdle(true);

        // Socket 超时配置
        SocketOptions socketOptions = SocketOptions.builder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(socketOptions)
                .build();

        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .poolConfig(poolConfig)
                .clientOptions(clientOptions)
                .commandTimeout(Duration.ofSeconds(5))
                .shutdownTimeout(Duration.ofMillis(200))
                .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(serverConfig, clientConfig);
        factory.afterPropertiesSet();
        return factory;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = cacheJsonSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    private GenericJackson2JsonRedisSerializer cacheJsonSerializer() {
        // 与历史缓存保持一致：使用 @class 属性写入类型信息（避免 WRAPPER_ARRAY/PROPERTY 混用导致反序列化失败）
        return new GenericJackson2JsonRedisSerializer("@class");
    }

    @Bean
    @Override
    public CacheManager cacheManager() {
        RedisConnectionFactory connectionFactory = redisConnectionFactory();
        RedisSerializationContext.SerializationPair<String> keySerializationPair =
                RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.string());
        RedisSerializationContext.SerializationPair<Object> valueSerializationPair =
                RedisSerializationContext.SerializationPair.fromSerializer(cacheJsonSerializer());

        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                // 增加版本前缀，避免读取历史序列化格式不兼容的缓存
                .computePrefixWith(cacheName -> "v2:" + cacheName + "::")
                .serializeKeysWith(keySerializationPair)
                .serializeValuesWith(valueSerializationPair)
                .disableCachingNullValues()
                .entryTtl(Duration.ofMinutes(10));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .transactionAware()
                .build();
    }

    /**
     * 缓存读取异常降级：当历史脏缓存反序列化失败时，记录日志并回源数据库，避免接口直接 500。
     */
    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis cache get error, cacheName={}, key={}, fallback to source. reason={}",
                        cache != null ? cache.getName() : "unknown", key, exception.getMessage());
                // 吞掉异常，允许业务继续执行
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Redis cache put error, cacheName={}, key={}, skip caching. reason={}",
                        cache != null ? cache.getName() : "unknown", key, exception.getMessage());
                // 吞掉异常，避免因缓存不可用影响主流程
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis cache evict error, cacheName={}, key={}, reason={}",
                        cache != null ? cache.getName() : "unknown", key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Redis cache clear error, cacheName={}, reason={}",
                        cache != null ? cache.getName() : "unknown", exception.getMessage());
            }
        };
    }
}
