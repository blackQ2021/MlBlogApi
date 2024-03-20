package cn.xuanq.blog;

import cn.xuanq.blog.utils.IdWorker;
import cn.xuanq.blog.utils.RedisUtil;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Random;

@Slf4j
@EnableSwagger2
@SpringBootApplication
public class BlogApplication {
    public static void main(String[] args) {
        SpringApplication.run(BlogApplication.class, args);
        log.info("run...");
    }

    @Bean
    public IdWorker createIdWorker() {
        return new IdWorker(0,0);
    }

    @Bean
    public BCryptPasswordEncoder createPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RedisUtil createRedisUtil() {
        return new RedisUtil();
    }

    @Bean
    public Random createRandom() {
        return new Random();
    }

    @Bean
    public Gson createGson() {
        return new Gson();
    }
}
