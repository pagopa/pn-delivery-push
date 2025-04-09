package it.pagopa.pn.deliverypush.action.it;

import it.pagopa.pn.deliverypush.action.it.mockbean.PnDeliveryClientMock;
import it.pagopa.pn.deliverypush.action.it.mockbean.SafeStorageClientMock;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action.startworkflow.StartWorkflowHandler;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.*;
import it.pagopa.pn.deliverypush.dto.timeline.NotificationRefusedErrorInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.PublicRegistryValidationCallDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.RequestRefusedDetailsInt;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static it.pagopa.pn.deliverypush.action.it.mockbean.NationalRegistriesClientMock.NOT_FOUND;
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

    @Test
    void testNotificationAddressLookup_AddressFound() {
        //Notifica monodestinatario con lookupAddress attivo che trova un indirizzo su National registries.
        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        notificationDocumentList = TestUtils.firstFileUploadFromNotification(listDocumentWithContent, notificationDocumentList, safeStorageClientMock);

        String taxId = "test_tax_id";
        NotificationRecipientInt recipient = getNotificationRecipientInt(taxId, null);

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

        await().untilAsserted(() ->
                Assertions.assertTrue(
                        TestUtils.checkIsPresentNationalRegistryValidationCall(iun, timelineService)
                )
        );
        checkRecIndexFromNationalRegistryValidationCall(iun, timelineService, List.of(recIndex));
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
    void testNotificationAddressLookup_AddressNotFound() {
        //Notifica monodestinatario con lookupAddress attivo che non trova un indirizzo su National registries
        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        notificationDocumentList = TestUtils.firstFileUploadFromNotification(listDocumentWithContent, notificationDocumentList, safeStorageClientMock);

        String taxId = "test_tax_id" + NOT_FOUND;
        NotificationRecipientInt recipient = getNotificationRecipientInt(taxId, null);

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

        await().untilAsserted(() ->
                Assertions.assertTrue(
                        TestUtils.checkIsPresentNationalRegistryValidationCall(iun, timelineService)
                )
        );
        checkRecIndexFromNationalRegistryValidationCall(iun, timelineService, List.of(recIndex));

        await().untilAsserted(() ->
                Assertions.assertTrue(
                        TestUtils.checkIsPresentNotificationRejected(iun, timelineService)
                )
        );

        String expectedValidationCallTimelineId = TestUtils.buildTimelineEventIdNationalRegistryValidationCall(iun);

        Assertions.assertFalse(
                TestUtils.checkIsPresentNationalRegistryValidationResponse(expectedValidationCallTimelineId, iun, recIndex, timelineService)
        );

        verifyNotificationRejectionForLookupAddress(iun, recIndex);
    }

    @Test
    void testNotificationAddressLookup_AddressNotFoundJustForOneRecipient() {
        //Notifica con 2 destinatari con lookupAddress attivo per entrambi; National Registries non trova un indirizzo per un destinatario.
        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        notificationDocumentList = TestUtils.firstFileUploadFromNotification(listDocumentWithContent, notificationDocumentList, safeStorageClientMock);

        String taxId1 = "test_tax_id1" + NOT_FOUND;
        String taxId2 = "test_tax_id2";
        NotificationRecipientInt recipient1 = getNotificationRecipientInt(taxId1, null);
        NotificationRecipientInt recipient2 = getNotificationRecipientInt(taxId2, null);

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

        await().untilAsserted(() ->
                Assertions.assertTrue(
                        TestUtils.checkIsPresentNationalRegistryValidationCall(iun, timelineService)
                )
        );

        checkRecIndexFromNationalRegistryValidationCall(iun, timelineService, List.of(recIndex1, recIndex2));

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

        verifyNotificationRejectionForLookupAddress(iun, recIndex1);
    }

    @Test
    void testNotificationAddressLookup_defaultPhysicalAddressJustForOneRecipient() {
        //Notifica con 2 destinatari con lookupAddress attivo per entrambi; National Registries trova l' indirizzo per un destinatario.
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
        Integer recIndex2 = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient2.getTaxId());
        pnDeliveryClientMock.addNotification(notification);

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        await().untilAsserted(() ->
                Assertions.assertTrue(
                        TestUtils.checkIsPresentNationalRegistryValidationCall(iun, timelineService)
                )
        );

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
    void testNotificationAddressLookup_AddressFoundWithoutLookupAddress() {
        // Notifica monodestinatario con lookupAddress disattivo e indirizzo trovato su National Registries.
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

        await().untilAsserted(() ->
                Assertions.assertFalse(
                        TestUtils.checkIsPresentNationalRegistryValidationCall(iun, timelineService)
                )
        );
        String expectedValidationCallTimelineId = TestUtils.buildTimelineEventIdNationalRegistryValidationCall(iun);

        await().untilAsserted(() ->
                Assertions.assertFalse(
                        TestUtils.checkIsPresentNationalRegistryValidationResponse(expectedValidationCallTimelineId, iun, recIndex, timelineService)
                )
        );
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

    private void verifyNotificationRejectionForLookupAddress(String iun, Integer recIndex) {
        String expectedErrorCode = PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt.ADDRESS_NOT_FOUND.getValue();
        String expectedDetail = "Address not found for recipient index: " + recIndex;
        TimelineElementInternal timelineElementInternal = TestUtils.getNotificationRejected(iun, timelineService).get();
        RequestRefusedDetailsInt details = (RequestRefusedDetailsInt) timelineElementInternal.getDetails();
        Assertions.assertEquals(1, details.getRefusalReasons().size());
        NotificationRefusedErrorInt error = details.getRefusalReasons().get(recIndex);
        Assertions.assertEquals(expectedErrorCode, error.getErrorCode());
        Assertions.assertEquals(expectedDetail, error.getDetail());
    }

    public static void checkRecIndexFromNationalRegistryValidationCall(String iun, TimelineService timelineService, List<Integer> recIndexesInput) {
        if (TestUtils.checkIsPresentNationalRegistryValidationCall(iun, timelineService)) {
            Optional<TimelineElementInternal> timelineElementOpt = timelineService.getTimelineElement(
                    iun,
                    TestUtils.buildTimelineEventIdNationalRegistryValidationCall(iun)
            );

            if (timelineElementOpt.isPresent()) {
                TimelineElementInternal timelineElement = timelineElementOpt.get();
                if (timelineElement.getDetails() instanceof PublicRegistryValidationCallDetailsInt details) {
                    List<Integer> recIndexesDetails = details.getRecIndexes();
                    if (details.getRecIndexes() != null) {
                        Assertions.assertEquals(recIndexesDetails.size(), details.getRecIndexes().size());
                        Assertions.assertTrue(compareRecIndexesLists(recIndexesDetails, recIndexesInput));
                    }
                }
            }
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
