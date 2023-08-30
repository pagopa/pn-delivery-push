package it.pagopa.pn.deliverypush.dto.timeline.details;

import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationCancellationRequestDetailsIntTest {
    private NotificationCancellationRequestDetailsInt detailsInt;
    private String cancellationRequestId;

    @BeforeEach
    void setUp() {
        cancellationRequestId = UUID.randomUUID().toString();
        detailsInt = new NotificationCancellationRequestDetailsInt();
        detailsInt.setCancellationRequestId(cancellationRequestId);
    }

    @Test
    void toLog() {
        String expectedValue = String.format("cancellationRequestId=%s", this.cancellationRequestId);
        Assertions.assertEquals(expectedValue, detailsInt.toLog());
    }

}
