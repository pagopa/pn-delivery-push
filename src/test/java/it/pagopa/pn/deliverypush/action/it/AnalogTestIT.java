package it.pagopa.pn.deliverypush.action.it;

import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileCreationResponse;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadInfo;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypush.action.*;
import it.pagopa.pn.deliverypush.action.it.mockbean.*;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action.utils.*;
import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.ContactPhaseInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.DeliveryModeInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogDetailsInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient;
import it.pagopa.pn.deliverypush.middleware.responsehandler.ExternalChannelResponseHandler;
import it.pagopa.pn.deliverypush.middleware.responsehandler.PublicRegistryResponseHandler;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.impl.*;
import it.pagopa.pn.deliverypush.service.utils.PublicRegistryUtils;
import it.pagopa.pn.deliverypush.utils.StatusUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.with;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        StartWorkflowHandler.class,
        AnalogWorkflowHandler.class,
        ChooseDeliveryModeHandler.class,
        DigitalWorkFlowHandler.class,
        CompletionWorkFlowHandler.class,
        PublicRegistryResponseHandler.class,
        ExternalChannelResponseHandler.class,
        PublicRegistryServiceImpl.class,
        ExternalChannelServiceImpl.class,
        IoServiceImpl.class,
        RefinementHandler.class,
        NotificationViewedHandler.class,
        DigitalWorkFlowUtils.class,
        CourtesyMessageUtils.class,
        AarUtils.class,
        ExternalChannelUtils.class,
        CompletelyUnreachableUtils.class,
        AnalogWorkflowUtils.class,
        TimelineUtils.class,
        PublicRegistryUtils.class,
        ChooseDeliveryModeUtils.class,
        NotificationUtils.class,
        NotificationServiceImpl.class,
        TimeLineServiceImpl.class,
        PaperNotificationFailedServiceImpl.class,
        StatusServiceImpl.class,
        ConfidentialInformationServiceImpl.class,
        AddressBookServiceImpl.class,
        CheckAttachmentUtils.class,
        StatusUtils.class,
        PaperNotificationFailedDaoMock.class,
        TimelineDaoMock.class,
        ExternalChannelMock.class,
        PaperNotificationFailedDaoMock.class,
        PnDataVaultClientMock.class,
        AnalogTestIT.SpringTestConfiguration.class
})
@TestPropertySource(properties = {
        "pn.delivery-push.featureflags.externalchannel=new",
})
class AnalogTestIT {

    public static final long WAITING_TIME = 10000;

    @TestConfiguration
    static class SpringTestConfiguration extends AbstractWorkflowTestConfiguration {
        public SpringTestConfiguration() {
            super();
        }
    }

    @Autowired
    private StartWorkflowHandler startWorkflowHandler;

    @Autowired
    private TimelineService timelineService;

    @Autowired
    private InstantNowSupplier instantNowSupplier;
    
    @Autowired
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;

    @SpyBean
    private PnSafeStorageClient safeStorageClientMock;

    @SpyBean
    private ExternalChannelMock externalChannelMock;

    @SpyBean
    private CompletionWorkFlowHandler completionWorkflow;

    @Autowired
    private PnDeliveryClientMock pnDeliveryClientMock;

    @Autowired
    private UserAttributesClientMock addressBookMock;

    @Autowired
    private PublicRegistryMock publicRegistryMock;
    
    @Autowired
    private TimelineDaoMock timelineDaoMock;

    @Autowired
    private NotificationUtils notificationUtils;

    @Autowired
    private PaperNotificationFailedDaoMock paperNotificationFailedDaoMock;
    
    @Autowired
    private PnDataVaultClientMock pnDataVaultClientMock;
    
    @Autowired
    private StatusUtils statusUtils;
    
    @BeforeEach
    public void setup() {
        TimeParams times = new TimeParams();
        times.setWaitingForReadCourtesyMessage(Duration.ofSeconds(1));
        times.setSchedulingDaysSuccessDigitalRefinement(Duration.ofSeconds(1));
        times.setSchedulingDaysFailureDigitalRefinement(Duration.ofSeconds(1));
        times.setSchedulingDaysSuccessAnalogRefinement(Duration.ofSeconds(1));
        times.setSchedulingDaysFailureAnalogRefinement(Duration.ofSeconds(1));
        times.setSecondNotificationWorkflowWaitingTime(Duration.ofSeconds(1));
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);

        PnDeliveryPushConfigs.ExternalChannel externalChannelCfg = new PnDeliveryPushConfigs.ExternalChannel();
        externalChannelCfg.setAnalogCodesFail(List.of("__005__","__006__","__008__","__009__"));
        externalChannelCfg.setAnalogCodesSuccess(List.of("__004__","__007__"));
        externalChannelCfg.setAnalogCodesProgress(List.of("__001__","__002__","__003__"));
        Mockito.when(pnDeliveryPushConfigs.getExternalChannel()).thenReturn(externalChannelCfg);

        PnDeliveryPushConfigs.Webapp webapp = new PnDeliveryPushConfigs.Webapp();
        webapp.setDirectAccessUrlTemplate("test");
        Mockito.when(pnDeliveryPushConfigs.getWebapp()).thenReturn(webapp);

