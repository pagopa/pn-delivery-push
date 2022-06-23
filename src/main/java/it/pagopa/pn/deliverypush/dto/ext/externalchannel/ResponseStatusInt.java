package it.pagopa.pn.deliverypush.dto.ext.externalchannel;

import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public enum ResponseStatusInt {
  OK("OK"),
  
  KO("KO");

  private final String value;

  ResponseStatusInt(String value) {
    this.value = value;
  }
}

