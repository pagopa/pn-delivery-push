package it.pagopa.pn.deliverypush.action.it;

import it.pagopa.pn.deliverypush.action.choosedeliverymode.ChooseDeliveryModeUtils;
import it.pagopa.pn.deliverypush.action.it.mockbean.AddressManagerClientMock;
import it.pagopa.pn.deliverypush.action.it.mockbean.ExternalChannelMock;
import it.pagopa.pn.deliverypush.action.it.mockbean.PaperChannelMock;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action.startworkflow.StartWorkflowHandler;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator;
import it.pagopa.pn.deliverypush.logtest.ConsoleAppenderCustom;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelSendRequest;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
<<<<<<< HEAD
import it.pagopa.pn.deliverypush.service.impl.*;
import it.pagopa.pn.deliverypush.service.utils.PublicRegistryUtils;
import it.pagopa.pn.deliverypush.utils.PaperSendModeUtils;
import it.pagopa.pn.deliverypush.utils.StatusUtils;
=======
>>>>>>> 4e3bc35f4e99d219f6af7ce4c631aa9aea1e8006
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.util.List;

import static org.awaitility.Awaitility.await;
<<<<<<< HEAD
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        StartWorkflowHandler.class,
        StartWorkflowForRecipientHandler.class,
        AnalogWorkflowHandler.class,
        ChooseDeliveryModeHandler.class,
        DigitalWorkFlowHandler.class,
        DigitalWorkFlowExternalChannelResponseHandler.class,
        PaperChannelServiceImpl.class,
        PaperChannelUtils.class,
        PaperChannelResponseHandler.class,
        AnalogWorkflowPaperChannelResponseHandler.class,
        AuditLogServiceImpl.class,
        DigitalWorkFlowRetryHandler.class,
        CompletionWorkFlowHandler.class,
        NationalRegistriesResponseHandler.class,
        NationalRegistriesServiceImpl.class,
        ExternalChannelServiceImpl.class,
        IoServiceImpl.class,
        NotificationProcessCostServiceImpl.class,
        SafeStorageServiceImpl.class,
        ExternalChannelResponseHandler.class,
        RefinementHandler.class,
        NotificationViewedRequestHandler.class,
        DigitalWorkFlowUtils.class,
        CourtesyMessageUtils.class,
        AarUtils.class,
        CompletelyUnreachableUtils.class,
        ExternalChannelUtils.class,
        AnalogWorkflowUtils.class,
        ChooseDeliveryModeUtils.class,
        TimelineUtils.class,
        PublicRegistryUtils.class,
        StatusUtils.class,
        NotificationServiceImpl.class,
        TimeLineServiceImpl.class,
        PaperNotificationFailedServiceImpl.class,
        StatusServiceImpl.class,
        AddressBookServiceImpl.class,
        ConfidentialInformationServiceImpl.class,
        AttachmentUtils.class,
        NotificationUtils.class,
        PecDeliveryWorkflowLegalFactsGenerator.class,
        RefinementScheduler.class,
        RegisteredLetterSender.class,
        PaperNotificationFailedDaoMock.class,
        TimelineDaoMock.class,
        TimelineCounterDaoMock.class,
        ExternalChannelMock.class,
        PaperNotificationFailedDaoMock.class,
        PnDataVaultClientMock.class,
        MVPParameterConsumer.class,
        NotificationCost.class,
        ViewNotification.class,
        PnDeliveryClientReactiveMock.class,
        PnDataVaultClientReactiveMock.class,
        DocumentCreationRequestServiceImpl.class,
        DocumentCreationRequestDaoMock.class,
        SafeStorageResponseHandler.class,
        DocumentCreationResponseHandler.class,
        ReceivedLegalFactCreationResponseHandler.class,
        ScheduleRecipientWorkflow.class,
        AarCreationResponseHandler.class,
        NotificationViewLegalFactCreationResponseHandler.class,
        DigitalDeliveryCreationResponseHandler.class,
        FailureWorkflowHandler.class,
        SuccessWorkflowHandler.class,
        NotificationValidationActionHandler.class,
        TaxIdPivaValidator.class,
        ReceivedLegalFactCreationRequest.class,
        NotificationValidationScheduler.class,
        DigitalWorkflowFirstSendRepeatHandler.class,
        SendAndUnscheduleNotification.class,
        AddressValidator.class,
        AddressManagerServiceImpl.class,
        AddressManagerClientMock.class,
        NormalizeAddressHandler.class,
        AddressManagerResponseHandler.class,
        AnalogDeliveryFailureWorkflowLegalFactsGenerator.class,
        AnalogFailureDeliveryCreationResponseHandler.class,
        AnalogTestNormalizedAddressIT.SpringTestConfiguration.class,
        F24Validator.class,
        F24ClientMock.class,
        PnExternalRegistriesClientReactiveMock.class,
        PaymentValidator.class,
        NotificationRefusedActionHandler.class,
        PaperSendModeUtils.class
})
@TestPropertySource("classpath:/application-test.properties")
@EnableConfigurationProperties(value = PnDeliveryPushConfigs.class)
@DirtiesContext
class AnalogTestNormalizedAddressIT {