        Mockito.when(pnDeliveryPushConfigs.getPaperMessageNotHandled()).thenReturn(false);

        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());

        //File mock to return for getFileAndDownloadContent
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        fileDownloadResponse.setContentType("application/pdf");
        fileDownloadResponse.setContentLength(new BigDecimal(0));
        fileDownloadResponse.setChecksum("123");
        fileDownloadResponse.setKey("123");
        fileDownloadResponse.setDownload(new FileDownloadInfo());
        fileDownloadResponse.getDownload().setUrl("https://www.url.qualcosa.it");
        fileDownloadResponse.getDownload().setRetryAfter(new BigDecimal(0));


        FileCreationResponse fileCreationResponse = new FileCreationResponse();
        fileCreationResponse.setKey("123");
        fileCreationResponse.setSecret("abc");
        fileCreationResponse.setUploadUrl("https://www.unqualcheurl.it");
        fileCreationResponse.setUploadMethod(FileCreationResponse.UploadMethodEnum.POST);


        Mockito.when( safeStorageClientMock.getFile( Mockito.anyString(), Mockito.anyBoolean()))
                .thenReturn( fileDownloadResponse );
        Mockito.when( safeStorageClientMock.createAndUploadContent(Mockito.any())).thenReturn(fileCreationResponse);

        // Given

        pnDeliveryClientMock.clear();
        addressBookMock.clear();
        publicRegistryMock.clear();
        timelineDaoMock.clear();
        paperNotificationFailedDaoMock.clear();
        pnDataVaultClientMock.clear();
    }
    
    @Test
    void notificationViewedPaPhysicalAddressSend() {
 /*
       - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)

       - Indirizzo courtesy message presente, dunque inviato (Ottenuto valorizzando il courtesyAddress del addressBookEntry)
       - Pa physical address presente con struttura indirizzo che porta al fallimento dell'invio tramite external channel (Ottenuto inserendo nell'indirizzo ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR)
         e ottenimento indirizzo investigazione
       - Viene visualizzata la notifica in fase d'invio del messaggio di cortesia, questo comporta che nessun invio analogico successivo avvenga
     */

        PhysicalAddressInt paPhysicalAddress = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR + ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " Via Nuova")
                .build();

        String iun = "IUN01";

        //Simulazione visualizzazione notifica a valle del send del messaggio di cortesi
        String taxId = TimelineDaoMock.SIMULATE_VIEW_NOTIFICATION +  TimelineEventId.SEND_PAPER_FEEDBACK.buildEventId(EventId.builder()
                .iun(iun)
                .recIndex(0)
                .index(1)
                .build()
        );

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withInternalId("ANON_"+taxId)
                .withPhysicalAddress(paPhysicalAddress)
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();

        List<CourtesyDigitalAddressInt> listCourtesyAddress = Collections.singletonList(CourtesyDigitalAddressInt.builder()
                .address("test@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL)
                .build());

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addCourtesyDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), listCourtesyAddress);

        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);
        
        String timelineId = TimelineEventId.PUBLIC_REGISTRY_RESPONSE.buildEventId(
                TimelineEventId.PUBLIC_REGISTRY_CALL.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex)
                                .deliveryMode(DeliveryModeInt.ANALOG)
                                .contactPhase(ContactPhaseInt.SEND_ATTEMPT)
                                .sentAttemptMade(1)
                                .build()
                )
        );

        // Viene atteso fino a che l'ultimo elemento di timeline sia stato inserito per procedere con le successive verifiche
        await().untilAsserted(() -> 
                Assertions.assertTrue(timelineService.getTimelineElement(iun, timelineId).isPresent())
        );
        
        //Viene verificato che sia stato inviato un messaggio ad ogni indirizzo presente nei courtesyaddress
        TestUtils.checkSendCourtesyAddresses(iun, recIndex, listCourtesyAddress, timelineService, externalChannelMock);

        //Viene verificato che gli indirizzi PLATFORM SPECIAL E GENERAL non siano presenti
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificata la presenza del primo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito dalla PA
        TestUtils.checkSendPaperToExtChannel(iun, recIndex, paPhysicalAddress, 0, timelineService);

        ArgumentCaptor<PhysicalAddressInt> pnPhysicalAddressArgumentCaptor = ArgumentCaptor.forClass(PhysicalAddressInt.class);

        Mockito.verify(externalChannelMock, Mockito.times(1)).sendAnalogNotification(Mockito.any(NotificationInt.class), Mockito.any(NotificationRecipientInt.class), pnPhysicalAddressArgumentCaptor.capture(), Mockito.anyString(), Mockito.any(), Mockito.anyString());


        PhysicalAddressInt pnExtChnPaperEvent = pnPhysicalAddressArgumentCaptor.getValue();
        Assertions.assertEquals(paPhysicalAddress.getAddress(), pnExtChnPaperEvent.getAddress());

        //Viene verificato che la notifica sia stata visualizzata
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.NOTIFICATION_VIEWED.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex)
                                .build())).isPresent());
    }

    @Test
    void notificationViewedNoAnalogSend() {
 /*
       - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)

       - Indirizzo courtesy message presente, dunque inviato (Ottenuto valorizzando il courtesyAddress del addressBookEntry)
       - Viene visualizzata la notifica in fase d'invio del messaggio di cortesia, questo comporta che nessun invio analogico avvenga
     */

        PhysicalAddressInt paPhysicalAddress = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR + ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " Via Nuova")
                .build();

        String iun = "IUN01";

        //Simulazione visualizzazione notifica a valle del send del messaggio di cortesi
        String taxId = TimelineDaoMock.SIMULATE_VIEW_NOTIFICATION +  TimelineEventId.SEND_COURTESY_MESSAGE.buildEventId(EventId.builder()
                .iun(iun)
                .recIndex(0)
                .index(0)
                .build()
        );

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withInternalId("ANON_"+taxId)
                .withPhysicalAddress(paPhysicalAddress)
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();

        List<CourtesyDigitalAddressInt> listCourtesyAddress = Collections.singletonList(CourtesyDigitalAddressInt.builder()
                .address("test@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL)
                .build());

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addCourtesyDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), listCourtesyAddress);

        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        String timelineId = TimelineEventId.SCHEDULE_ANALOG_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build()
        );
        
        //Dal momento che l'ultimo elemento di timeline non viene inserito in prossimità della fine del workflow viene utilizzato un delay
        with().pollDelay(5, SECONDS).await().untilAsserted(() ->
                Assertions.assertTrue(timelineService.getTimelineElement(iun, timelineId).isPresent())
        );
        
        //Viene verificato che sia stato inviato un messaggio ad ogni indirizzo presente nei courtesyaddress
        TestUtils.checkSendCourtesyAddresses(iun, recIndex, listCourtesyAddress, timelineService, externalChannelMock);

        //Viene verificato che gli indirizzi PLATFORM SPECIAL E GENERAL non siano presenti
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificata l'assenza degli invii verso external channel
        TestUtils.checkNotSendPaperToExtChannel(iun, recIndex, 0, timelineService);
        Mockito.verify(externalChannelMock, Mockito.times(0)).sendAnalogNotification(Mockito.any(NotificationInt.class), Mockito.any(NotificationRecipientInt.class), Mockito.any(PhysicalAddressInt.class), Mockito.anyString(), Mockito.any(), Mockito.anyString());

        //Viene verificato che la notifica sia stata visualizzata
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.NOTIFICATION_VIEWED.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex)
                                .build())).isPresent());
    }

    @Test
    void completelyUnreachable() {
 /*
       - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
       
       - Indirizzo courtesy message presente, dunque inviato (Ottenuto valorizzando il courtesyAddress del addressBookEntry)
       - Pa physical address presente con struttura indirizzo che porta al fallimento dell'invio tramite external channel (Ottenuto inserendo nell'indirizzo ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR)
         e invio di una seconda notifica (all'indirizzo ottenuto dall'investigazione) con successivo fallimento (ottenuto concatenando all'indirizzo ExternalChannelMock.EXTCHANNEL_SEND_FAIL) 
       - Public Registry Indirizzo fisico non trovato (Ottenuto non valorizzando nessun indirizzo fisico per il recipient in PUB_REGISTRY_PHYSICAL)
     */

        PhysicalAddressInt paPhysicalAddress = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR + ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " Via Nuova")
                .build();

        String taxid01 = "TAXID01";
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxid01)
                .withInternalId("ANON_"+taxid01)
                .withPhysicalAddress(paPhysicalAddress)
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("IUN01")
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();


        List<CourtesyDigitalAddressInt> listCourtesyAddress = Collections.singletonList(CourtesyDigitalAddressInt.builder()
                .address("test@works.it")
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL)
                .build());

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addCourtesyDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), listCourtesyAddress);

        String iun = notification.getIun();
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());
        

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che lo stato non passi in EFFECTIVE DATE
        await().untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.EFFECTIVE_DATE, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );

        //Viene verificato che sia stato inviato un messaggio ad ogni indirizzo presente nei courtesyaddress
        TestUtils.checkSendCourtesyAddresses(iun, recIndex, listCourtesyAddress, timelineService, externalChannelMock);

        //Viene verificato che gli indirizzi PLATFORM SPECIAL E GENERAL non siano presenti
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificata la presenza del primo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito dalla PA
        TestUtils.checkSendPaperToExtChannel(iun, recIndex, paPhysicalAddress, 0, timelineService);
        //Viene verificata la presenza del secondo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito dal postino
        //checkSendToExtChannel(iun, TestUtils.PHYSICAL_ADDRESS_FAILURE_BOTH, 1);
        
        //Viene verificato l'effettivo invio delle due notifiche verso externalChannel
        Mockito.verify(externalChannelMock, Mockito.times(2)).sendAnalogNotification(Mockito.any(NotificationInt.class), Mockito.any(NotificationRecipientInt.class), Mockito.any(PhysicalAddressInt.class), Mockito.anyString(), Mockito.any(), Mockito.anyString());

        String eventIdFirstSend = TimelineEventId.SEND_ANALOG_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .index(0)
                        .build());

        Optional<SendAnalogDetailsInt> sendPaperDetailsOpt = timelineService.getTimelineElementDetails(iun, eventIdFirstSend, SendAnalogDetailsInt.class);
        Assertions.assertTrue(sendPaperDetailsOpt.isPresent());

        SendAnalogDetailsInt sendPaperDetails = sendPaperDetailsOpt.get();
        Assertions.assertEquals( paPhysicalAddress.getAddress() , sendPaperDetails.getPhysicalAddress().getAddress() );
        Assertions.assertEquals( paPhysicalAddress.getForeignState() , sendPaperDetails.getPhysicalAddress().getForeignState());
        Assertions.assertEquals(1, sendPaperDetails.getNumberOfPages());

        //Viene verificato che il workflow sia fallito
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.ANALOG_FAILURE_WORKFLOW.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex)
                                .build())).isPresent());

        //Viene verificato che il destinatario risulti completamente irraggiungibile
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.COMPLETELY_UNREACHABLE.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex)
                                .build())).isPresent());

        //Viene verificato che sia avvenuto il perfezionamento
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.REFINEMENT.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex)
                                .build())).isPresent());
    }

    @Test
    void publicRegistryAddressFailInvestigationAddressSuccessTest() {
  /*
       - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
       
       - Pa physical address NON presente (Ottenuto NON valorizzando physicalAddress del recipient della notifica)
       - Public Registry indirizzo trovato ma restituisce un indirizzo che fallirà nell'invio di external channel (Ottenuto inserendo nell'indirizzo ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR)
         con invio di una seconda notifica (all'indirizzo ottenuto dall'investigazione) con successo (ottenuto concatenando all'indirizzo ExternalChannelMock.EXTCHANNEL_SEND_OK)
ì    */

        PhysicalAddressInt publicRegistryAddress = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR + ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS + " Via Nuova")
                .build();

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId("TAXID01")
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("IUN01")
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();
        
        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient.getTaxId(), notification.getSender().getPaId(), Collections.emptyList());

        publicRegistryMock.addPhysical(recipient.getTaxId(), publicRegistryAddress);

        String iun = notification.getIun();
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(notification.getIun());

        // Viene atteso fino a che lo stato non passi in EFFECTIVE DATE
        await().untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.EFFECTIVE_DATE, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );
        
        //Viene verificato che non sia stato inviato alcun messaggio di cortesia
        TestUtils.checkSendCourtesyAddresses(iun, recIndex, Collections.emptyList(), timelineService, externalChannelMock);

        //Viene verificato che gli indirizzi PLATFORM SPECIAL E GENERAL non siano presenti
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificata la presenza del primo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito da publicRegistry
        TestUtils.checkSendPaperToExtChannel(iun, recIndex, publicRegistryAddress, 0, timelineService);

        /*
        Viene verificata la presenza del primo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito dall'investigazione
            checkSendToExtChannel(iun, TestUtils.PHYSICAL_ADDRESS_OK, 1);
        */

        //Vengono verificati il numero di send verso external channel
        Mockito.verify(externalChannelMock, Mockito.times(2)).sendAnalogNotification(Mockito.any(NotificationInt.class), Mockito.any(NotificationRecipientInt.class), Mockito.any(PhysicalAddressInt.class), Mockito.anyString(), Mockito.any(), Mockito.anyString());

        TestUtils.checkSuccessAnalogWorkflow(iun, recIndex, timelineService, completionWorkflow);

        //Viene verificato che sia avvenuto il perfezionamento
        TestUtils.checkRefinement(iun, recIndex, timelineService);
    }

    @Test
    void completelyUnreachableTwoRecipient() {
 /*     PRIMO RECIPIENT
       - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
       
       - Indirizzo courtesy message presente, dunque inviato (Ottenuto valorizzando il courtesyAddress del addressBookEntry)
       - Pa physical address presente con struttura indirizzo che porta al fallimento dell'invio tramite external channel (Ottenuto inserendo nell'indirizzo ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR)
         e invio di una seconda notifica (all'indirizzo ottenuto dall'investigazione) con successivo fallimento (ottenuto concatenando all'indirizzo ExternalChannelMock.EXTCHANNEL_SEND_FAIL) 
       - Public Registry Indirizzo fisico non trovato (Ottenuto non valorizzando nessun indirizzo fisico per il recipient in PUB_REGISTRY_PHYSICAL)
        
         SECONDO RECIPIENT
       - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
       
       - Indirizzo courtesy message presente, dunque inviato (Ottenuto valorizzando il courtesyAddress del addressBookEntry)
       - Pa physical address presente con struttura indirizzo che porta al fallimento dell'invio tramite external channel (Ottenuto inserendo nell'indirizzo ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR)
         e invio di una seconda notifica (all'indirizzo ottenuto dall'investigazione) con successivo fallimento (ottenuto concatenando all'indirizzo ExternalChannelMock.EXTCHANNEL_SEND_FAIL) 
       - Public Registry Indirizzo fisico non trovato (Ottenuto non valorizzando nessun indirizzo fisico per il recipient in PUB_REGISTRY_PHYSICAL)
     */

        PhysicalAddressInt paPhysicalAddress1 = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR + ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " Via Nuova")
                .build();

        String taxid01 = "TAXID01";
        
        NotificationRecipientInt recipient1 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxid01)
                .withInternalId("ANON_"+taxid01)
                .withPhysicalAddress(paPhysicalAddress1)
                .build();
        
        List<CourtesyDigitalAddressInt> listCourtesyAddressRecipient1 = Collections.singletonList(CourtesyDigitalAddressInt.builder()
                .address("test@mail.it")
                .type( CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL )
                .build());

        PhysicalAddressInt paPhysicalAddress2 = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR + ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " Via Nuova")
                .build();

        String taxid02 = "TAXID02";
        NotificationRecipientInt recipient2 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxid02)
                .withInternalId("ANON_"+taxid02)
                .withPhysicalAddress(paPhysicalAddress2)
                .build();

        List<CourtesyDigitalAddressInt> listCourtesyAddressRecipient2 = Collections.singletonList(CourtesyDigitalAddressInt.builder()
                .address("test2@mail.it")
                .type( CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL )
                .build());

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("IUN01")
                .withPaId("paId01")
                .withNotificationRecipient( List.of(recipient1, recipient2) )
                .build();

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addCourtesyDigitalAddresses(recipient1.getInternalId(), notification.getSender().getPaId(), listCourtesyAddressRecipient1);
        addressBookMock.addCourtesyDigitalAddresses(recipient2.getInternalId(), notification.getSender().getPaId(), listCourtesyAddressRecipient2);

        String iun = notification.getIun();
        Integer recIndex1 = notificationUtils.getRecipientIndex(notification, recipient1.getTaxId());
        Integer recIndex2 = notificationUtils.getRecipientIndex(notification, recipient2.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che lo stato non passi in EFFECTIVE DATE
        await().untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.EFFECTIVE_DATE, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );
        
        //Viene verificato che sia stato inviato un messaggio ad ogni indirizzo presente nei courtesyaddress per il recipient1
        TestUtils.checkSendCourtesyAddressFromTimeline(iun, recIndex1, listCourtesyAddressRecipient1, timelineService, externalChannelMock);

        //Viene verificato che sia stato inviato un messaggio ad ogni indirizzo presente nei courtesyaddress per il recipient2
        TestUtils.checkSendCourtesyAddressFromTimeline(iun, recIndex2, listCourtesyAddressRecipient2, timelineService, externalChannelMock);

        //Viene verificato l'effettivo invio del messaggio di cortesia verso external channel
        Mockito.verify(externalChannelMock, Mockito.times(listCourtesyAddressRecipient1.size() + listCourtesyAddressRecipient2.size()))
                .sendCourtesyNotification(Mockito.any(NotificationInt.class), Mockito.any(NotificationRecipientInt.class), Mockito.any(CourtesyDigitalAddressInt.class), Mockito.anyString());

        //Viene verificata la presenza degli indirizzi per il primo recipient
        TestUtils.checkGetAddress(iun, recIndex1, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, false, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex1, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificata la presenza degli indirizzi per il secondo recipient
        TestUtils.checkGetAddress(iun, recIndex2, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, false, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        
        //Viene verificata la presenza del primo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito dalla PA per il rec1
        TestUtils.checkSendPaperToExtChannel(iun, recIndex1, paPhysicalAddress1, 0, timelineService);

        //Viene verificata la presenza del primo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito dalla PA per il rec2
        TestUtils.checkSendPaperToExtChannel(iun, recIndex2, paPhysicalAddress2, 0, timelineService);

        //Viene verificato che il workflow sia fallito
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.ANALOG_FAILURE_WORKFLOW.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex1)
                                .build())).isPresent());

        //Viene verificato che il destinatario risulti completamente irraggiungibile
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.COMPLETELY_UNREACHABLE.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex1)
                                .build())).isPresent());
    }


    @Test
    void twoRecipientDigitalDeliveredAnalogUnreachable() {
 /*     PRIMO RECIPIENT
       - Platform address presente e invio con successo (Ottenuto valorizzando il platformAddress in addressBookEntry con ExternalChannelMock.EXT_CHANNEL_WORKS)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
       
       - Indirizzo courtesy message presente, dunque inviato (Ottenuto valorizzando il courtesyAddress del addressBookEntry)
       - Pa physical address presente con struttura indirizzo che porta al fallimento dell'invio tramite external channel (Ottenuto inserendo nell'indirizzo ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR)
         e invio di una seconda notifica (all'indirizzo ottenuto dall'investigazione) con successivo fallimento (ottenuto concatenando all'indirizzo ExternalChannelMock.EXTCHANNEL_SEND_FAIL) 
       - Public Registry Indirizzo fisico non trovato (Ottenuto non valorizzando nessun indirizzo fisico per il recipient in PUB_REGISTRY_PHYSICAL)
        
         SECONDO RECIPIENT
       - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
       
       - Indirizzo courtesy message presente, dunque inviato (Ottenuto valorizzando il courtesyAddress del addressBookEntry)
       - Pa physical address presente con struttura indirizzo che porta al fallimento dell'invio tramite external channel (Ottenuto inserendo nell'indirizzo ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR)
         e invio di una seconda notifica (all'indirizzo ottenuto dall'investigazione) con successivo fallimento (ottenuto concatenando all'indirizzo ExternalChannelMock.EXTCHANNEL_SEND_FAIL) 
       - Public Registry Indirizzo fisico non trovato (Ottenuto non valorizzando nessun indirizzo fisico per il recipient in PUB_REGISTRY_PHYSICAL)
     */

        LegalDigitalAddressInt platformAddress = LegalDigitalAddressInt.builder()
                .address("test@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();
        
        PhysicalAddressInt paPhysicalAddress1 = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR + ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " Via Nuova")
                .build();

        String taxid01 = "TAXID01";
        NotificationRecipientInt recipient1 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxid01)
                .withInternalId("ANON_"+taxid01)
                .withPhysicalAddress(paPhysicalAddress1)
                .build();

        List<CourtesyDigitalAddressInt> listCourtesyAddressRecipient1 = Collections.singletonList(CourtesyDigitalAddressInt.builder()
                .address("test@mail.it")
                .type( CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL )
                .build());

        PhysicalAddressInt paPhysicalAddress2 = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR + ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " Via Nuova")
                .build();

        String taxid02 = "TAXID02";
        NotificationRecipientInt recipient2 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxid02)
                .withInternalId("ANON_"+taxid02)
                .withPhysicalAddress(paPhysicalAddress2)
                .build();

        List<CourtesyDigitalAddressInt> listCourtesyAddressRecipient2 = Collections.singletonList(CourtesyDigitalAddressInt.builder()
                .address("test2@mail.it")
                .type( CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL )
                .build());

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("IUN01")
                .withPaId("paId01")
                .withNotificationRecipient( List.of(recipient1, recipient2) )
                .build();

        pnDeliveryClientMock.addNotification(notification);
        
        addressBookMock.addLegalDigitalAddresses(recipient1.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));
        
        addressBookMock.addCourtesyDigitalAddresses(recipient1.getInternalId(), notification.getSender().getPaId(), listCourtesyAddressRecipient1);
        addressBookMock.addCourtesyDigitalAddresses(recipient2.getInternalId(), notification.getSender().getPaId(), listCourtesyAddressRecipient2);

        String iun = notification.getIun();
        Integer recIndex1 = notificationUtils.getRecipientIndex(notification, recipient1.getTaxId());
        Integer recIndex2 = notificationUtils.getRecipientIndex(notification, recipient2.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che lo stato non passi in EFFECTIVE DATE
        await().untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.EFFECTIVE_DATE, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );
        
        //Viene verificato che sia stato inviato un messaggio ad ogni indirizzo presente nei courtesyaddress
        TestUtils.checkSendCourtesyAddressFromTimeline(iun, recIndex1, listCourtesyAddressRecipient1, timelineService, externalChannelMock);

        //Viene verificata la presenza degli indirizzi per il primo recipient
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificata la presenza degli indirizzi per il secondo recipient
        TestUtils.checkGetAddress(iun, recIndex2, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, false, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato che il workflow sia stato completato con successo per il primo recipient
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.DIGITAL_SUCCESS_WORKFLOW.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex1)
                                .build())
        ).isPresent());
        
        //Viene verificato che il workflow sia fallito per il secondo recipient
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.ANALOG_FAILURE_WORKFLOW.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex2)
                                .build())).isPresent());

        //Viene verificato che il recipient 2 risulti completamente irraggiungibile
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.COMPLETELY_UNREACHABLE.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex2)
                                .build())).isPresent());

        //Viene verificato che sia avvenuto il perfezionamento per entrambi i recipient
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.REFINEMENT.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex1)
                                .build())).isPresent());

        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.REFINEMENT.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex2)
                                .build())).isPresent());
    }


    @Test
    void twoRecipientAnalogUnreachableDigitalDelivered() {
 /*     PRIMO RECIPIENT
           - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
           - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
           - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
           
           - Indirizzo courtesy message presente, dunque inviato (Ottenuto valorizzando il courtesyAddress del addressBookEntry)
           - Pa physical address presente con struttura indirizzo che porta al fallimento dell'invio tramite external channel (Ottenuto inserendo nell'indirizzo ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR)
             e invio di una seconda notifica (all'indirizzo ottenuto dall'investigazione) con successivo fallimento (ottenuto concatenando all'indirizzo ExternalChannelMock.EXTCHANNEL_SEND_FAIL) 
           - Public Registry Indirizzo fisico non trovato (Ottenuto non valorizzando nessun indirizzo fisico per il recipient in PUB_REGISTRY_PHYSICAL)

         SECONDO RECIPIENT
           - Platform address presente e invio con successo (Ottenuto valorizzando il platformAddress in addressBookEntry con ExternalChannelMock.EXT_CHANNEL_WORKS)
           - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
           - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
           
           - Indirizzo courtesy message presente, dunque inviato (Ottenuto valorizzando il courtesyAddress del addressBookEntry)
           - Pa physical address presente con struttura indirizzo che porta al fallimento dell'invio tramite external channel (Ottenuto inserendo nell'indirizzo ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR)
             e invio di una seconda notifica (all'indirizzo ottenuto dall'investigazione) con successivo fallimento (ottenuto concatenando all'indirizzo ExternalChannelMock.EXTCHANNEL_SEND_FAIL) 
           - Public Registry Indirizzo fisico non trovato (Ottenuto non valorizzando nessun indirizzo fisico per il recipient in PUB_REGISTRY_PHYSICAL)

     */

        LegalDigitalAddressInt platformAddress = LegalDigitalAddressInt.builder()
                .address("test@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        PhysicalAddressInt paPhysicalAddress1 = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR + ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " Via Nuova")
                .build();

        String taxid01 = "TAXID01";
        NotificationRecipientInt recipient1 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxid01)
                .withInternalId("ANON_"+taxid01)
                .withPhysicalAddress(paPhysicalAddress1)
                .build();

        List<CourtesyDigitalAddressInt> listCourtesyAddressRecipient1 = Collections.singletonList(CourtesyDigitalAddressInt.builder()
                .address("test@mail.it")
                .type( CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL )
                .build());

        PhysicalAddressInt paPhysicalAddress2 = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR + ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " Via Nuova")
                .build();

        String taxid02 = "TAXID02";
        NotificationRecipientInt recipient2 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxid02)
                .withInternalId("ANON_"+taxid02)
                .withPhysicalAddress(paPhysicalAddress2)
                .build();

        List<CourtesyDigitalAddressInt> listCourtesyAddressRecipient2 = Collections.singletonList(CourtesyDigitalAddressInt.builder()
                .address("test2@mail.it")
                .type( CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL )
                .build());

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("IUN01")
                .withPaId("paId01")
                .withNotificationRecipient( List.of(recipient1, recipient2) )
                .build();

        pnDeliveryClientMock.addNotification(notification);

        addressBookMock.addLegalDigitalAddresses(recipient2.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));

        addressBookMock.addCourtesyDigitalAddresses(recipient1.getInternalId(), notification.getSender().getPaId(), listCourtesyAddressRecipient1);
        addressBookMock.addCourtesyDigitalAddresses(recipient2.getInternalId(), notification.getSender().getPaId(), listCourtesyAddressRecipient2);

        String iun = notification.getIun();
        Integer rec1Index = notificationUtils.getRecipientIndex(notification, recipient1.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che lo stato non passi in EFFECTIVE DATE
        await().untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.EFFECTIVE_DATE, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );
        
        //Viene verificato che sia stato inviato un messaggio ad ogni indirizzo presente nei courtesyaddress
        TestUtils.checkSendCourtesyAddressFromTimeline(iun, rec1Index, listCourtesyAddressRecipient1, timelineService, externalChannelMock);

        //Viene verificato che gli indirizzi PLATFORM SPECIAL E GENERAL non siano presenti
        TestUtils.checkGetAddress(iun, rec1Index, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, rec1Index, false, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, rec1Index, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificata la presenza del primo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito dalla PA
        TestUtils.checkSendPaperToExtChannel(iun, rec1Index, paPhysicalAddress1, 0, timelineService);
        //Viene verificata la presenza del secondo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito dal postino
        //checkSendToExtChannel(iun, TestUtils.PHYSICAL_ADDRESS_FAILURE_BOTH, 1);

        //Viene verificato l'effettivo invio delle due notifiche verso externalChannel
        Mockito.verify(externalChannelMock, Mockito.times(2)).sendAnalogNotification(
                Mockito.any(NotificationInt.class),
                Mockito.any(NotificationRecipientInt.class),
                Mockito.any(PhysicalAddressInt.class),
                Mockito.any(String.class),
                Mockito.any(PhysicalAddressInt.ANALOG_TYPE.class),
                Mockito.any(String.class)
            );

        //Viene verificato che il workflow sia fallito
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.ANALOG_FAILURE_WORKFLOW.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(rec1Index)
                                .build())).isPresent());

        //Viene verificato che il destinatario risulti completamente irraggiungibile
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.COMPLETELY_UNREACHABLE.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(rec1Index)
                                .build())).isPresent());

        //Viene verificato che sia avvenuto il perfezionamento
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.REFINEMENT.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(rec1Index)
                                .build())).isPresent());
    }


    @Test
    void twoRecipientAnalogUnreachableWaitForDigitalDelivered() {
 /*     PRIMO RECIPIENT
           - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
           - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
           - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
           
           - Indirizzo courtesy message presente, dunque inviato (Ottenuto valorizzando il courtesyAddress del addressBookEntry)
           - Pa physical address presente con struttura indirizzo che porta al fallimento dell'invio tramite external channel (Ottenuto inserendo nell'indirizzo ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR)
             e invio di una seconda notifica (all'indirizzo ottenuto dall'investigazione) con successivo fallimento (ottenuto concatenando all'indirizzo ExternalChannelMock.EXTCHANNEL_SEND_FAIL) 
           - Public Registry Indirizzo fisico non trovato (Ottenuto non valorizzando nessun indirizzo fisico per il recipient in PUB_REGISTRY_PHYSICAL)

         SECONDO RECIPIENT
           - Platform address presente e invio con successo (Ottenuto valorizzando il platformAddress in addressBookEntry con ExternalChannelMock.EXT_CHANNEL_WORKS)
           - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
           - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
           
           - Indirizzo courtesy message presente, dunque inviato (Ottenuto valorizzando il courtesyAddress del addressBookEntry)
           - Pa physical address presente con struttura indirizzo che porta al fallimento dell'invio tramite external channel (Ottenuto inserendo nell'indirizzo ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR)
             e invio di una seconda notifica (all'indirizzo ottenuto dall'investigazione) con successivo fallimento (ottenuto concatenando all'indirizzo ExternalChannelMock.EXTCHANNEL_SEND_FAIL) 
           - Public Registry Indirizzo fisico non trovato (Ottenuto non valorizzando nessun indirizzo fisico per il recipient in PUB_REGISTRY_PHYSICAL)

     */

        LegalDigitalAddressInt platformAddress = LegalDigitalAddressInt.builder()
                .address("test@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        PhysicalAddressInt paPhysicalAddress1 = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR + ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " Via Nuova")
                .build();

        String iun = "IUN01";

        //Simulazione attesa del primo recipient in 
        String elementIdInWait = TimelineEventId.SEND_PAPER_FEEDBACK.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(0)
                        .index(1)
                        .build()
        );

        String elementIdToWait = TimelineEventId.DIGITAL_SUCCESS_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(1)
                        .build()
        );
        
        String taxId = TimelineDaoMock.SIMULATE_RECIPIENT_WAIT + elementIdInWait + TimelineDaoMock.WAIT_SEPARATOR + elementIdToWait;
        
        NotificationRecipientInt recipient1 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withInternalId("ANON_"+taxId)
                .withPhysicalAddress(paPhysicalAddress1)
                .build();

        List<CourtesyDigitalAddressInt> listCourtesyAddressRecipient1 = Collections.singletonList(CourtesyDigitalAddressInt.builder()
                .address("test@mail.it")
                .type( CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL )
                .build());

        String taxid02 = "TAXID02";
        NotificationRecipientInt recipient2 = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxid02)
                .withInternalId("ANON_"+taxid02)
                .build();

        List<CourtesyDigitalAddressInt> listCourtesyAddressRecipient2 = Collections.singletonList(CourtesyDigitalAddressInt.builder()
                .address("test2@mail.it")
                .type( CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL )
                .build());

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .withPaId("paId01")
                .withNotificationRecipient( List.of(recipient1, recipient2) )
                .build();

        pnDeliveryClientMock.addNotification(notification);

        addressBookMock.addLegalDigitalAddresses(recipient2.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));

        addressBookMock.addCourtesyDigitalAddresses(recipient1.getInternalId(), notification.getSender().getPaId(), listCourtesyAddressRecipient1);
        addressBookMock.addCourtesyDigitalAddresses(recipient2.getInternalId(), notification.getSender().getPaId(), listCourtesyAddressRecipient2);

        Integer rec1Index = notificationUtils.getRecipientIndex(notification, recipient1.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        // Viene atteso fino a che lo stato non passi in EFFECTIVE DATE
        await().untilAsserted(() ->
                Assertions.assertEquals(NotificationStatusInt.EFFECTIVE_DATE, TestUtils.getNotificationStatus(notification, timelineService, statusUtils))
        );

        //Viene verificato che sia stato inviato un messaggio ad ogni indirizzo presente nei courtesyaddress
        TestUtils.checkSendCourtesyAddressFromTimeline(iun, rec1Index, listCourtesyAddressRecipient1, timelineService, externalChannelMock);

        //Viene verificato che gli indirizzi PLATFORM SPECIAL E GENERAL non siano presenti
        TestUtils.checkGetAddress(iun, rec1Index, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, rec1Index, false, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, rec1Index, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificata la presenza del primo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito dalla PA
        TestUtils.checkSendPaperToExtChannel(iun, rec1Index, paPhysicalAddress1, 0, timelineService);
        //Viene verificata la presenza del secondo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito dal postino
        //checkSendToExtChannel(iun, TestUtils.PHYSICAL_ADDRESS_FAILURE_BOTH, 1);

        //Viene verificato l'effettivo invio delle due notifiche verso externalChannel
        Mockito.verify(externalChannelMock, Mockito.times(2)).sendAnalogNotification(
                Mockito.any(NotificationInt.class),
                Mockito.any(NotificationRecipientInt.class),
                Mockito.any(PhysicalAddressInt.class),
                Mockito.any(String.class),
                Mockito.any(PhysicalAddressInt.ANALOG_TYPE.class),
                Mockito.any(String.class)
        );
        //Viene verificato che il workflow sia fallito
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.ANALOG_FAILURE_WORKFLOW.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(rec1Index)
                                .build())).isPresent());

        //Viene verificato che il destinatario risulti completamente irraggiungibile
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.COMPLETELY_UNREACHABLE.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(rec1Index)
                                .build())).isPresent());

        //Viene verificato che sia avvenuto il perfezionamento
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.REFINEMENT.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(rec1Index)
                                .build())).isPresent());
    }

}
