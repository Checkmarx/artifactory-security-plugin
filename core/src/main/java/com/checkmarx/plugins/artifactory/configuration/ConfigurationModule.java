package com.checkmarx.plugins.artifactory.configuration;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static com.checkmarx.plugins.artifactory.configuration.PluginConfiguration.API_TOKEN;
import static java.lang.String.format;

public class ConfigurationModule {

  private final Properties properties;

  public ConfigurationModule(@Nonnull Properties properties) {
    this.properties = properties;
  }

  public Set<Map.Entry<Object, Object>> getPropertyEntries() {
    return new HashSet<>(properties.entrySet());
  }

  public String getProperty(Configuration config) {
    return properties.getProperty(config.propertyKey());
  }

  public String getPropertyOrDefault(Configuration config) {
    return properties.getProperty(config.propertyKey(), config.defaultValue());
  }

  public void validate() {
    final String apiToken = getProperty(PluginConfiguration.API_TOKEN);
    if (apiToken == null || apiToken.isEmpty()) {
      throw new IllegalArgumentException(String.format("'%s' must not be null or empty", PluginConfiguration.API_TOKEN.propertyKey()));
    }

  }
}
