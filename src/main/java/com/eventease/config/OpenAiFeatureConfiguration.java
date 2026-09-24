package com.eventease.config;

import org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "spring.ai.openai.chat.enabled", havingValue = "true")
@Import(OpenAiAutoConfiguration.class)
class OpenAiFeatureConfiguration {
}