package io.scalecube.config.service;

import java.util.List;

public class BrokerData {
  public String BrokerID;
  public List<ApiKey> APIKeys;
  
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public BrokerData() {
    // TODO Auto-generated constructor stub
  }

  public void setBrokerID(String BrokerID) {
    this.BrokerID = BrokerID;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder
        .append("BrokerData [BrokerID=")
        .append(this.BrokerID)
        .append(", APIKeys=")
        .append(this.APIKeys)
        .append("]");
    return builder.toString();
  }
}
