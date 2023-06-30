package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.timeline.NotificationRefusedErrorInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class RequestRefusedDetailsIntTest {

    private RequestRefusedDetailsInt request;

    @BeforeEach
    public void setup() {
        //List<String> errors = new ArrayList<>();
        //errors.add(PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt.FILE_NOTFOUND.getValue());

        List<NotificationRefusedErrorInt> errors = new ArrayList<>();
        NotificationRefusedErrorInt notificationRefusedError = NotificationRefusedErrorInt.builder()
                .errorCode("FILE_NOTFOUND")
                .detail("details")
                .build();
        errors.add(notificationRefusedError);

        request = RequestRefusedDetailsInt.builder()
                .refusalReasons(errors)
                .build();
    }

    @Test
    void toLog() {
        String log = request.toLog();
        Assertions.assertEquals("errors=[NotificationRefusedErrorInt(errorCode=FILE_NOTFOUND, detail=details)]", log);
    }

    @Test
    void getErrors() {
        //List<String> actualErrors = request.getErrors();
        List<NotificationRefusedErrorInt> actualErrors = request.getRefusalReasons();
        Assertions.assertEquals(1, actualErrors.size());
    }

}