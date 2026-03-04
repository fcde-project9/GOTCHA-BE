package com.gotcha._global.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gotcha.domain.shop.dto.ShopDetailResponse;
import java.util.List;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisCacheConfig {
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();

        // shop-detail: ShopDetailResponse 타입 명시로 @class 없이 안정적인 직렬화/역직렬화
        Jackson2JsonRedisSerializer<ShopDetailResponse> shopDetailSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, ShopDetailResponse.class);

        RedisCacheConfiguration shopDetailConfig = RedisCacheConfiguration
                .defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(shopDetailSerializer))
                .entryTtl(Duration.ofMinutes(30L));

        // blocked-user-ids: List<Long> 타입 명시
        JavaType listLongType = objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class);
        Jackson2JsonRedisSerializer<List<Long>> blockedUserIdsSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, listLongType);

        RedisCacheConfiguration blockedUserIdsConfig = RedisCacheConfiguration
                .defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(blockedUserIdsSerializer))
                .entryTtl(Duration.ofMinutes(10L));

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration
                .defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer))
                .entryTtl(Duration.ofMinutes(1L));

        return RedisCacheManager
                .RedisCacheManagerBuilder
                .fromConnectionFactory(redisConnectionFactory)
                .transactionAware()
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("shop-detail", shopDetailConfig)
                .withCacheConfiguration("blocked-user-ids", blockedUserIdsConfig)
                .build();
    }
}
