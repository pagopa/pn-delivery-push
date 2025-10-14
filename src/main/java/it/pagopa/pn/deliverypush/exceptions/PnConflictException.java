package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnInternalException;

public class PnConflictException extends PnInternalException {

  public PnConflictException(String errorCode, String message) {
    super(message, 409, errorCode);
  }
}
