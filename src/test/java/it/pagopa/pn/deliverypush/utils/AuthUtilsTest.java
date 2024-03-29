package it.pagopa.pn.deliverypush.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.mandate.MandateDtoInt;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry.PnExternalRegistryClient;
import it.pagopa.pn.deliverypush.service.MandateService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.util.Base64Utils;
import reactor.core.publisher.Mono;

class AuthUtilsTest {

    private MandateService mandateService;
    private PnExternalRegistryClient externalRegistryClient;

    private AuthUtils authUtils;

    @BeforeEach
    void setup() {
        mandateService = Mockito.mock(MandateService.class);
        externalRegistryClient = Mockito.mock(PnExternalRegistryClient.class);
        authUtils = new AuthUtils(mandateService,externalRegistryClient);
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
        assertDoesNotThrow(() -> authUtils.checkUserPaAndMandateAuthorization(notification, taxIdAnon, null, CxTypeAuthFleet.PF, null));
    }

    @Test
    void checkValidAuthorizationForRecipientPG() {
        String taxId = "taxId";
        String taxIdAnon = Base64Utils.encodeToString(taxId.getBytes());
        NotificationInt notification = getNotification("iun", taxId, taxIdAnon, "paId", Instant.now());
        assertDoesNotThrow(() -> authUtils.checkUserPaAndMandateAuthorization(notification, taxIdAnon, null, CxTypeAuthFleet.PG, null));
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
        assertThrows(PnNotFoundException.class, () ->
                authUtils.checkUserPaAndMandateAuthorization(notification, senderTaxId, null, CxTypeAuthFleet.PF, null)
        );
    }