    @TestConfiguration
    static class SpringTestConfiguration extends AbstractWorkflowTestConfiguration {
        public SpringTestConfiguration() {
            super();
        }
    }

=======
>>>>>>> 4e3bc35f4e99d219f6af7ce4c631aa9aea1e8006

class AnalogTestNormalizedAddressIT extends CommonTestConfiguration{
    @SpyBean
    LegalFactGenerator legalFactGenerator;
    @SpyBean
    PaperChannelMock paperChannelMock;
    @Autowired
    StartWorkflowHandler startWorkflowHandler;
    @Autowired
    TimelineService timelineService;
    @Autowired
    NotificationUtils notificationUtils;
    @Autowired
    NotificationService notificationService;

    @Test
    void sendAnalogSuccessWithNormalizedAddress() {
    /*
       - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)

       - Pa physical address presente con struttura indirizzo che porta al successo del primo invio
     */

        PhysicalAddressInt paPhysicalAddress = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS + " Via Nuova_" + AddressManagerClientMock.ADDRESS_MANAGER_TO_NORMALIZE)
                .build();

        String iun = TestUtils.getRandomIun();

        String taxId = "test_tax_id";
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withInternalId("ANON_"+taxId)
                .withPhysicalAddress(paPhysicalAddress)
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withIun(iun)
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);
        
        pnDeliveryClientMock.addNotification(notification);

        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        PhysicalAddressInt paPhysicalAddressNormalized =  PhysicalAddressInt.builder()
                .address(paPhysicalAddress.getAddress() +"_NORMALIZED")
                .addressDetails(paPhysicalAddress.getAddressDetails() +"_NORMALIZED")
                .foreignState(paPhysicalAddress.getForeignState() +"_NORMALIZED")
                .province(paPhysicalAddress.getProvince() +"_NORMALIZED")
                .municipality(paPhysicalAddress.getMunicipality() +"_NORMALIZED")
                .zip(paPhysicalAddress.getZip() +"_NORMALIZED")
                .municipalityDetails(paPhysicalAddress.getMunicipalityDetails() +"_NORMALIZED")
                .fullname(recipient.getDenomination())
                .at(paPhysicalAddress.getAt())
                .build();
        
        addressManagerClientMock.addNormalizedAddress(iun, recIndex, paPhysicalAddressNormalized );

        
        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);
        
        String timelineId = TimelineEventId.REFINEMENT.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build()
        );

        // Viene atteso fino a che l'ultimo elemento di timeline sia stato inserito per procedere con le successive verifiche
        await().untilAsserted(() ->
                Assertions.assertTrue(timelineService.getTimelineElement(iun, timelineId).isPresent())
        );
        
        //Ottengo la notifica normalizzata
        NotificationInt notificationNormalized = notificationService.getNotificationByIun(notification.getIun());
        //Ottengo il recpient normalizzato
        NotificationRecipientInt recipientNormalized = notificationUtils.getRecipientFromIndex(notificationNormalized, recIndex);
        

        //Viene verificato che gli indirizzi PLATFORM SPECIAL E GENERAL non siano presenti
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificata la presenza del primo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito dalla PA
        TestUtils.checkSendPaperToExtChannel(iun, recIndex, paPhysicalAddressNormalized, 0, timelineService);
        
        Mockito.verify(paperChannelMock, Mockito.times(1)).send(Mockito.any(PaperChannelSendRequest.class));
        
        //Viene effettuato il check dei legalFacts generati
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(false)
                .build();

        TestUtils.checkGeneratedLegalFacts(
                notificationNormalized,
                recipientNormalized,
                recIndex,
                0,
                generatedLegalFactsInfo,
                EndWorkflowStatus.SUCCESS,
                legalFactGenerator,
                timelineService,
                null
        );

        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs();
    }

}
