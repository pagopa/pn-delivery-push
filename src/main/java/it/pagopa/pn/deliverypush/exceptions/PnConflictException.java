package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;

import java.util.List;

public class PnConflictException extends PnRuntimeException {

  public PnConflictException(String errorCode, String message) {
    super(message, errorCode, 409, List.of(ProblemError.builder().code(errorCode).detail(message).build()));
  }
}