    @Test
    void checkNotValidAuthorizationForRecipientPG() {
        String taxId = "taxId";
        String taxIdAnon = Base64Utils.encodeToString(taxId.getBytes());
        NotificationInt notification = getNotification("iun", taxId, taxIdAnon, "paId", Instant.now());
        List<String> groups = List.of("G1");
        assertThrows(PnNotFoundException.class, () -> authUtils.checkUserPaAndMandateAuthorization(notification, taxIdAnon, null, CxTypeAuthFleet.PG, groups));
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
        assertDoesNotThrow(() -> authUtils.checkUserPaAndMandateAuthorization(notification, paId01, null, CxTypeAuthFleet.PF, null));
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
        assertThrows(PnNotFoundException.class, () ->
                authUtils.checkUserPaAndMandateAuthorization(notification, senderPaId01, null, CxTypeAuthFleet.PF, null)
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
                .visibilityIds(Collections.singletonList(paId01))
                .build();

        when(mandateService.listMandatesByDelegate(anyString(), anyString(), any(), any()))
                .thenReturn(List.of(mandate));

        when(externalRegistryClient.getRootSenderId(anyString())).thenReturn("paId01");

        //WHEN
        assertDoesNotThrow(() -> authUtils.checkUserPaAndMandateAuthorization(notification, taxIdAnon, mandateId, CxTypeAuthFleet.PF, null));
    }

    @Test
    void checkValidAuthorizationWithVisibilityIdNotRootForDelegate() {
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
                .visibilityIds(Collections.singletonList(paId01))
                .build();

        when(mandateService.listMandatesByDelegate(anyString(), anyString(), any(), any()))
                .thenReturn(List.of(mandate));

        when(externalRegistryClient.getRootSenderId(anyString())).thenReturn("paId00");

        //WHEN
        assertThrows(PnNotFoundException.class, () -> authUtils.checkUserPaAndMandateAuthorization(notification, taxIdAnon, mandateId, CxTypeAuthFleet.PF, null));
    }

    @Test
    void checkValidAuthorizationForDelegatePG() {
        String taxId = "taxId";
        String taxIdAnon = Base64Utils.encodeToString(taxId.getBytes());
        Instant sentAt = Instant.now();
        NotificationInt notification = getNotification("iun", taxId, taxIdAnon, "paId", sentAt);

        String mandateId = "mandateId";
        MandateDtoInt mandate = MandateDtoInt.builder()
                .mandateId(mandateId)
                .delegate("delegate")
                .delegator(taxId)
                .dateFrom(sentAt.minus(2, ChronoUnit.DAYS))
                .dateTo(sentAt.plus(2, ChronoUnit.DAYS))
                .visibilityIds(Collections.emptyList())
                .build();

        when(mandateService.listMandatesByDelegate(taxIdAnon, mandateId, CxTypeAuthFleet.PG, null))
                .thenReturn(List.of(mandate));

        assertDoesNotThrow(() -> authUtils.checkUserPaAndMandateAuthorization(notification, taxIdAnon, mandateId, CxTypeAuthFleet.PG, null));
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
                .visibilityIds(Collections.singletonList(paId01))
                .build();

        when(mandateService.listMandatesByDelegate(anyString(), anyString(), any(), any()))
                .thenReturn(List.of(mandate));

        //WHEN
        assertThrows(PnNotFoundException.class, () ->
                authUtils.checkUserPaAndMandateAuthorization(notification, taxIdAnon, mandateId, CxTypeAuthFleet.PF, null)
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

        when(mandateService.listMandatesByDelegate(anyString(), anyString(), any(), any()))
                .thenReturn(Collections.emptyList());

        //WHEN
        assertThrows(PnNotFoundException.class, () ->
                authUtils.checkUserPaAndMandateAuthorization(notification, taxIdAnon, mandateId, CxTypeAuthFleet.PF, null)
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
                .visibilityIds(Collections.singletonList(paId02))
                .build();

        when(mandateService.listMandatesByDelegate(anyString(), anyString(), any(), any()))
                .thenReturn(List.of(mandate));

        //WHEN
        assertThrows(PnNotFoundException.class, () ->
                authUtils.checkUserPaAndMandateAuthorization(notification, taxIdAnon, mandateId, CxTypeAuthFleet.PF, null)
        );
    }

    @Test
    void checkPaIdAndGroup() {
        //GIVEN
        String iun = "iun";
        String taxId = "testTaxId";
        String taxIdAnon = Base64Utils.encodeToString(taxId.getBytes());
        String paId = "paId01";

        Instant sentAt = Instant.now();

        NotificationInt notification = getNotification(iun, taxId, taxIdAnon, paId, sentAt);

        //WHEN
        assertDoesNotThrow(() ->
                authUtils.checkPaIdAndGroup(notification, paId, CxTypeAuthFleet.PA, new ArrayList<>()).block()
        );
    }

    @Test
    void checkPaIdAndGroupErrorCxType() {
        //GIVEN
        String iun = "iun";
        String taxId = "testTaxId";
        String taxIdAnon = Base64Utils.encodeToString(taxId.getBytes());
        String paId = "paId01";

        Instant sentAt = Instant.now();

        NotificationInt notification = getNotification(iun, taxId, taxIdAnon, paId, sentAt);
        
        Mono<Void> monoResp =  authUtils.checkPaIdAndGroup(notification, paId, CxTypeAuthFleet.PG, new ArrayList<>());
        
        //WHEN
        assertThrows(PnNotFoundException.class, monoResp::block);
    }

    @Test
    void checkPaIdAndGroupErrorGroup() {
        //GIVEN
        String iun = "iun";
        String taxId = "testTaxId";
        String taxIdAnon = Base64Utils.encodeToString(taxId.getBytes());
        String paId = "paId01";

        Instant sentAt = Instant.now();

        NotificationInt notification = getNotification(iun, taxId, taxIdAnon, paId, sentAt);

        List<String> groups = new ArrayList<>();
        groups.add("noway");
        Mono<Void> monoResp =  authUtils.checkPaIdAndGroup(notification, paId, CxTypeAuthFleet.PA, groups);

        //WHEN
        assertThrows(PnNotFoundException.class, monoResp::block);
    }
    @Test
    void checkPaIdAndGroupOKGroup() {
        //GIVEN
        String iun = "iun";
        String taxId = "testTaxId";
        String taxIdAnon = Base64Utils.encodeToString(taxId.getBytes());
        String paId = "paId01";

        Instant sentAt = Instant.now();

        NotificationInt notification = getNotification(iun, taxId, taxIdAnon, paId, sentAt);

        List<String> groups = new ArrayList<>();
        groups.add("group1");
        groups.add("group2");

        //WHEN
        assertDoesNotThrow(() -> authUtils.checkPaIdAndGroup(notification, paId, CxTypeAuthFleet.PA, groups));
    }

    @Test
    void checkPaIdAndGroupErrorPaId() {
        //GIVEN
        String iun = "iun";
        String taxId = "testTaxId";
        String taxIdAnon = Base64Utils.encodeToString(taxId.getBytes());
        String paId = "paId01";

        Instant sentAt = Instant.now();

        NotificationInt notification = getNotification(iun, taxId, taxIdAnon, paId, sentAt);

        Mono<Void> monoResp = authUtils.checkPaIdAndGroup(notification, "anotherPaId", CxTypeAuthFleet.PA, new ArrayList<>());
        
        //WHEN
        assertThrows(PnNotFoundException.class, monoResp::block);
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
                .withGroup("group1")
                .withNotificationRecipient(recipient)
                .build();
    }
}