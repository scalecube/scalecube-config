package io.scalecube.config.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ApiKey {

  private String apiKey;
  private String[] permissions;

  @JsonCreator
  public ApiKey(
      @JsonProperty("APIKey") String apiKey, @JsonProperty("Permissions") String[] permissions) {
    this.apiKey = apiKey;
    this.permissions = permissions;
  }

  @JsonCreator
  public ApiKey() {
    this(null, null);
  }

  /** @return the aPIKey */
  @JsonGetter("APIKey")
  public String getAPIKey() {
    return this.apiKey;
  }

  /**
   * @param apiKey the aPIKey to set
   * @return
   */
  @JsonSetter("APIKey")
  public void setAPIKey(String apiKey) {
    this.apiKey = apiKey;
  }

  /** @return the permissions */
  @JsonGetter("Permissions")
  public String[] getPermissions() {
    return this.permissions;
  }

  /**
   * @param permissions the permissions to set
   * @return
   */
  @JsonSetter("Permissions")
  public void setPermissions(String[] permissions) {
    this.permissions = permissions;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(this.permissions);
    result = prime * result + Objects.hash(apiKey);
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof ApiKey)) {
      return false;
    }
    ApiKey other = (ApiKey) obj;
    return Objects.equals(apiKey, other.apiKey) && Arrays.equals(permissions, other.permissions);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append('{').append('"').append("apiKey").append('"').append(':');
    builder.append('"').append(this.apiKey).append('"');
    builder.append(',').append('"').append("permissions").append('"').append(':');
    builder.append(
        Stream.of(this.permissions)
            .map(s -> new StringJoiner("", "\"", "\"").add(s).toString())
            .collect(Collectors.joining(",", "[", "]")));

    builder.append("}");
    return builder.toString();
  }
}
