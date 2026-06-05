package com.bhagwat.scm.config;

import com.bhagwat.scm.api.RedisClient;
import com.bhagwat.scm.api.RedisClientImpl;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@ConditionalOnClass(RedisTemplate.class)
public class RedisClientConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper());

        /* Key serializer */
        template.setKeySerializer(new StringRedisSerializer());

        /* Value serializer */
        template.setValueSerializer(serializer);

        /* Hash key serializer */
        template.setHashKeySerializer(new StringRedisSerializer());

        /* Hash value serializer */
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();

        return template;
    }

    @Bean
    public RedisClient redisClient(RedisTemplate<String, Object> redisTemplate) {
        return new RedisClientImpl(redisTemplate);
    }

    private ObjectMapper redisObjectMapper(){
        ObjectMapper mapper = new ObjectMapper();

        /* Support Java 8 time types (LocalDate, Instant etc) */
        mapper.registerModule(new JavaTimeModule());

        /* Enable polymorphic typing for Redis */
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        return mapper;
    }
}
