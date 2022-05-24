package it.pagopa.pn.deliverypush.action2.it;

import it.pagopa.pn.api.dto.events.PnExtChnPecEvent;
import it.pagopa.pn.commons.abstractions.FileData;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypush.action2.*;
import it.pagopa.pn.deliverypush.action2.it.mockbean.*;
import it.pagopa.pn.deliverypush.action2.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action2.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action2.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action2.utils.*;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddressSource;
import it.pagopa.pn.deliverypush.legalfacts.LegalfactsMetadataUtils;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.impl.*;
import it.pagopa.pn.deliverypush.util.StatusUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        StartWorkflowHandler.class,
        AnalogWorkflowHandler.class,
        ChooseDeliveryModeHandler.class,
        DigitalWorkFlowHandler.class,
        CompletionWorkFlowHandler.class,
        PublicRegistryResponseHandler.class,
        PublicRegistrySendHandler.class,
        ExternalChannelSendHandler.class,
        ExternalChannelResponseHandler.class,
        RefinementHandler.class,
        LegalfactsMetadataUtils.class,
        DigitalWorkFlowUtils.class,
        CourtesyMessageUtils.class,
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
        CheckAttachmentUtils.class,
        NotificationUtils.class,
        PaperNotificationFailedDaoMock.class,
        TimelineDaoMock.class,
        ExternalChannelMock.class,
        PaperNotificationFailedDaoMock.class,
        DigitalTestIT.SpringTestConfiguration.class
})
class DigitalTestIT {
    
    
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
    private ExternalChannelMock externalChannelMock;

    @SpyBean
    private CompletionWorkFlowHandler completionWorkflow;
    
    @SpyBean
    private FileStorage fileStorage;
    
    @Autowired
    private PnDeliveryClientMock pnDeliveryClientMock;

    @Autowired
    private UserAttributesMock addressBookMock;

    @Autowired
    private PublicRegistryMock publicRegistryMock;
    
    @Autowired
    private TimelineDaoMock timelineDaoMock;

    @Autowired
    private PaperNotificationFailedDaoMock paperNotificationFailedDaoMock;

