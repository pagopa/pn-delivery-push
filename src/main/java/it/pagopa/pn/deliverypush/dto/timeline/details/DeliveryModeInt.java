package it.pagopa.pn.deliverypush.dto.timeline.details;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public enum DeliveryModeInt {
  
  DIGITAL("DIGITAL"),
  
  ANALOG("ANALOG");

  private final String value;

  DeliveryModeInt(String value) {
    this.value = value;
  }

}

