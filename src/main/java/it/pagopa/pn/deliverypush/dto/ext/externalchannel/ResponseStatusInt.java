package it.pagopa.pn.deliverypush.dto.ext.externalchannel;

import lombok.Getter;

@Getter
public enum ResponseStatusInt {
  OK("OK"),

  PROGRESS("PROGRESS"),

  PROGRESS_WITH_RETRY("PROGRESS_WITH_RETRY"),
  
  KO("KO");

  private final String value;

  ResponseStatusInt(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }
}

