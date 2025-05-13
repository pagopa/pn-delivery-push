package it.pagopa.pn.deliverypush.action.it;

import it.pagopa.pn.deliverypush.action.it.mockbean.PnDeliveryClientMock;
import it.pagopa.pn.deliverypush.action.it.mockbean.SafeStorageClientMock;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action.startworkflow.StartWorkflowHandler;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.cost.RefusalCostMode;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.UsedServicesInt;
import it.pagopa.pn.deliverypush.dto.timeline.NotificationRefusedErrorInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.PublicRegistryValidationCallDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.RequestRefusedDetailsInt;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.utils.PnTechnicalRefusalCostMode;
import lombok.Builder;
import lombok.Getter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static it.pagopa.pn.deliverypush.action.it.mockbean.NationalRegistriesClientMock.PHYS_ADDR_ERROR;
import static it.pagopa.pn.deliverypush.action.it.mockbean.NationalRegistriesClientMock.PHYS_ADDR_NOT_FOUND;
import static org.awaitility.Awaitility.await;

public class NotificationLookupAddressTestIT extends CommonTestConfiguration {

    @Autowired
    StartWorkflowHandler startWorkflowHandler;
    @Autowired
    PnDeliveryClientMock pnDeliveryClientMock;
    @Autowired
    TimelineService timelineService;
    @Autowired
    SafeStorageClientMock safeStorageClientMock;
    @MockBean
    PnTechnicalRefusalCostMode pnTechnicalRefusalCostMode;

    @Test
    void testNotificationAddressLookup_MonoRecipient_AcceptedWithAddressFound() {
        /*
            Scenario 1:
            Notifica monodestinatario con lookupAddress attivo che trova il physicalAddress su National Registries.
         */
        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        notificationDocumentList = TestUtils.firstFileUploadFromNotification(listDocumentWithContent, notificationDocumentList, safeStorageClientMock);

        NotificationRecipientInt recipient = getNotificationRecipientInt("test_tax_id", null);

        UsedServicesInt usedServices = new UsedServicesInt().toBuilder()
                .physicalAddressLookUp(true)
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipients(List.of(recipient))
                .withUsedServices(usedServices)
                .build();

        String iun = notification.getIun();
        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());
        pnDeliveryClientMock.addNotification(notification);

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        checkRecIndexInNationalRegistryValidationCall(iun, List.of(recIndex));
        String expectedValidationCallTimelineId = TestUtils.buildTimelineEventIdNationalRegistryValidationCall(iun);

        await().untilAsserted(() ->
                Assertions.assertTrue(
                        TestUtils.checkIsPresentNationalRegistryValidationResponse(expectedValidationCallTimelineId, iun, recIndex, timelineService)
                )
        );

