package it.pagopa.pn.deliverypush.rest.handler;

import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.Problem;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ProblemError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@ControllerAdvice
public class RestResponseEntityExceptionHandler {
    
    @ExceptionHandler({RuntimeException.class})
    public ResponseEntity<Problem> handleRuntimeException(RuntimeException ex ) {
        log.error("handleRuntimeException, ex=", ex);

        Problem problem = Problem.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .title(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .detail(ex.getMessage())
                .build();

        return ResponseEntity.internalServerError().body(problem);
    }

    @ExceptionHandler({WebExchangeBindException.class})
    public ResponseEntity<Problem> handleWebExchangeBindException(WebExchangeBindException ex ) {
        log.error("HandleWebExchangeBindException, ex=", ex);

        List<ObjectError> objectErrorList = ex.getAllErrors();
        List<ProblemError> problemErrors = new ArrayList<>();
        objectErrorList.forEach(er -> problemErrors.add(
                new ProblemError().detail(er.getCodes() != null ? er.getCodes()[0] : null)
        ));

        Problem problem = Problem.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .title(ex.getReason())
                .detail(ex.getMessage())
                .errors(problemErrors)
                .build();

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler({ServerWebInputException.class})
    public ResponseEntity<Problem> handleServerWebInputException(ServerWebInputException ex ) {
        log.error("HandleServerWebInputException, ex=", ex);
        
        Problem problem = Problem.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .title(ex.getReason())
                .detail(ex.getMessage())
                .build();

        return ResponseEntity.badRequest().body(problem);
    }
    
    @ExceptionHandler({PnNotFoundException.class})
    public ResponseEntity<Problem> handleNotFoundException(PnNotFoundException ex) {
        log.error("HandleNotFoundException, ex=", ex);

        Problem problem = Problem.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .title(ex.getTitle())
                .detail(ex.getMessage())
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }
}
