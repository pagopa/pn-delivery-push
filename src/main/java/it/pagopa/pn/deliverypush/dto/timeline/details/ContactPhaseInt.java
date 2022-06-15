package it.pagopa.pn.deliverypush.dto.timeline.details;

import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public enum ContactPhaseInt {
  
  CHOOSE_DELIVERY("CHOOSE_DELIVERY"),
  
  SEND_ATTEMPT("SEND_ATTEMPT");

  private final String value;

  ContactPhaseInt(String value) {
    this.value = value;
  }

}