        await().untilAsserted(() ->
                Assertions.assertTrue(
                        TestUtils.checkIsPresentRequestAccepted(iun, timelineService)
                )
        );
    }

    @Test
    void testNotificationAddressLookup_MonoRecipient_RefusedForAddressNotFound() {
        /*
            Scenario 2:
            Notifica monodestinatario con lookupAddress attivo che non trova il physicalAddress su National Registries.
         */
        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        notificationDocumentList = TestUtils.firstFileUploadFromNotification(listDocumentWithContent, notificationDocumentList, safeStorageClientMock);

        NotificationRecipientInt recipient = getNotificationRecipientInt(PHYS_ADDR_NOT_FOUND, null);

        UsedServicesInt usedServices = new UsedServicesInt().toBuilder()
                .physicalAddressLookUp(true)
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipients(List.of(recipient))
                .withUsedServices(usedServices)
                .build();

        String iun = notification.getIun();
        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());
        pnDeliveryClientMock.addNotification(notification);

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        checkRecIndexInNationalRegistryValidationCall(iun, List.of(recIndex));

        await().untilAsserted(() ->
                Assertions.assertTrue(
                        TestUtils.checkIsPresentNotificationRejected(iun, timelineService)
                )
        );

        String expectedValidationCallTimelineId = TestUtils.buildTimelineEventIdNationalRegistryValidationCall(iun);

        Assertions.assertFalse(
                TestUtils.checkIsPresentNationalRegistryValidationResponse(expectedValidationCallTimelineId, iun, recIndex, timelineService)
        );

        List<RefusalReason> expectedRefusalReasons = List.of(
                RefusalReason.builder()
                        .errorCode(PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt.ADDRESS_NOT_FOUND.getValue())
                        .recIndex(recIndex)
                        .build()
        );
        verifyNotificationRejectionForLookupAddress(iun, expectedRefusalReasons);
    }

    @Test
    void testNotificationAddressLookup_MultiRecipients_RefusedWithAddressNotFoundJustForOneRecipient() {
        /*
            Scenario 3:
            Notifica con lookupAddress attivo e con 2 destinatari da ricercare sui registri.
            National Registries non trova il physicalAddress per il primo destinatario.
            Ci aspettiamo la notifica sia rifiutata e nelle refusalReasons ci sia solo il primo destinatario.
         */
        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        notificationDocumentList = TestUtils.firstFileUploadFromNotification(listDocumentWithContent, notificationDocumentList, safeStorageClientMock);

        NotificationRecipientInt recipient1 = getNotificationRecipientInt(PHYS_ADDR_NOT_FOUND, null);
        NotificationRecipientInt recipient2 = getNotificationRecipientInt("test_tax_id2", null);

        UsedServicesInt usedServices = new UsedServicesInt().toBuilder()
                .physicalAddressLookUp(true) // lookupAddress attivo per entrambi i destinatari
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipients(List.of(recipient1, recipient2))
                .withUsedServices(usedServices)
                .build();

        String iun = notification.getIun();
        Integer recIndex1 = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient1.getTaxId());
        Integer recIndex2 = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient2.getTaxId());
        pnDeliveryClientMock.addNotification(notification);

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        checkRecIndexInNationalRegistryValidationCall(iun, List.of(recIndex1, recIndex2));

        await().untilAsserted(() ->
                Assertions.assertTrue(
                        TestUtils.checkIsPresentNotificationRejected(iun, timelineService)
                )
        );

        String expectedValidationCallTimelineId = TestUtils.buildTimelineEventIdNationalRegistryValidationCall(iun);

        Assertions.assertFalse(
                TestUtils.checkIsPresentNationalRegistryValidationResponse(expectedValidationCallTimelineId, iun, recIndex1, timelineService)
        );

        Assertions.assertFalse(
                TestUtils.checkIsPresentNationalRegistryValidationResponse(expectedValidationCallTimelineId, iun, recIndex2, timelineService)
        );

        List<RefusalReason> expectedRefusalReasons = List.of(
                RefusalReason.builder()
                        .errorCode(PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt.ADDRESS_NOT_FOUND.getValue())
                        .recIndex(recIndex1)
                        .build()
        );
        verifyNotificationRejectionForLookupAddress(iun, expectedRefusalReasons);
    }

    @Test
    void testNotificationAddressLookup_MultiRecipients_AcceptedPhysicalAddressJustForOneRecipient() {
        /*
            Scenario 4:
            Notifica con lookupAddress attivo e con 2 destinatari.
            Il primo destinatario non ha il physicalAddress dichiarato, mentre il secondo destinatario si.
            National Registries trova il physicalAddress per il primo destinatario.
         */
        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        notificationDocumentList = TestUtils.firstFileUploadFromNotification(listDocumentWithContent, notificationDocumentList, safeStorageClientMock);

        String taxId1 = "TAXID_0001";
        String taxId2 = "TAXID_0002";
        NotificationRecipientInt recipient1 = getNotificationRecipientInt(taxId1, null);
        NotificationRecipientInt recipient2 = getNotificationRecipientInt(taxId2, defaultPhysicalAddress());

        UsedServicesInt usedServices = new UsedServicesInt().toBuilder()
                .physicalAddressLookUp(true) // lookupAddress attivo per entrambi i destinatari
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipients(List.of(recipient1, recipient2))
                .withUsedServices(usedServices)
                .build();

        String iun = notification.getIun();
        Integer recIndex1 = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient1.getTaxId());
        pnDeliveryClientMock.addNotification(notification);

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        checkRecIndexInNationalRegistryValidationCall(iun, List.of(recIndex1));

        String expectedValidationCallTimelineId = TestUtils.buildTimelineEventIdNationalRegistryValidationCall(iun);


        await().untilAsserted(() ->
                Assertions.assertTrue(
                        TestUtils.checkIsPresentNationalRegistryValidationResponse(expectedValidationCallTimelineId, iun, recIndex1, timelineService)
                )
        );

        await().untilAsserted(() ->
                Assertions.assertTrue(
                        TestUtils.checkIsPresentRequestAccepted(iun, timelineService)
                )
        );
    }

    @Test
    void testNotificationAddressLookup_MonoRecipient_AddressFoundWithoutLookupAddress() {
        /*
            Scenario 5:
            Notifica monodestinatario con lookupAddress attivo. La notifica dovrebbe seguire il flusso senza ricercare il physicalAddress su National Registries.
         */
        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        notificationDocumentList = TestUtils.firstFileUploadFromNotification(listDocumentWithContent, notificationDocumentList, safeStorageClientMock);

        String taxId = "test_tax_id";
        NotificationRecipientInt recipient = getNotificationRecipientInt(taxId, defaultPhysicalAddress());

        UsedServicesInt usedServices = new UsedServicesInt().toBuilder()
                .physicalAddressLookUp(false)
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipients(List.of(recipient))
                .withUsedServices(usedServices)
                .build();

        String iun = notification.getIun();
        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());
        pnDeliveryClientMock.addNotification(notification);

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        await().untilAsserted(() ->
                Assertions.assertTrue(
                        TestUtils.checkIsPresentRequestAccepted(iun, timelineService)
                )
        );

        Assertions.assertFalse(
                TestUtils.checkIsPresentNationalRegistryValidationCall(iun, timelineService)
        );

        String expectedValidationCallTimelineId = TestUtils.buildTimelineEventIdNationalRegistryValidationCall(iun);

        Assertions.assertFalse(
                TestUtils.checkIsPresentNationalRegistryValidationResponse(expectedValidationCallTimelineId, iun, recIndex, timelineService)
        );
    }

    @Test
    void testNotificationAddressLookup_MonoRecipient_RefusedForAddressSearchFailed() {
        /*
            Scenario 6:
            Notifica monodestinatario con lookupAddress attivo che riscontra errore tecnico in fase di ricerca del physicalAddress su National Registries.
         */
        Mockito.when(pnTechnicalRefusalCostMode.getMode()).thenReturn(RefusalCostMode.RECIPIENT_BASED);
        Mockito.when(pnTechnicalRefusalCostMode.getCost()).thenReturn(100);
        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        notificationDocumentList = TestUtils.firstFileUploadFromNotification(listDocumentWithContent, notificationDocumentList, safeStorageClientMock);

        NotificationRecipientInt recipient = getNotificationRecipientInt(PHYS_ADDR_ERROR, null);

        UsedServicesInt usedServices = new UsedServicesInt().toBuilder()
                .physicalAddressLookUp(true)
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipients(List.of(recipient))
                .withUsedServices(usedServices)
                .build();

        String iun = notification.getIun();
        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());
        pnDeliveryClientMock.addNotification(notification);

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        checkRecIndexInNationalRegistryValidationCall(iun, List.of(recIndex));

        await().untilAsserted(() ->
                Assertions.assertTrue(
                        TestUtils.checkIsPresentNotificationRejected(iun, timelineService)
                )
        );

        String expectedValidationCallTimelineId = TestUtils.buildTimelineEventIdNationalRegistryValidationCall(iun);

        Assertions.assertFalse(
                TestUtils.checkIsPresentNationalRegistryValidationResponse(expectedValidationCallTimelineId, iun, recIndex, timelineService)
        );

        List<RefusalReason> expectedRefusalReasons = List.of(
                RefusalReason.builder()
                        .errorCode(PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt.ADDRESS_SEARCH_FAILED.getValue())
                        .recIndex(recIndex)
                        .build()
        );
        verifyNotificationRejectionForLookupAddress(iun, expectedRefusalReasons);
    }

    @ParameterizedTest()
    @CsvSource(value = {
            "RECIPIENT_BASED, 100, 300", // Simula la configurazione di default
            "UNIFORM, 0, 0",
            "RECIPIENT_BASED, 50, 250",
    })
    void testNotificationAddressLookup_MultiRecipients_RefusedForVariousErrorsAndVerifyCost(RefusalCostMode refusalCostMode, Integer refusalCost, Integer expectedCost) {
        /*
            Scenari 7,8,9:
            Notifica con lookupAddress attivo e con 3 destinatari da ricercare sui registri.
            Primo destinatario: indirizzo trovato su National Registries.
            Secondo destinatario: indirizzo non trovato su National Registries.
            Terzo destinatario: errore tecnico in fase di ricerca su National Registries.
            Ci aspettiamo la notifica sia rifiutata e nelle refusalReasons ci siano solo il secondo e terzo destinatario.
         */
        Mockito.when(pnTechnicalRefusalCostMode.getMode()).thenReturn(refusalCostMode);
        Mockito.when(pnTechnicalRefusalCostMode.getCost()).thenReturn(refusalCost);
        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        notificationDocumentList = TestUtils.firstFileUploadFromNotification(listDocumentWithContent, notificationDocumentList, safeStorageClientMock);

        NotificationRecipientInt recipient1 = getNotificationRecipientInt("test_tax_id", null);
        NotificationRecipientInt recipient2 = getNotificationRecipientInt(PHYS_ADDR_NOT_FOUND, null);
        NotificationRecipientInt recipient3 = getNotificationRecipientInt(PHYS_ADDR_ERROR, null);

        UsedServicesInt usedServices = new UsedServicesInt().toBuilder()
                .physicalAddressLookUp(true) // lookupAddress attivo per entrambi i destinatari
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipients(List.of(recipient1, recipient2, recipient3))
                .withUsedServices(usedServices)
                .build();

        String iun = notification.getIun();
        Integer recIndex1 = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient1.getTaxId());
        Integer recIndex2 = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient2.getTaxId());
        Integer recIndex3 = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient3.getTaxId());
        pnDeliveryClientMock.addNotification(notification);

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        checkRecIndexInNationalRegistryValidationCall(iun, List.of(recIndex1, recIndex2, recIndex3));

        await().untilAsserted(() ->
                Assertions.assertTrue(
                        TestUtils.checkIsPresentNotificationRejected(iun, timelineService)
                )
        );

        String expectedValidationCallTimelineId = TestUtils.buildTimelineEventIdNationalRegistryValidationCall(iun);

        Assertions.assertFalse(
                TestUtils.checkIsPresentNationalRegistryValidationResponse(expectedValidationCallTimelineId, iun, recIndex1, timelineService)
        );

        Assertions.assertFalse(
                TestUtils.checkIsPresentNationalRegistryValidationResponse(expectedValidationCallTimelineId, iun, recIndex2, timelineService)
        );

        Assertions.assertFalse(
                TestUtils.checkIsPresentNationalRegistryValidationResponse(expectedValidationCallTimelineId, iun, recIndex3, timelineService)
        );

        List<RefusalReason> expectedRefusalReasons = List.of(
                RefusalReason.builder()
                        .errorCode(PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt.ADDRESS_NOT_FOUND.getValue())
                        .recIndex(recIndex2)
                        .build(),
                RefusalReason.builder()
                        .errorCode(PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt.ADDRESS_SEARCH_FAILED.getValue())
                        .recIndex(recIndex3)
                        .build()
        );
        verifyNotificationRejectionForLookupAddress(iun, expectedRefusalReasons, expectedCost);
    }

    private NotificationRecipientInt getNotificationRecipientInt(String taxId, PhysicalAddressInt physicalAddress) {
        return NotificationRecipientInt.builder()
                .taxId(taxId)
                .denomination("denomination")
                .physicalAddress(physicalAddress)
                .internalId("internalIdTest")
                .recipientType(RecipientTypeInt.PF)
                .build();
    }

    private PhysicalAddressInt defaultPhysicalAddress() {
        return PhysicalAddressInt.builder()
                .address("Test address")
                .at("At")
                .zip("00133")
                .municipality("Test municipality")
                .province("TS")
                .build();
    }

    @Builder
    @Getter
    private static class RefusalReason {
        private String errorCode;
        private Integer recIndex;
    }

    private void verifyNotificationRejectionForLookupAddress(String iun, List<RefusalReason> expectedRefusalReasons) {
        verifyNotificationRejectionForLookupAddress(iun, expectedRefusalReasons, null);
    }

    private void verifyNotificationRejectionForLookupAddress(String iun, List<RefusalReason> expectedRefusalReasons, Integer expectedCost) {
        TimelineElementInternal timelineElementInternal = TestUtils.getNotificationRejected(iun, timelineService).get();
        RequestRefusedDetailsInt details = (RequestRefusedDetailsInt) timelineElementInternal.getDetails();
        Assertions.assertEquals(expectedRefusalReasons.size(), details.getRefusalReasons().size());
        List<NotificationRefusedErrorInt> actualRefusalReasons = details.getRefusalReasons();
        for(RefusalReason expectedRefusalReason : expectedRefusalReasons) {
            Assertions.assertTrue(checkRefusalReason(expectedRefusalReason, actualRefusalReasons),
                    "Refusal reason not found in the list of refusal reasons for IUN: " + iun);

        }
        if(expectedCost != null) {
            Assertions.assertEquals(expectedCost, details.getNotificationCost());
        }
    }

    private boolean checkRefusalReason(RefusalReason expectedRefusalReason, List<NotificationRefusedErrorInt> actualRefusalReasons) {
        return actualRefusalReasons.stream()
                .anyMatch(r -> r.getErrorCode().equals(expectedRefusalReason.getErrorCode()) &&
                        r.getRecIndex().equals(expectedRefusalReason.getRecIndex()));
    }

    private void checkRecIndexInNationalRegistryValidationCall(String iun, List<Integer> recIndexesInput) {
        // Attendo sia presente l'elemento di timeline di ricerca in fase di validazione sui registri nazionali
        await().untilAsserted(() ->
                Assertions.assertTrue(
                        TestUtils.checkIsPresentNationalRegistryValidationCall(iun, timelineService)
                )
        );
        Optional<TimelineElementInternal> timelineElementOpt = timelineService.getTimelineElement(
                iun,
                TestUtils.buildTimelineEventIdNationalRegistryValidationCall(iun)
        );

        if (timelineElementOpt.isPresent()) {
            TimelineElementInternal timelineElement = timelineElementOpt.get();
            PublicRegistryValidationCallDetailsInt details =
                    (PublicRegistryValidationCallDetailsInt) timelineElement.getDetails();
            List<Integer> recIndexesDetails = details.getRecIndexes();
            Assertions.assertEquals(recIndexesDetails.size(), details.getRecIndexes().size());
            Assertions.assertTrue(compareRecIndexesLists(recIndexesDetails, recIndexesInput));
        } else {
            Assertions.fail("Timeline element with category NATIONAL_REGISTRY_VALIDATION_CALL not found for IUN: " + iun);
        }
    }

    public static boolean compareRecIndexesLists(List<Integer> recIndexesDetails, List<Integer> recIndexesInput) {
        if (recIndexesDetails == null && recIndexesInput == null) {
            return true;
        }

        if (recIndexesDetails == null || recIndexesInput == null) {
            return false;
        }

        Set<Integer> integerList1 = new HashSet<>(recIndexesDetails);
        Set<Integer> integerList2 = new HashSet<>(recIndexesInput);

        return integerList1.equals(integerList2);
    }

}
