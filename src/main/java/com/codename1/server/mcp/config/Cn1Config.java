package com.codename1.server.mcp.config;

import com.codename1.server.mcp.tools.GlobalExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(Cn1Config.Cn1Props.class)
public class Cn1Config {

    private static final Logger LOG = LoggerFactory.getLogger(Cn1Config.class);

    @Bean
    public GlobalExtractor globalExtractor(Cn1Props p) {
        LOG.info("Creating GlobalExtractor with cacheDir={} versionTag={}", p.cacheDir(), p.libsVersionTag());
        return new GlobalExtractor(p.cacheDir(), p.libsVersionTag());
    }

    @ConfigurationProperties(prefix = "cn1")
    public record Cn1Props(String cacheDir, String libsVersionTag, Jdk8Props jdk8) {
        public record Jdk8Props(String linuxResourcePath, String macUrl, String windowsUrl, String rootMarker) {}
    }
}