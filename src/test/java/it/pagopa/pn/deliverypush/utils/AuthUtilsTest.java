package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.mandate.MandateDtoInt;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.service.MandateService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.util.Base64Utils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AuthUtilsTest {
    private MandateService mandateService;

    private AuthUtils authUtils;

    @BeforeEach
    void setup() {
        mandateService = Mockito.mock(MandateService.class);

        authUtils = new AuthUtils(mandateService);
    }

    @Test
    void checkValidAuthorizationForRecipient() {
        //GIVEN
        String iun = "iun";
        String taxId = "testTaxId";
        String taxIdAnon = Base64Utils.encodeToString(taxId.getBytes());
        String paId01 = "paId01";
        Instant sentAt = Instant.now();

        NotificationInt notification = getNotification(iun, taxId, taxIdAnon, paId01, sentAt);

        //WHEN
        assertDoesNotThrow(() -> authUtils.checkUserPaAndMandateAuthorization(notification, taxIdAnon, null));
    }

    @Test
    void checkNotValidAuthorizationForRecipient() {
        //GIVEN
        String iun = "iun";
        String taxId = "testTaxId";
        String taxIdAnon = Base64Utils.encodeToString(taxId.getBytes());
        String senderTaxId = "SenderTxId";
        String paId01 = "paId01";
        Instant sentAt = Instant.now();

        NotificationInt notification = getNotification(iun, taxId, taxIdAnon, paId01, sentAt);

        //WHEN
        Assertions.assertThrows(PnNotFoundException.class, () ->
                authUtils.checkUserPaAndMandateAuthorization(notification, senderTaxId, null)
        );
    }

    @Test
    void checkValidAuthorizationForPa() {
        //GIVEN
        String iun = "iun";
        String taxId = "testTaxId";
        String taxIdAnon = Base64Utils.encodeToString(taxId.getBytes());
        String paId01 = "paId01";
        Instant sentAt = Instant.now();

        NotificationInt notification = getNotification(iun, taxId, taxIdAnon, paId01, sentAt);

        //WHEN
        assertDoesNotThrow(() -> authUtils.checkUserPaAndMandateAuthorization(notification, paId01, null));
    }

    @Test
    void checkNotValidAuthorizationForPa() {
        //GIVEN
        String iun = "iun";
        String taxId = "testTaxId";
        String taxIdAnon = Base64Utils.encodeToString(taxId.getBytes());
        String paId01 = "paId01";
        String senderPaId01 = "paId02";
        Instant sentAt = Instant.now();

        NotificationInt notification = getNotification(iun, taxId, taxIdAnon, paId01, sentAt);

        //WHEN
        Assertions.assertThrows(PnNotFoundException.class, () ->
                authUtils.checkUserPaAndMandateAuthorization(notification, senderPaId01, null)
        );
    }

    @Test
    void checkValidAuthorizationWithVisibilityIdForDelegate() {
        //GIVEN
        String iun = "iun";
        String taxId = "testTaxId";
        String taxIdAnon = Base64Utils.encodeToString(taxId.getBytes());
        String paId01 = "paId01";
        Instant sentAt = Instant.now();

        NotificationInt notification = getNotification(iun, taxId, taxIdAnon, paId01, sentAt);

        String mandateId = "mandateId";

        MandateDtoInt mandate = MandateDtoInt.builder()
                .mandateId(mandateId)
                .delegate("delegate")
                .delegator(taxId)
                .dateFrom(sentAt.minus(2, ChronoUnit.DAYS))
                .dateTo(sentAt.plus(2, ChronoUnit.DAYS))
                .visibilityIds(
                        Collections.singletonList(paId01)
                )
                .build();

        Mockito.when(mandateService.listMandatesByDelegate(Mockito.anyString(), Mockito.anyString())).thenReturn(List.of(mandate));

        //WHEN
        assertDoesNotThrow(() -> authUtils.checkUserPaAndMandateAuthorization(notification, taxIdAnon, mandateId));
    }

    @Test
    void checkNotValidAuthorizationStartDateForDelegate() {
        //GIVEN
        String iun = "iun";
        String taxId = "testTaxId";
        String taxIdAnon = Base64Utils.encodeToString(taxId.getBytes());
        String paId01 = "paId01";
        Instant sentAt = Instant.now();

        NotificationInt notification = getNotification(iun, taxId, taxIdAnon, paId01, sentAt);

        String mandateId = "mandateId";

        MandateDtoInt mandate = MandateDtoInt.builder()
                .mandateId(mandateId)
                .delegate("delegate")
                .delegator(taxId)
                .dateFrom(sentAt.plus(2, ChronoUnit.DAYS))
                .dateTo(Instant.now().plus(2, ChronoUnit.DAYS))
                .visibilityIds(
                        Collections.singletonList(paId01)
                )
                .build();

        Mockito.when(mandateService.listMandatesByDelegate(Mockito.anyString(), Mockito.anyString())).thenReturn(List.of(mandate));

        //WHEN
        Assertions.assertThrows(PnNotFoundException.class, () ->
                authUtils.checkUserPaAndMandateAuthorization(notification, taxIdAnon, mandateId)
        );
    }

    @Test
    void checkNotValidAuthorizationNotExistingMandateForDelegate() {
        //GIVEN
        String iun = "iun";
        String taxId = "testTaxId";
        String taxIdAnon = Base64Utils.encodeToString(taxId.getBytes());
        String paId01 = "paId01";
        Instant sentAt = Instant.now();

        NotificationInt notification = getNotification(iun, taxId, taxIdAnon, paId01, sentAt);

        String mandateId = "mandateId";

        Mockito.when(mandateService.listMandatesByDelegate(Mockito.anyString(), Mockito.anyString())).thenReturn(Collections.emptyList());

        //WHEN
        Assertions.assertThrows(PnNotFoundException.class, () ->
                authUtils.checkUserPaAndMandateAuthorization(notification, taxIdAnon, mandateId)
        );
    }

    @Test
    void checkNotValidAuthorizationVisibilityIdForDelegate() {
        //GIVEN
        String iun = "iun";
        String taxId = "testTaxId";
        String taxIdAnon = Base64Utils.encodeToString(taxId.getBytes());
        String paId01 = "paId01";
        String paId02 = "paId02";

        Instant sentAt = Instant.now();

        NotificationInt notification = getNotification(iun, taxId, taxIdAnon, paId01, sentAt);

        String mandateId = "mandateId";

        MandateDtoInt mandate = MandateDtoInt.builder()
                .mandateId(mandateId)
                .delegate("delegate")
                .delegator(taxId)
                .dateFrom(sentAt.plus(2, ChronoUnit.DAYS))
                .dateTo(Instant.now().plus(2, ChronoUnit.DAYS))
                .visibilityIds(
                        Collections.singletonList(paId02)
                )
                .build();

        Mockito.when(mandateService.listMandatesByDelegate(Mockito.anyString(), Mockito.anyString())).thenReturn(List.of(mandate));

        //WHEN
        Assertions.assertThrows(PnNotFoundException.class, () ->
                authUtils.checkUserPaAndMandateAuthorization(notification, taxIdAnon, mandateId)
        );
    }

    private NotificationInt getNotification(String iun, String taxId, String taxIdAnon, String paId, Instant sentAt) {
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withInternalId(taxIdAnon)
                .withTaxId(taxId)
                .build();

        return NotificationTestBuilder.builder()
                .withSentAt(sentAt)
                .withIun(iun)
                .withPaId(paId)
                .withNotificationRecipient(recipient)
                .build();
    }
}