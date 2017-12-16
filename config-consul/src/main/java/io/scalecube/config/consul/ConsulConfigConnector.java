package io.scalecube.config.consul;

import com.google.common.net.HostAndPort;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.util.bookend.ConsulBookend;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.net.Proxy;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class ConsulConfigConnector {
  private final Consul consul;

  public ConsulConfigConnector(Builder builder) {
    this.consul = builder.consulBuilder.build();
  }

  public static Builder forUri(String uri) {
    return new Builder().withUrl(uri);
  }

  public KeyValueClient getClient() {
    return consul.keyValueClient();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Consul.Builder consulBuilder = Consul.builder();

    public Builder withUrl(URL url) {
      consulBuilder.withUrl(url);
      return this;
    }

    public Builder withPing(boolean ping) {
      consulBuilder.withPing(ping);
      return this;
    }

    public Builder withBasicAuth(String username, String password) {
      consulBuilder.withBasicAuth(username, password);
      return this;
    }

    public Builder withAclToken(String token) {
      consulBuilder.withAclToken(token);
      return this;
    }

    public Builder withHeaders(Map<String, String> headers) {
      consulBuilder.withHeaders(headers);
      return this;
    }

    public Builder withConsulBookend(ConsulBookend consulBookend) {
      consulBuilder.withConsulBookend(consulBookend);
      return this;
    }

    public Builder withHostAndPort(HostAndPort hostAndPort) {
      consulBuilder.withHostAndPort(hostAndPort);
      return this;
    }

    public Builder withUrl(String url) {
      consulBuilder.withUrl(url);
      return this;
    }

    public Builder withSslContext(SSLContext sslContext) {
      consulBuilder.withSslContext(sslContext);
      return this;
    }

    public Builder withHostnameVerifier(HostnameVerifier hostnameVerifier) {
      consulBuilder.withHostnameVerifier(hostnameVerifier);
      return this;
    }

    public Builder withProxy(Proxy proxy) {
      consulBuilder.withProxy(proxy);
      return this;
    }

    public Builder withConnectTimeoutMillis(long timeoutMillis) {
      consulBuilder.withConnectTimeoutMillis(timeoutMillis);
      return this;
    }

    public Builder withReadTimeoutMillis(long timeoutMillis) {
      consulBuilder.withReadTimeoutMillis(timeoutMillis);
      return this;
    }

    public Builder withWriteTimeoutMillis(long timeoutMillis) {
      consulBuilder.withWriteTimeoutMillis(timeoutMillis);
      return this;
    }

    public Builder withExecutorService(ExecutorService executorService) {
      consulBuilder.withExecutorService(executorService);
      return this;
    }

    public ConsulConfigConnector build() {
      return new ConsulConfigConnector(this);
    }
  }
}
