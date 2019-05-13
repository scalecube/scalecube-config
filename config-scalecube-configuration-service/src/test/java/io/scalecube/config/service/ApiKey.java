package io.scalecube.config.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.Arrays;

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
    result = prime * result + ((this.apiKey == null) ? 0 : this.apiKey.hashCode());
    result = prime * result + Arrays.hashCode(this.permissions);
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    ApiKey other = (ApiKey) obj;
    if (this.apiKey == null) {
      if (other.apiKey != null) return false;
    } else if (!this.apiKey.equals(other.apiKey)) return false;
    if (!Arrays.equals(this.permissions, other.permissions)) return false;
    return true;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("ApiKey [apiKey=");
    builder.append(this.apiKey);
    builder.append(", permissions=");
    builder.append(Arrays.toString(this.permissions));
    builder.append("]");
    return builder.toString();
  }
  
}
