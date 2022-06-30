package it.pagopa.pn.deliverypush.dto.timeline.details;

import lombok.Getter;

@Getter
public enum ContactPhaseInt {
  CHOOSE_DELIVERY("CHOOSE_DELIVERY"),
  
  SEND_ATTEMPT("SEND_ATTEMPT");
  
  private final String value;
  
  ContactPhaseInt(String value) {
    this.value = value;
  }
  
  @Override
  public String toString() {
    return String.valueOf(value);
  }
  
}