    @Autowired
    private NotificationUtils notificationUtils;

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
        PnDeliveryPushConfigs.Webapp webapp = new PnDeliveryPushConfigs.Webapp();
        webapp.setDirectAccessUrlTemplate("test");
        Mockito.when(pnDeliveryPushConfigs.getWebapp()).thenReturn(webapp);

        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());

        FileData fileData = FileData.builder()
                .content( new ByteArrayInputStream("Body".getBytes(StandardCharsets.UTF_8)) )
                .build();

        // Given
        Mockito.when( fileStorage.getFileVersion( Mockito.anyString(), Mockito.anyString()))
                .thenReturn( fileData );

        pnDeliveryClientMock.clear();
        addressBookMock.clear();
        publicRegistryMock.clear();
        timelineDaoMock.clear();
        paperNotificationFailedDaoMock.clear();
    }

    @Test
    void completeFail() throws IdConflictException {
        
        /*
       - Platform address presente e invio fallito per entrambi gli invii (Ottenuto valorizzando il platformAddress in addressBookEntry con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
       - Special address presente e invio fallito per entrambi gli invii (Ottenuto valorizzando il digitalDomicile del recipient con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
       - General address presente e invio fallito per entrambi gli invii (Ottenuto non valorizzando il pbDigitalAddress per il recipient in PUB_REGISTRY_DIGITAL con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
        */

        DigitalAddress platformAddress = DigitalAddress.builder()
                .address("platformAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(DigitalAddress.TypeEnum.PEC)
                .build();

        DigitalAddress digitalDomicile = DigitalAddress.builder()
                .address("digitalDomicile@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(DigitalAddress.TypeEnum.PEC)
                .build();

        DigitalAddress pbDigitalAddress = DigitalAddress.builder()
                .address("pbDigitalAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(DigitalAddress.TypeEnum.PEC)
                .build();

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId("TAXID01")
                .withDigitalDomicile(digitalDomicile)
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("IUN01")
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();
        
        addressBookMock.addLegalDigitalAddresses(recipient.getTaxId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));

        pnDeliveryClientMock.addNotification(notification);
        publicRegistryMock.addDigital(recipient.getTaxId(), pbDigitalAddress);
        
        String iun = notification.getIun();
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);
        
        //Viene verificata la disponibilità degli indirizzi per il primo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSource.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSource.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSource.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        //Viene verificata la disponibilità degli indirizzi per il secondo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSource.PLATFORM, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSource.SPECIAL, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSource.GENERAL, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<PnExtChnPecEvent> pnExtChnPecEventCaptor = ArgumentCaptor.forClass(PnExtChnPecEvent.class);
        Mockito.verify(externalChannelMock, Mockito.times(6)).sendNotification(pnExtChnPecEventCaptor.capture());

        List<PnExtChnPecEvent> sendPecEvent = pnExtChnPecEventCaptor.getAllValues();

        //Viene verificato che il primo tentativo sia avvenuto con il platform address
        TestUtils.checkExternalChannelPecSend(iun, recipient.getTaxId(), sendPecEvent, 0, platformAddress.getAddress());
        //Viene verificato che il secondo tentativo sia avvenuto con il domicilio digitale
        TestUtils.checkExternalChannelPecSend(iun, recipient.getTaxId(), sendPecEvent, 1, digitalDomicile.getAddress());
        //Viene verificato che il secondo tentativo sia avvenuto con l'indirizzo fornito dai registri pubblici
        TestUtils.checkExternalChannelPecSend(iun, recipient.getTaxId(), sendPecEvent, 2, pbDigitalAddress.getAddress());

        //Viene verificato che il quarto tentativo sia avvenuto con il platform address
        TestUtils.checkExternalChannelPecSend(iun, recipient.getTaxId(), sendPecEvent, 3, platformAddress.getAddress());
        //Viene verificato che il quinto tentativo sia avvenuto con il domicilio digitale
        TestUtils.checkExternalChannelPecSend(iun, recipient.getTaxId(), sendPecEvent, 4, digitalDomicile.getAddress());
        //Viene verificato che il sesto tentativo sia avvenuto con l'indirizzo fornito dai registri pubblici
        TestUtils.checkExternalChannelPecSend(iun, recipient.getTaxId(), sendPecEvent, 5, pbDigitalAddress.getAddress());

        //Viene verificato che il workflow abbia avuto successo
        TestUtils.checkFailDigitalWorkflow(iun, recIndex, timelineService, completionWorkflow);

        //Viene verificato che sia avvenuto il perfezionamento
        TestUtils.checkRefinement(iun, recIndex, timelineService);

    }

    @Test
    void emptyFirstSuccessGeneral() throws IdConflictException {
  /*
       - Platform address vuoto (Ottenuto non valorizzando nessun platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando nessun digitalDomicile del recipient)
       - General presente ed primo invio avvenuto con successo (Ottenuto valorizzando il digital address per il recipient in PUB_REGISTRY_DIGITAL con )
    */

        DigitalAddress pbDigitalAddress = DigitalAddress.builder()
                .address("pbDigitalAddress@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(DigitalAddress.TypeEnum.PEC)
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
        publicRegistryMock.addDigital(recipient.getTaxId(), pbDigitalAddress);

        String iun = notification.getIun();
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSource.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSource.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSource.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<PnExtChnPecEvent> pnExtChnPecEventCaptor = ArgumentCaptor.forClass(PnExtChnPecEvent.class);
        Mockito.verify(externalChannelMock, Mockito.times(1)).sendNotification(pnExtChnPecEventCaptor.capture());
        List<PnExtChnPecEvent> sendPecEvent = pnExtChnPecEventCaptor.getAllValues();

        TestUtils.checkExternalChannelPecSend(iun, recipient.getTaxId(), sendPecEvent, 0, pbDigitalAddress.getAddress());

        //Viene verificato che il workflow abbia avuto successo
        TestUtils.checkSuccessDigitalWorkflow(iun, recIndex, timelineService, completionWorkflow, pbDigitalAddress, 1, 0);

        //Viene verificato che sia avvenuto il perfezionamento
        TestUtils.checkRefinement(iun, recIndex, timelineService);

    }

    @Test
    void emptyFirstSuccessSpecial() throws IdConflictException {
  /*
       - Platform address vuoto (Ottenuto non valorizzando nessun platformAddress in addressBookEntry)
       - Special address presente e primo invio con successo (Ottenuto valorizzando il digitalDomicile del recipient con ExternalChannelMock.EXT_CHANNEL_WORKS)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
    */


        final DigitalAddress digitalDomicile = DigitalAddress.builder()
                .address("digitalDomicile@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(DigitalAddress.TypeEnum.PEC)
                .build();

        final NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId("TAXID01")
                .withDigitalDomicile(digitalDomicile)
                .build();

        final NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("IUN01")
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();

        pnDeliveryClientMock.addNotification(notification);

        String iun = notification.getIun();
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSource.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSource.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<PnExtChnPecEvent> pnExtChnPecEventCaptor = ArgumentCaptor.forClass(PnExtChnPecEvent.class);
        Mockito.verify(externalChannelMock, Mockito.times(1)).sendNotification(pnExtChnPecEventCaptor.capture());

        List<PnExtChnPecEvent> sendPecEvent = pnExtChnPecEventCaptor.getAllValues();

        //Viene verificato che il primo tentativo sia avvenuto con il domicilio digitale
        TestUtils.checkExternalChannelPecSend(iun, recipient.getTaxId(), sendPecEvent, 0, digitalDomicile.getAddress());

        //Viene verificato che il workflow abbia avuto successo
        TestUtils.checkSuccessDigitalWorkflow(iun, recIndex, timelineService, completionWorkflow, digitalDomicile, 1, 0);

        //Viene verificato che sia avvenuto il perfezionamento
        TestUtils.checkRefinement(iun, recIndex, timelineService);
    }


    @Test
    void firstSuccessGeneral() throws IdConflictException {
  /*
       - Platform address presente e primo invio con fallimento (Ottenuto valorizzando il platformAddress in addressBookEntry con ExternalChannelMock.EXT_CHANNEL_WORKS)
       - Special address presente e primo invio con fallimento (Ottenuto valorizzando il digitalDomicile del recipient con ExternalChannelMock.EXT_CHANNEL_WORKS)
       - General address presente e primo invio con successo (Ottenuto valorizzando digital address per il recipient in ExternalChannelMock.EXT_CHANNEL_WORKS)
    */
        final DigitalAddress platformAddress = DigitalAddress.builder()
                .address("test@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
                .type(DigitalAddress.TypeEnum.PEC)
                .build();

        final DigitalAddress digitalDomicile = DigitalAddress.builder()
                .address("digitalDomicile@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
                .type(DigitalAddress.TypeEnum.PEC)
                .build();

        final DigitalAddress pbDigitalAddress = DigitalAddress.builder()
                .address("pbDigitalAddress@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(DigitalAddress.TypeEnum.PEC)
                .build();

        final NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId("TAXID01")
                .withDigitalDomicile(digitalDomicile)
                .build();

        final NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("IUN01")
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();

        addressBookMock.addLegalDigitalAddresses(recipient.getTaxId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));
        pnDeliveryClientMock.addNotification(notification);
        publicRegistryMock.addDigital(recipient.getTaxId(), pbDigitalAddress);

        String iun = notification.getIun();
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSource.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSource.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSource.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<PnExtChnPecEvent> pnExtChnPecEventCaptor = ArgumentCaptor.forClass(PnExtChnPecEvent.class);
        Mockito.verify(externalChannelMock, Mockito.times(3)).sendNotification(pnExtChnPecEventCaptor.capture());

        List<PnExtChnPecEvent> sendPecEvent = pnExtChnPecEventCaptor.getAllValues();

        //Viene verificato che il primo tentativo sia avvenuto con il platform address
        TestUtils.checkExternalChannelPecSend(iun, recipient.getTaxId(), sendPecEvent, 0, platformAddress.getAddress());
        //Viene verificato che il secondo tentativo sia avvenuto con il domicilio digitale
        TestUtils.checkExternalChannelPecSend(iun, recipient.getTaxId(), sendPecEvent, 1, digitalDomicile.getAddress());
        TestUtils.checkExternalChannelPecSend(iun, recipient.getTaxId(), sendPecEvent, 2, pbDigitalAddress.getAddress());

        //Viene verificato che il workflow abbia avuto successo
        TestUtils.checkSuccessDigitalWorkflow(iun, recIndex, timelineService, completionWorkflow, pbDigitalAddress, 1, 0);

        //Viene verificato che sia avvenuto il perfezionamento
        TestUtils.checkRefinement(iun, recIndex, timelineService);
    }

    @Test
    void firstSuccessPlatform() throws IdConflictException {
     /*
       - Platform address presente e invio con successo (Ottenuto valorizzando il platformAddress in addressBookEntry con ExternalChannelMock.EXT_CHANNEL_WORKS)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
    */
        DigitalAddress platformAddress = DigitalAddress.builder()
                .address("test@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(DigitalAddress.TypeEnum.PEC)
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
        addressBookMock.addLegalDigitalAddresses(recipient.getTaxId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));

        String iun = notification.getIun();
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        //Viene verificata la presenza dell'indirizzo di piattaforma
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSource.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        //Viene verificato che sia stata effettuata una sola chiamata ad external channel
        Mockito.verify(externalChannelMock, Mockito.times(1)).sendNotification(Mockito.any(PnExtChnPecEvent.class));

        //Viene verificato che il workflow abbia avuto successo
        TestUtils.checkSuccessDigitalWorkflow(iun, recIndex, timelineService, completionWorkflow, platformAddress, 1, 0);

        //Viene verificato che sia avvenuto il perfezionamento
        TestUtils.checkRefinement(iun, recIndex, timelineService);
    }

    @Test
    void firstSuccessSpecial() throws IdConflictException {
        /*
       - Platform address presente e primo invio con fallimento (Ottenuto valorizzando il platformAddress in addressBookEntry con ExternalChannelMock.EXT_CHANNEL_WORKS)
       - Special address presente e primo invio con successo (Ottenuto valorizzando il digitalDomicile del recipient con ExternalChannelMock.EXT_CHANNEL_WORKS)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
    */
        DigitalAddress platformAddress = DigitalAddress.builder()
                .address("test@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
                .type(DigitalAddress.TypeEnum.PEC)
                .build();

        DigitalAddress digitalDomicile = DigitalAddress.builder()
                .address("digitalDomicile@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(DigitalAddress.TypeEnum.PEC)
                .build();

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId("TAXID01")
                .withDigitalDomicile(digitalDomicile)
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("IUN01")
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient.getTaxId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));

        String iun = notification.getIun();
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSource.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSource.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<PnExtChnPecEvent> pnExtChnPecEventCaptor = ArgumentCaptor.forClass(PnExtChnPecEvent.class);
        Mockito.verify(externalChannelMock, Mockito.times(2)).sendNotification(pnExtChnPecEventCaptor.capture());

        List<PnExtChnPecEvent> sendPecEvent = pnExtChnPecEventCaptor.getAllValues();

        //Viene verificato che il primo tentativo sia avvenuto con il platform address
        TestUtils.checkExternalChannelPecSend(iun, recipient.getTaxId(), sendPecEvent, 0, platformAddress.getAddress());
        //Viene verificato che il secondo tentativo sia avvenuto con il domicilio digitale
        TestUtils.checkExternalChannelPecSend(iun, recipient.getTaxId(), sendPecEvent, 1, digitalDomicile.getAddress());

        //Viene verificato che il workflow abbia avuto successo
        TestUtils.checkSuccessDigitalWorkflow(iun, recIndex, timelineService, completionWorkflow, digitalDomicile, 1, 0);

        //Viene verificato che sia avvenuto il perfezionamento
        TestUtils.checkRefinement(iun, recIndex, timelineService);
    }

    @Test
    void secondSuccessGeneral() throws IdConflictException {
       /*
       - Platform address presente fallimento sia primo che secondo tentativo (Ottenuto valorizzando il platformAddress in addressBookEntry con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
       - Special address presente fallimento sia primo che secondo tentativo (Ottenuto valorizzando il digitaldomicile con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
       - General address successo (Ottenuto valorizzando il digitaladdress con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST per il recipient in PUB_REGISTRY_DIGITAL)
    */
        DigitalAddress platformAddress = DigitalAddress.builder()
                .address("test@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(DigitalAddress.TypeEnum.PEC)
                .build();


        DigitalAddress digitalDomicile = DigitalAddress.builder()
                .address("digitalDomicile@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(DigitalAddress.TypeEnum.PEC)
                .build();

        DigitalAddress pbDigitalAddress = DigitalAddress.builder()
                .address("pbDigitalAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
                .type(DigitalAddress.TypeEnum.PEC)
                .build();

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId("TAXID01")
                .withDigitalDomicile(digitalDomicile)
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("IUN01")
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient.getTaxId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));
        publicRegistryMock.addDigital(recipient.getTaxId(), pbDigitalAddress);

        String iun = notification.getIun();
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSource.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSource.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSource.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        //Viene verificata la disponibilità degli indirizzi per il secondo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSource.PLATFORM, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSource.SPECIAL, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSource.GENERAL, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<PnExtChnPecEvent> pnExtChnPecEventCaptor = ArgumentCaptor.forClass(PnExtChnPecEvent.class);
        Mockito.verify(externalChannelMock, Mockito.times(6)).sendNotification(pnExtChnPecEventCaptor.capture());

        List<PnExtChnPecEvent> sendPecEvent = pnExtChnPecEventCaptor.getAllValues();

        //Viene verificato che il primo tentativo sia avvenuto con il platform address
        TestUtils.checkExternalChannelPecSend(iun, recipient.getTaxId(), sendPecEvent, 0, platformAddress.getAddress());
        //Viene verificato che il secondo tentativo sia avvenuto con il domicilio digitale
        TestUtils.checkExternalChannelPecSend(iun, recipient.getTaxId(), sendPecEvent, 1, digitalDomicile.getAddress());
        //Viene verificato che il secondo tentativo sia avvenuto con l'indirizzo fornito dai registri pubblici
        TestUtils.checkExternalChannelPecSend(iun, recipient.getTaxId(), sendPecEvent, 2, pbDigitalAddress.getAddress());
        //Viene verificato che il quarto tentativo sia avvenuto con il platform address
        TestUtils.checkExternalChannelPecSend(iun, recipient.getTaxId(), sendPecEvent, 3, platformAddress.getAddress());
        //Viene verificato che il quinto tentativo sia avvenuto con il domicilio digitale
        TestUtils.checkExternalChannelPecSend(iun, recipient.getTaxId(), sendPecEvent, 4, digitalDomicile.getAddress());
        //Viene verificato che il sesto tentativo sia avvenuto con l'indirizzo fornito dai registri pubblici
        TestUtils.checkExternalChannelPecSend(iun, recipient.getTaxId(), sendPecEvent, 5, pbDigitalAddress.getAddress());

        //Viene verificato che il workflow abbia avuto successo
        TestUtils.checkSuccessDigitalWorkflow(iun, recIndex, timelineService, completionWorkflow, pbDigitalAddress, 1, 0);

        //Viene verificato che sia avvenuto il perfezionamento
        TestUtils.checkRefinement(iun, recIndex, timelineService);
    }

    @Test
    void secondSuccessPlatform() {

        pnDeliveryClientMock.clear();
        addressBookMock.clear();
        publicRegistryMock.clear();

        /*
       - Platform address presente e fallimento primo tentativo e successo secondo (Ottenuto valorizzando il platformAddress in addressBookEntry con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
       - Special address presente e fallimento primo tentativo (Ottenuto valorizzando il digitaldomicile con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
       - General address presente e fallimento primo tentativo (Ottenuto non valorizzando il digitaladdress con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST  per il recipient in PUB_REGISTRY_DIGITAL)
    */
        
        DigitalAddress platformAddress = DigitalAddress.builder()
                .address("test@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
                .type(DigitalAddress.TypeEnum.PEC)
                .build();

        DigitalAddress digitalDomicile = DigitalAddress.builder()
                .address("digitalDomicile@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
                .type(DigitalAddress.TypeEnum.PEC)
                .build();

        DigitalAddress pbDigitalAddress = DigitalAddress.builder()
                .address("pbDigitalAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
                .type(DigitalAddress.TypeEnum.PEC)
                .build();

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId("TAXID01")
                .withDigitalDomicile(digitalDomicile)
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("IUN01")
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient.getTaxId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));
        publicRegistryMock.addDigital(recipient.getTaxId(), pbDigitalAddress);

        String iun = notification.getIun();
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSource.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSource.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSource.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        //Viene verificata la disponibilità degli indirizzi per il secondo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSource.PLATFORM, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<PnExtChnPecEvent> pnExtChnPecEventCaptor = ArgumentCaptor.forClass(PnExtChnPecEvent.class);
        Mockito.verify(externalChannelMock, Mockito.times(4)).sendNotification(pnExtChnPecEventCaptor.capture());

        List<PnExtChnPecEvent> sendPecEvent = pnExtChnPecEventCaptor.getAllValues();

        //Viene verificato che il primo tentativo sia avvenuto con il platform address
        TestUtils.checkExternalChannelPecSend(iun, recipient.getTaxId(), sendPecEvent, 0, platformAddress.getAddress());
        //Viene verificato che il secondo tentativo sia avvenuto con il domicilio digitale
        TestUtils.checkExternalChannelPecSend(iun, recipient.getTaxId(), sendPecEvent, 1, digitalDomicile.getAddress());
        //Viene verificato che il secondo tentativo sia avvenuto con l'indirizzo fornito dai registri pubblici
        TestUtils.checkExternalChannelPecSend(iun, recipient.getTaxId(), sendPecEvent, 2, pbDigitalAddress.getAddress());
        //Viene verificato che il quarto tentativo sia avvenuto con il platform address
        TestUtils.checkExternalChannelPecSend(iun, recipient.getTaxId(), sendPecEvent, 3, platformAddress.getAddress());

        //Viene verificato che il workflow abbia avuto successo
        TestUtils.checkSuccessDigitalWorkflow(iun, recIndex, timelineService, completionWorkflow, platformAddress, 1, 0);

        //Viene verificato che sia avvenuto il perfezionamento
        TestUtils.checkRefinement(iun, recIndex, timelineService);
    }

    @Test
    void secondSuccessSpecial() {
      /*
       - Platform address presente sia primo che secondo tentativo (Ottenuto valorizzando il platformAddress in addressBookEntry con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
       - Special address presente fallimento primo tentativo successo secondo tentativo (Ottenuto valorizzando il digitaldomicile con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
       - General address presente fallimento primo tentativo (Ottenuto valorizzando il digitaladdress con ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST  per il recipient in PUB_REGISTRY_DIGITAL)
    */
        DigitalAddress platformAddress = DigitalAddress.builder()
                .address("test@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(DigitalAddress.TypeEnum.PEC)
                .build();

        DigitalAddress digitalDomicile = DigitalAddress.builder()
                .address("digitalDomicile@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
                .type(DigitalAddress.TypeEnum.PEC)
                .build();

        DigitalAddress pbDigitalAddress = DigitalAddress.builder()
                .address("pbDigitalAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
                .type(DigitalAddress.TypeEnum.PEC)
                .build();

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId("TAXID01")
                .withDigitalDomicile(digitalDomicile)
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("IUN01")
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();
        
        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient.getTaxId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));
        publicRegistryMock.addDigital(recipient.getTaxId(), pbDigitalAddress);

        String iun = notification.getIun();
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSource.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSource.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSource.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        //Viene verificata la disponibilità degli indirizzi per il secondo tentativo
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSource.PLATFORM, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSource.SPECIAL, ChooseDeliveryModeUtils.ONE_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<PnExtChnPecEvent> pnExtChnPecEventCaptor = ArgumentCaptor.forClass(PnExtChnPecEvent.class);
        Mockito.verify(externalChannelMock, Mockito.times(5)).sendNotification(pnExtChnPecEventCaptor.capture());

        List<PnExtChnPecEvent> sendPecEvent = pnExtChnPecEventCaptor.getAllValues();

        //Viene verificato che il primo tentativo sia avvenuto con il platform address
        TestUtils.checkExternalChannelPecSend(iun, recipient.getTaxId(), sendPecEvent, 0, platformAddress.getAddress());
        //Viene verificato che il secondo tentativo sia avvenuto con il domicilio digitale
        TestUtils.checkExternalChannelPecSend(iun, recipient.getTaxId(), sendPecEvent, 1, digitalDomicile.getAddress());
        //Viene verificato che il secondo tentativo sia avvenuto con l'indirizzo fornito dai registri pubblici
        TestUtils.checkExternalChannelPecSend(iun, recipient.getTaxId(), sendPecEvent, 2, pbDigitalAddress.getAddress());
        //Viene verificato che il quarto tentativo sia avvenuto con il platform address
        TestUtils.checkExternalChannelPecSend(iun, recipient.getTaxId(), sendPecEvent, 3, platformAddress.getAddress());
        //Viene verificato che il quinto tentativo sia avvenuto con il domicilio digitale
        TestUtils.checkExternalChannelPecSend(iun, recipient.getTaxId(), sendPecEvent, 4, digitalDomicile.getAddress());

        //Viene verificato che il workflow abbia avuto successo
        TestUtils.checkSuccessDigitalWorkflow(iun, recIndex, timelineService, completionWorkflow, digitalDomicile, 1, 0);

        //Viene verificato che sia avvenuto il perfezionamento
        TestUtils.checkRefinement(iun, recIndex, timelineService);
    }


    @Test
    void twoRecipient() throws IdConflictException {
       /* Primo recipient
       - Platform address presente e primo invio con fallimento
       - Special address presente e primo invio con successo
       - General address vuoto (Ottenuto non valorizzando
       
       Secondo recipient
       - Platform address presente ed entrambi gli invii con fallimento
       - Special address presente e primo invio con fallimento ma secondo con successo 
       - General address vuoto
    */

        //Primo Recipient
        DigitalAddress platformAddress1 = DigitalAddress.builder()
                .address("test1@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
                .type(DigitalAddress.TypeEnum.PEC)
                .build();

        DigitalAddress digitalDomicile1 = DigitalAddress.builder()
                .address("digitalDomicile1@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(DigitalAddress.TypeEnum.PEC)
                .build();

        NotificationRecipientInt recipient1 = NotificationRecipientTestBuilder.builder()
                .withTaxId("TAXID01")
                .withDigitalDomicile(digitalDomicile1)
                .build();

        //Secondo recipient
        DigitalAddress platformAddress2 = DigitalAddress.builder()
                .address("test2@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(DigitalAddress.TypeEnum.PEC)
                .build();

        DigitalAddress digitalDomicile2 = DigitalAddress.builder()
                .address("digitalDomicile2@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_FIRST)
                .type(DigitalAddress.TypeEnum.PEC)
                .build();

        NotificationRecipientInt recipient2 = NotificationRecipientTestBuilder.builder()
                .withTaxId("TAXID02")
                .withDigitalDomicile(digitalDomicile2)
                .build();
        
        List<NotificationRecipientInt> recipients = new ArrayList<>();

        recipients.add(recipient1);
        recipients.add(recipient2);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("IUN01")
                .withPaId("paId01")
                .withNotificationRecipients(recipients)
                .build();

        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient1.getTaxId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress1));
        addressBookMock.addLegalDigitalAddresses(recipient2.getTaxId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress2));

        String iun = notification.getIun();
        Integer recIndex1 = notificationUtils.getRecipientIndex(notification, recipient1.getTaxId());
        Integer recIndex2 = notificationUtils.getRecipientIndex(notification, recipient2.getTaxId());
        

        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);

        //Viene verificata la disponibilità degli indirizzi per il primo tentativo per il primo recipient
        TestUtils.checkGetAddress(iun, recIndex1, true, DigitalAddressSource.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex2, true, DigitalAddressSource.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato il numero di send PEC verso external channel
        ArgumentCaptor<PnExtChnPecEvent> pnExtChnPecEventCaptor = ArgumentCaptor.forClass(PnExtChnPecEvent.class);
        Mockito.verify(externalChannelMock, Mockito.times(6)).sendNotification(pnExtChnPecEventCaptor.capture());

        List<PnExtChnPecEvent> sendPecEvent = pnExtChnPecEventCaptor.getAllValues();

        //Viene verificato per il primo recipient che il primo tentativo sia avvenuto con il platform address
        TestUtils.checkExternalChannelPecSend(iun, recipient1.getTaxId(), sendPecEvent, 0, platformAddress1.getAddress());
        //Viene verificato per il primo recipient che il secondo tentativo sia avvenuto con il domicilio digitale
        TestUtils.checkExternalChannelPecSend(iun, recipient1.getTaxId(), sendPecEvent, 1, digitalDomicile1.getAddress());

        //Viene verificato per il primo recipient che il workflow abbia avuto successo
        TestUtils.checkSuccessDigitalWorkflow(iun, recIndex1, timelineService, completionWorkflow, digitalDomicile1, 2, 0);

        //Viene verificato per il primo recipient che sia avvenuto il perfezionamento
        TestUtils.checkRefinement(iun, recIndex1, timelineService);

        //Viene verificato per il secondo recipient che il primo tentativo sia avvenuto con il platform address
        TestUtils.checkExternalChannelPecSend(iun, recipient2.getTaxId(), sendPecEvent, 2, platformAddress2.getAddress());
        //Viene verificato per il secondo recipient che il secondo tentativo sia avvenuto con il domicilio digitale
        TestUtils.checkExternalChannelPecSend(iun, recipient2.getTaxId(), sendPecEvent, 3, digitalDomicile2.getAddress());
        //Viene verificato per il secondo recipient che il terzo tentativo sia avvenuto con il platform address
        TestUtils.checkExternalChannelPecSend(iun, recipient2.getTaxId(), sendPecEvent, 2, platformAddress2.getAddress());
        //Viene verificato per il secondo recipient che il quarto tentativo sia avvenuto con il domicilio digitale
        TestUtils.checkExternalChannelPecSend(iun, recipient2.getTaxId(), sendPecEvent, 3, digitalDomicile2.getAddress());

        //Viene verificato per il secondo recipient che il workflow abbia avuto successo
        TestUtils.checkSuccessDigitalWorkflow(iun, recIndex2, timelineService, completionWorkflow, digitalDomicile2, 2, 1);

        //Viene verificato per il secondo recipient che sia avvenuto il perfezionamento
        TestUtils.checkRefinement(iun, recIndex2, timelineService);
    }
}
