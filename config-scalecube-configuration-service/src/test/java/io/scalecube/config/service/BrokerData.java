package io.scalecube.config.service;

import java.util.Arrays;
import java.util.Objects;

public class BrokerData {

  private String BrokerID;
  private ApiKey[] APIKeys;

  public BrokerData(String BrokerID, ApiKey[] APIKeys) {
    this.BrokerID = BrokerID;
    this.APIKeys = APIKeys;
  }

  public BrokerData() {}

  /** @return the brokerID */
  public String getBrokerID() {
    return this.BrokerID;
  }

  /** @return the apiKeys */
  public ApiKey[] getApiKeys() {
    return this.APIKeys;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(this.APIKeys);
    result = prime * result + Objects.hash(BrokerID);
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
    return Arrays.equals(APIKeys, other.APIKeys) && Objects.equals(BrokerID, other.BrokerID);
  }
}
