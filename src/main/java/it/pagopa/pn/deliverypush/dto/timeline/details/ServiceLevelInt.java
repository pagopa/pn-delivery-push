package it.pagopa.pn.deliverypush.dto.timeline.details;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public enum ServiceLevelInt {
  
  SIMPLE_REGISTERED_LETTER("SIMPLE_REGISTERED_LETTER"),
  
  REGISTERED_LETTER_890("REGISTERED_LETTER_890");

  private final String value;

  ServiceLevelInt(String value) {
    this.value = value;
  }

}

