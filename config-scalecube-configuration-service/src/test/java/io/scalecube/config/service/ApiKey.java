package io.scalecube.config.service;

import java.util.List;

public class ApiKey {
  public String APIKey;
  public List<String> Permissions;
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder
        .append("ApiKey [APIKey=")
        .append(this.APIKey)
        .append(", Permissions=")
        .append(this.Permissions)
        .append("]");
    return builder.toString();
  }
}