package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.timeline.NotificationRefusedErrorInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.RefusedReason;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class NotificationRefusedMapperTest {
    public static final String ERROR_CODE = "FILE_NOTFOUND";
    public static final String DETAIL = "Allegato non trovato. fileKey=81dde2a8-9719-4407-b7b3-63e7ea694869";

    @Test
    void internalToExternal() {

        NotificationRefusedErrorInt notificationRefusedErrorInt = NotificationRefusedErrorInt.builder()
                .errorCode( ERROR_CODE )
                .detail( DETAIL )
                .build();
        RefusedReason refusedReason = NotificationRefusedMapper.internalToExternal(notificationRefusedErrorInt);

        Assertions.assertEquals( ERROR_CODE, refusedReason.getErrorCode() );
        Assertions.assertEquals( DETAIL, refusedReason.getDetail() );
    }
}
