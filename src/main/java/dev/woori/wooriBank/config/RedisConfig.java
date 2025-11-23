package dev.woori.wooriBank.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import dev.woori.wooriBank.domain.auth.dto.AuthSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator.instance;

@Configuration
@EnableCaching
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Bean
    public RedisConnectionFactory redisConnectionFactory(){
        return new LettuceConnectionFactory(host, port);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        // 형식 설정: key = String, value = object
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // 직렬화(객체->json) 시 클래스 타입 정보를 JSON에 포함시키도록 설정 => 안전하게 역직렬화(json->객체) 가능
        // NON_FINAL: final이 아닌 모든 클래스에 대해 타입 정보 포함
        // JsonTypeInfo.As.PROPERTY: JSON에 타입 정보 삽입
        ObjectMapper objectMapper = new ObjectMapper();
        var ptv = BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(AuthSession.class) // AuthSession 클래스에 대해서만 역직렬화 허용
                                .build();
        objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        template.setConnectionFactory(redisConnectionFactory());

        // Key: 문자열
        template.setKeySerializer(new StringRedisSerializer());

        // Value: JSON 형태로 저장
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));

        return template;
    }
}
