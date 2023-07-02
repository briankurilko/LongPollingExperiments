package com.skillshare.demo.config;

import com.skillshare.demo.CustomExceptionHandler;
import com.skillshare.demo.SkillShareServiceAtomicReferenceImpl;
import com.skillshare.demo.api.SkillShareService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SkillShareConfig {

    @Bean
    public SkillShareService skillShareService() {
        return new SkillShareServiceAtomicReferenceImpl();
    }

    @Bean
    public CustomExceptionHandler globalExceptionHandler() {
        return new CustomExceptionHandler();
    }
}
