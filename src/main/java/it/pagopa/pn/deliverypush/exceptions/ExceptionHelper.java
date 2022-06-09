package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.Problem;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

@Slf4j
public class ExceptionHelper {

    public static final String MDC_TRACE_ID_KEY = "trace_id";

    private ExceptionHelper(){}


    public static HttpStatus getHttpStatusFromException(Throwable ex){
        if (ex instanceof PnInternalException)
        {
            return HttpStatus.BAD_REQUEST;
        }
        else
            return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public static Problem handleException(Throwable ex, HttpStatus statusError){
        // gestione exception e generazione fault
        Problem res = new Problem();
        res.setStatus(statusError.value());
        try {
            res.setTraceId(MDC.get(MDC_TRACE_ID_KEY));
        } catch (Exception e) {
            log.warn("Cannot get traceid", e);
        }

        if (ex instanceof PnInternalException)
        {
            res.setTitle(ex.getMessage());
            res.setDetail(ex.getMessage());
            log.warn("pn-exception catched", ex);

        }
        else
        {
            // nascondo all'utente l'errore
            res.title("Errore generico");
            res.detail("Qualcosa è andato storto, ritenta più tardi");
            log.error("exception catched", ex);
        }

        return res;
    }
}
