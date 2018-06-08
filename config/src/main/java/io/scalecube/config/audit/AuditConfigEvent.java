package io.scalecube.config.audit;

public class AuditConfigEvent {

  private final String source;
  private final String origin;
  private final String propName;
  private final String propValue;
  private final String updateDate;

  public AuditConfigEvent(ConfigEvent event) {
    if (event.getOldSource() == null && event.getNewSource() == null) {
      this.source = null;
    } else {
      this.source = event.getOldSource() + "->" + event.getNewSource();
    }

    if (event.getOldOrigin() == null && event.getNewOrigin() == null) {
      this.origin = null;
    } else {
      this.origin = event.getOldOrigin() + "->" + event.getNewOrigin();
    }

    this.propName = event.getName();

    if (event.getOldValue() != null && event.getNewValue() != null) {
      this.propValue = "***->***";
    } else if (event.getOldValue() != null) {
      this.propValue = "***->null";
    } else if (event.getNewValue() != null) {
      this.propValue = "null->***";
    } else {
      this.propValue = null;
    }

    this.updateDate = String.valueOf(event.getTimestamp().getTime());
  }

  public String getSource() {
    return source;
  }

  public String getOrigin() {
    return origin;
  }

  public String getPropName() {
    return propName;
  }

  public String getPropValue() {
    return propValue;
  }

  public String getUpdateDate() {
    return updateDate;
  }

  @Override
  public String toString() {
    return "{" +
        "source='" + source + '\'' +
        ", origin='" + origin + '\'' +
        ", propName='" + propName + '\'' +
        ", propValue='" + propValue + '\'' +
        ", updateDate='" + updateDate + '\'' +
        '}';
  }
}
