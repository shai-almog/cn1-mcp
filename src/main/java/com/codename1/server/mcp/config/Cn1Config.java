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

  /**
   * Provides the shared {@link GlobalExtractor} used to hydrate Codename One tooling libraries.
   */
  @Bean
  public GlobalExtractor globalExtractor(Cn1Props props) {
    LOG.info(
        "Creating GlobalExtractor with cacheDir={} versionTag={}",
        props.cacheDir(),
        props.libsVersionTag());
    return new GlobalExtractor(props.cacheDir(), props.libsVersionTag());
  }

  /** Configuration properties backing Codename One resources. */
  @ConfigurationProperties(prefix = "cn1")
  public record Cn1Props(String cacheDir, String libsVersionTag, Jdk8Props jdk8) {

    /** JDK 8 download sources for different platforms. */
    public record Jdk8Props(
        String linuxResourcePath, String macUrl, String windowsUrl, String rootMarker) {}
  }
}
