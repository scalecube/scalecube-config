package io.scalecube.config.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.Arrays;

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
    builder.append("BrokerData [brokerID=");
    builder.append(this.brokerID);
    builder.append(", apiKeys=");
    builder.append(Arrays.toString(this.apiKeys));
    builder.append("]");
    return builder.toString();
  }
  
}
