package com.codename1.server.mcp.config;

import com.codename1.server.mcp.tools.GlobalExtractor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(Cn1Config.Cn1Props.class)
public class Cn1Config {

    @Bean
    public GlobalExtractor globalExtractor(Cn1Props p) {
        return new GlobalExtractor(p.cacheDir(), p.libsVersionTag());
    }

    @ConfigurationProperties(prefix = "cn1")
    public record Cn1Props(String cacheDir, String libsVersionTag, Jdk8Props jdk8) {
        public record Jdk8Props(String resourcePath, String rootMarker) {}
    }
}