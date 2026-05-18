package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;

import java.util.List;

public class PnRestartException extends PnRuntimeException {

  public PnRestartException(String message, String errorCode, int status) {
    super(message, errorCode, status, List.of(ProblemError.builder().code(errorCode).detail(message).build()));
  }
}
