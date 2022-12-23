package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowUtils;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.action.startworkflow.AttachmentUtils;
import it.pagopa.pn.deliverypush.action.utils.*;
import it.pagopa.pn.deliverypush.dto.address.*;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.ServiceLevelTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.EventCodeInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarGenerationDetailsInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelSendClient;
import it.pagopa.pn.deliverypush.service.PaperChannelService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.eq;

class PaperChannelServiceImplTest {
    @Mock
    private PaperChannelUtils paperChannelUtils;
    @Mock
    private PaperChannelSendClient paperChannelSendClient;
    @Mock
    private NotificationUtils notificationUtils;
    @Mock
    private AarUtils aarUtils;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private MVPParameterConsumer mvpParameterConsumer;
    @Mock
    private AnalogWorkflowUtils analogWorkflowUtils;
    @Mock
    private AttachmentUtils attachmentUtils;


    private PaperChannelService paperChannelService;

    @BeforeEach
    void setup() {
        paperChannelService = new PaperChannelServiceImpl(
                paperChannelUtils,
                paperChannelSendClient,
                notificationUtils,
                aarUtils,
                timelineUtils,
                mvpParameterConsumer,
                analogWorkflowUtils,
                attachmentUtils);
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationForSimpleRegisteredLetter() {
        //GIVEN
        NotificationInt notificationInt = newNotification("taxid");
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        Mockito.when(timelineUtils.checkNotificationIsAlreadyViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(aarUtils.getAarGenerationDetails(Mockito.any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);

        // WHEN
        paperChannelService.prepareAnalogNotificationForSimpleRegisteredLetter(notificationInt, 0);

        // THEN
        Mockito.verify(paperChannelSendClient).prepare(Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationForSimpleRegisteredLetterAlreadyviewed() {
        //GIVEN
        NotificationInt notificationInt = newNotification("taxid");
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        Mockito.when(timelineUtils.checkNotificationIsAlreadyViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);

        // WHEN
        paperChannelService.prepareAnalogNotificationForSimpleRegisteredLetter(notificationInt, 0);

        // THEN
        Mockito.verify(paperChannelSendClient,Mockito.never()).prepare(Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotification() {
        //GIVEN
        NotificationInt notificationInt = newNotification("taxid");
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        Mockito.when(timelineUtils.checkNotificationIsAlreadyViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(aarUtils.getAarGenerationDetails(Mockito.any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);

        // WHEN
        paperChannelService.prepareAnalogNotificationForSimpleRegisteredLetter(notificationInt, 0);

        // THEN
        Mockito.verify(paperChannelSendClient).prepare(Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationAlreadyviewed() {
        //GIVEN
        NotificationInt notificationInt = newNotification("taxid");
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        Mockito.when(timelineUtils.checkNotificationIsAlreadyViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);

        // WHEN
        paperChannelService.prepareAnalogNotificationForSimpleRegisteredLetter(notificationInt, 0);

        // THEN
        Mockito.verify(paperChannelSendClient, Mockito.never()).prepare(Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendSimpleRegisteredLetter() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification("taxid");
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        Mockito.when(timelineUtils.checkNotificationIsAlreadyViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(aarUtils.getAarGenerationDetails(Mockito.any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);

        // WHEN
        paperChannelService.sendSimpleRegisteredLetter(notificationInt, 0, "req123", physicalAddressInt, "NR_SR");

        // THEN
        Mockito.verify(paperChannelSendClient).send(Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendSimpleRegisteredLetterAlreadyViewed() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification("taxid");
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        Mockito.when(timelineUtils.checkNotificationIsAlreadyViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);

        // WHEN
        paperChannelService.sendSimpleRegisteredLetter(notificationInt, 0, "req123", physicalAddressInt, "NR_SR");

        // THEN
        Mockito.verify(paperChannelSendClient, Mockito.never()).send(Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendAnalogNotification() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification("taxid");
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        Mockito.when(timelineUtils.checkNotificationIsAlreadyViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(aarUtils.getAarGenerationDetails(Mockito.any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);

        // WHEN
        paperChannelService.sendAnalogNotification(notificationInt, 0, 0, "req123", physicalAddressInt, "NR_SR");

        // THEN
        Mockito.verify(paperChannelSendClient).send(Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendAnalogNotificationAlreadyViewed() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification("taxid");
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        Mockito.when(timelineUtils.checkNotificationIsAlreadyViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);

        // WHEN
        paperChannelService.sendAnalogNotification(notificationInt, 0, 0, "req123", physicalAddressInt, "NR_SR");

        // THEN
        Mockito.verify(paperChannelSendClient, Mockito.never()).send(Mockito.any());
    }

    private NotificationInt newNotification(String TAX_ID) {
        return NotificationInt.builder()
                .iun("IUN_01")
                .paProtocolNumber("protocol_01")
                .sender(NotificationSenderInt.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipientInt.builder()
                                .taxId(TAX_ID)
                                .internalId(TAX_ID + "ANON")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(LegalDigitalAddressInt.builder()
                                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .build();
    }
/*
    @ExtendWith(MockitoExtension.class)
    @Test
    void sendDigitalNotification() {
        //GIVEN
        String iun = "IUN01";
        String taxId = "taxId";

        LegalDigitalAddressInt digitalDomicile = LegalDigitalAddressInt.builder()
                .address("digitalDomicile@test.it")
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();
        
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withInternalId("ANON_"+taxId)
                .withDigitalDomicile(digitalDomicile)
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress("_Via Nuova")
                                .build()
                )
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();
        
        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);
        
        DigitalAddressSourceInt addressSource = DigitalAddressSourceInt.PLATFORM;
        int recIndex = 0;
        int sentAttemptMade = 0;

        //WHEN        
        paperChannelService.sendDigitalNotification(notification, digitalDomicile, addressSource, recIndex, sentAttemptMade, false);
        
        //THEN
        String eventIdExpected = TimelineEventId.SEND_DIGITAL_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .source(addressSource)
                        .sentAttemptMade(sentAttemptMade)
                        .build()
        );
        
        Mockito.verify(externalChannel).sendLegalNotification(notification, recipient,  digitalDomicile, eventIdExpected);
        Mockito.verify(externalChannelUtils).addSendDigitalNotificationToTimeline(notification, digitalDomicile, addressSource, recIndex, sentAttemptMade, eventIdExpected);
    }


    @AfterAll
    static void afterAll() {
        
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendDigitalNotification_AlreadyInProgress() {
        //GIVEN
        String iun = "IUN01";
        String taxId = "taxId";

        LegalDigitalAddressInt digitalDomicile = LegalDigitalAddressInt.builder()
                .address("digitalDomicile@test.it")
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withInternalId("ANON_"+taxId)
                .withDigitalDomicile(digitalDomicile)
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress("_Via Nuova")
                                .build()
                )
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();

        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);

        DigitalAddressSourceInt addressSource = DigitalAddressSourceInt.PLATFORM;
        int recIndex = 0;
        int sentAttemptMade = 0;

        //WHEN
        paperChannelService.sendDigitalNotification(notification, digitalDomicile, addressSource, recIndex, sentAttemptMade, true);

        //THEN
        String eventIdExpected = TimelineEventId.SEND_DIGITAL_PROGRESS.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .source(addressSource)
                        .index(sentAttemptMade)
                        .progressIndex(1)
                        .build()
        );

        Mockito.verify(externalChannel).sendLegalNotification(notification, recipient,  digitalDomicile, eventIdExpected);
        
        Mockito.verify(digitalWorkFlowUtils).addDigitalDeliveringProgressTimelineElement(
                eq(notification),
                eq(EventCodeInt.DP00),
                        eq(recIndex),
                                eq(false),
                                        eq(null),
                                                Mockito.any(DigitalAddressFeedback.class)
        );
        
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendCourtesyNotification() {
        //GIVEN
        String iun = "IUN01";
        String taxId = "taxId";

        CourtesyDigitalAddressInt courtesyDigitalAddress = CourtesyDigitalAddressInt.builder()
                .address("courtesyDigitalAddress@test.it")
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL)
                .build();

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withInternalId("ANON_"+taxId)
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress("_Via Nuova")
                                .build()
                )
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .withPaId("paId01")
                .build();

        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);

        int recIndex = 0;
        String eventId = "eventId";
        
        //WHEN
        paperChannelService.sendCourtesyNotification(notification, courtesyDigitalAddress, recIndex, eventId);

        //THEN
        Mockito.verify(externalChannel).sendCourtesyNotification(notification, recipient,  courtesyDigitalAddress, eventId);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendNotificationForRegisteredLetterNotViewed() {
        //GIVEN
        String iun = "IUN01";
        String taxId = "taxId";

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withInternalId("ANON_"+taxId)
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress("_Via Nuova")
                                .build()
                )
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();

        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);
        
        Mockito.when( timelineUtils.checkNotificationIsAlreadyViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);

        AarGenerationDetailsInt aarGenerationDetailsInt = AarGenerationDetailsInt.builder()
                .numberOfPages(1)
                .generatedAarUrl("testUrl")
                .build();
        
        Mockito.when(aarUtils.getAarGenerationDetails(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(aarGenerationDetailsInt);
                
        int recIndex = 0;

        //WHEN        
        paperChannelService.sendNotificationForRegisteredLetter(notification, recipient.getPhysicalAddress(), recIndex);

        //THEN
        String eventIdExpected = TimelineEventId.SEND_SIMPLE_REGISTERED_LETTER.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build()
        );
        
        Mockito.verify(externalChannel).sendAnalogNotification(
                notification, recipient, recipient.getPhysicalAddress(),  eventIdExpected, PhysicalAddressInt.ANALOG_TYPE.SIMPLE_REGISTERED_LETTER,
                aarGenerationDetailsInt.getGeneratedAarUrl()
        );
        Mockito.verify(externalChannelUtils).addSendSimpleRegisteredLetterToTimeline(
                notification,  recipient.getPhysicalAddress(), recIndex, eventIdExpected, aarGenerationDetailsInt.getNumberOfPages()
        );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendNotificationForRegisteredLetterViewed() {
        //GIVEN
        String iun = "IUN01";
        String taxId = "taxId";

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withInternalId("ANON_"+taxId)
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress("_Via Nuova")
                                .build()
                )
                .build();
        
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();
        
        Mockito.when( timelineUtils.checkNotificationIsAlreadyViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);
        
        int recIndex = 0;

        //WHEN        
        paperChannelService.sendNotificationForRegisteredLetter(notification, recipient.getPhysicalAddress(), recIndex);

        //THEN
        Mockito.verify(externalChannel, Mockito.times(0)).sendAnalogNotification(
                Mockito.any(), Mockito.any(), Mockito.any(),  Mockito.any(), Mockito.any(),  Mockito.any()
        );
        Mockito.verify(externalChannelUtils,  Mockito.times(0)).addSendSimpleRegisteredLetterToTimeline(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()
        );
    }
    
    @ExtendWith(MockitoExtension.class)
    @Test
    void sendAnalogNotificationNotViewedHandled() {
        //GIVEN
        String iun = "IUN01";
        String taxId = "taxId";

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withInternalId("ANON_"+taxId)
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress("_Via Nuova")
                                .build()
                )
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();

        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);
        
        Mockito.when( timelineUtils.checkNotificationIsAlreadyViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(mvpParameterConsumer.isMvp(Mockito.anyString())).thenReturn(false);
         
        AarGenerationDetailsInt aarGenerationDetailsInt = AarGenerationDetailsInt.builder()
                .numberOfPages(1)
                .generatedAarUrl("testUrl")
                .build();

        Mockito.when(aarUtils.getAarGenerationDetails(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(aarGenerationDetailsInt);

        int recIndex = 0;
        boolean investigation = false;
        int sentAttemptMade = 0;
        
        //WHEN                 
        paperChannelService.sendAnalogNotification(notification, recipient.getPhysicalAddress(), recIndex, investigation, sentAttemptMade );

        //THEN
        String eventIdExpected = TimelineEventId.SEND_ANALOG_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .sentAttemptMade(sentAttemptMade)
                        .build()
        );
        
        Mockito.verify(externalChannel).sendAnalogNotification(
                notification, recipient, recipient.getPhysicalAddress(),  eventIdExpected, 
                notification.getPhysicalCommunicationType()== ServiceLevelTypeInt.REGISTERED_LETTER_890 ? PhysicalAddressInt.ANALOG_TYPE.REGISTERED_LETTER_890 : PhysicalAddressInt.ANALOG_TYPE.AR_REGISTERED_LETTER,
                aarGenerationDetailsInt.getGeneratedAarUrl()
        );
        
        Mockito.verify(externalChannelUtils).addSendAnalogNotificationToTimeline(
                notification,  recipient.getPhysicalAddress(), recIndex, investigation, sentAttemptMade, eventIdExpected, aarGenerationDetailsInt.getNumberOfPages()
        );

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendAnalogNotificationNotViewedNotHandled() {
        //GIVEN
        String iun = "IUN01";
        String taxId = "taxId";

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withInternalId("ANON_"+taxId)
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress("_Via Nuova")
                                .build()
                )
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();


        Mockito.when( timelineUtils.checkNotificationIsAlreadyViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(mvpParameterConsumer.isMvp(Mockito.anyString())).thenReturn(true);
        
        int recIndex = 0;
        boolean investigation = false;
        int sentAttemptMade = 0;

        //WHEN                 
        paperChannelService.sendAnalogNotification(notification, recipient.getPhysicalAddress(), recIndex, investigation, sentAttemptMade );

        //THEN

        Mockito.verify(externalChannelUtils).addPaperNotificationNotHandledToTimeline(notification, recIndex);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendAnalogNotificationViewed() {
        //GIVEN
        String iun = "IUN01";
        String taxId = "taxId";

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withInternalId("ANON_"+taxId)
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress("_Via Nuova")
                                .build()
                )
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();


        Mockito.when( timelineUtils.checkNotificationIsAlreadyViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);

        int recIndex = 0;
        boolean investigation = false;
        int sentAttemptMade = 0;

        //WHEN                 
        paperChannelService.sendAnalogNotification(notification, recipient.getPhysicalAddress(), recIndex, investigation, sentAttemptMade );

        //THEN
        Mockito.verify(externalChannelUtils, Mockito.times(0)).addPaperNotificationNotHandledToTimeline(Mockito.any(), Mockito.any());

        Mockito.verify(externalChannel, Mockito.times(0)).sendAnalogNotification(
                Mockito.any(), Mockito.any(), Mockito.any(),  Mockito.any(), Mockito.any(), Mockito.any()
        );

    }

 */
}