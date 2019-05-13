package io.scalecube.config.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BrokerData {

  private String brokerID;
  private ApiKey[] apiKeys;

  @JsonCreator
  public BrokerData(
      @JsonProperty("BrokerID") String brokerID, @JsonProperty("APIKeys") ApiKey[] apiKeys) {
    this.brokerID = brokerID;
    this.apiKeys = apiKeys;
  }

  @JsonCreator
  public BrokerData() {}

  @JsonSetter("BrokerID")
  public void setBrokerID(@JsonProperty("BrokerID") String brokerID) {
    this.brokerID = brokerID;
  }

  /** @return the brokerID */
  @JsonGetter("BrokerID")
  public String getBrokerID() {
    return this.brokerID;
  }

  /** @return the aPIKeys */
  @JsonGetter("APIKeys")
  public ApiKey[] getApikeys() {
    return this.apiKeys;
  }

  /**
   * @param apiKeys the aPIKeys to set
   * @return
   */
  @JsonSetter("APIKeys")
  public void setApikeys(@JsonProperty("APIKeys") ApiKey[] apiKeys) {
    this.apiKeys = apiKeys;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append('{').append('"').append("brokerID").append('"').append(':');
    builder.append('"').append(this.brokerID).append('"');
    builder.append(',').append('"').append("apiKeys").append('"').append(':');
    builder.append(
        Stream.of(this.apiKeys).map(ApiKey::toString).collect(Collectors.joining(",", "[", "]")));
    builder.append("}");
    return builder.toString();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(this.apiKeys);
    result = prime * result + Objects.hash(brokerID);
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
    if (!(obj instanceof BrokerData)) {
      return false;
    }
    BrokerData other = (BrokerData) obj;
    return Arrays.equals(apiKeys, other.apiKeys) && Objects.equals(brokerID, other.brokerID);
  }
}
