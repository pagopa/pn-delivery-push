package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowUtils;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.action.utils.ExternalChannelUtils;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressFeedback;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.EventCodeInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalchannel.ExternalChannelSendClient;
import it.pagopa.pn.deliverypush.service.AuditLogService;
import it.pagopa.pn.deliverypush.service.ExternalChannelService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;

class ExternalChannelServiceImplTest {
    @Mock
    private ExternalChannelUtils externalChannelUtils;
    @Mock
    private ExternalChannelSendClient externalChannel;
    @Mock
    private NotificationUtils notificationUtils;

    @Mock
    private DigitalWorkFlowUtils digitalWorkFlowUtils;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditLogService auditLogService;


    private ExternalChannelService externalChannelService;

    @BeforeEach
    void setup() {
        externalChannelService = new ExternalChannelServiceImpl(
                externalChannelUtils,
                externalChannel,
                notificationUtils,
                digitalWorkFlowUtils,
                notificationService, auditLogService);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendDigitalNotification() {
        //GIVEN
        String iun = "IUN01";
        String taxId = "taxId";
        String quickAccessToken = "test";

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

        String aarKey = "testKey";
        Mockito.when( externalChannelUtils.getAarKey(Mockito.anyString(), Mockito.anyInt()) ).thenReturn(aarKey);
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_DD_SEND), Mockito.anyString())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);

        Map<String, String> quickLinkTestMap = Map.of(recipient.getInternalId(), quickAccessToken);
        Mockito.when(notificationService.getRecipientsQuickAccessLinkToken(iun)).thenReturn(quickLinkTestMap);

        DigitalAddressSourceInt addressSource = DigitalAddressSourceInt.PLATFORM;
        int recIndex = 0;
        int sentAttemptMade = 0;

        //WHEN        
        externalChannelService.sendDigitalNotification(notification, digitalDomicile, addressSource, recIndex, sentAttemptMade, false);
        
        //THEN
        String eventIdExpected = TimelineEventId.SEND_DIGITAL_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .source(addressSource)
                        .sentAttemptMade(sentAttemptMade)
                        .build()
        );
        
        Mockito.verify(externalChannel).sendLegalNotification(notification, recipient,  digitalDomicile, eventIdExpected, aarKey, quickAccessToken);
        Mockito.verify(externalChannelUtils).addSendDigitalNotificationToTimeline(notification, digitalDomicile, addressSource, recIndex, sentAttemptMade, eventIdExpected);
        Mockito.verify( auditLogEvent).generateSuccess(Mockito.anyString(), Mockito.any());
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendDigitalNotification_fail() {
        //GIVEN
        String iun = "IUN01";
        String taxId = "taxId";
        String quickAccessToken = "test";

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

        String aarKey = "testKey";
        Mockito.when( externalChannelUtils.getAarKey(Mockito.anyString(), Mockito.anyInt()) ).thenReturn(aarKey);
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_DD_SEND), Mockito.anyString())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateFailure(Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);

        Map<String, String> quickLinkTestMap = Map.of(recipient.getInternalId(), quickAccessToken);
        Mockito.when(notificationService.getRecipientsQuickAccessLinkToken(iun)).thenReturn(quickLinkTestMap);

        Mockito.doThrow(new PnInternalException("fake", "fake")).when(externalChannel).sendLegalNotification(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        DigitalAddressSourceInt addressSource = DigitalAddressSourceInt.PLATFORM;
        int recIndex = 0;
        int sentAttemptMade = 0;

        //WHEN
        Assertions.assertThrows(PnInternalException.class, () -> externalChannelService.sendDigitalNotification(notification, digitalDomicile, addressSource, recIndex, sentAttemptMade, false));

        //THEN
        String eventIdExpected = TimelineEventId.SEND_DIGITAL_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .source(addressSource)
                        .sentAttemptMade(sentAttemptMade)
                        .build()
        );

        Mockito.verify(externalChannel).sendLegalNotification(notification, recipient,  digitalDomicile, eventIdExpected, aarKey, quickAccessToken);
        Mockito.verify(externalChannelUtils, Mockito.never()).addSendDigitalNotificationToTimeline(notification, digitalDomicile, addressSource, recIndex, sentAttemptMade, eventIdExpected);
        Mockito.verify( auditLogEvent, Mockito.never()).generateSuccess();
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent).generateFailure(Mockito.any(), Mockito.any());
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void sendDigitalNotification_AlreadyInProgress() {
        //GIVEN
        String iun = "IUN01";
        String taxId = "taxId";
        String quickAccessToken = "test";

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

        String aarKey = "testKey";
        Mockito.when( externalChannelUtils.getAarKey(Mockito.anyString(), Mockito.anyInt()) ).thenReturn(aarKey);
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_DD_SEND), Mockito.anyString())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);

        Map<String, String> quickLinkTestMap = Map.of(recipient.getInternalId(), quickAccessToken);
        Mockito.when(notificationService.getRecipientsQuickAccessLinkToken(iun)).thenReturn(quickLinkTestMap);

        //WHEN
        externalChannelService.sendDigitalNotification(notification, digitalDomicile, addressSource, recIndex, sentAttemptMade, true);

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

        Mockito.verify(externalChannel).sendLegalNotification(notification, recipient,  digitalDomicile, eventIdExpected, aarKey, quickAccessToken);
        
        Mockito.verify(digitalWorkFlowUtils).addDigitalDeliveringProgressTimelineElement(
                eq(notification),
                eq(EventCodeInt.DP00),
                        eq(recIndex),
                                eq(false),
                                        eq(null),
                                                Mockito.any(DigitalAddressFeedback.class)
        );

        Mockito.verify( auditLogEvent).generateSuccess(Mockito.anyString(), Mockito.any());
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(Mockito.any());
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

        String aarKey = "testKey";
        Mockito.when( externalChannelUtils.getAarKey(Mockito.anyString(), Mockito.anyInt()) ).thenReturn(aarKey);
        String quickAccessToken = "test";
        Map<String, String> quickLinkTestMap = Map.of(recipient.getInternalId(), quickAccessToken);
        Mockito.when(notificationService.getRecipientsQuickAccessLinkToken(iun)).thenReturn(quickLinkTestMap);
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_DA_SEND_EMAIL), Mockito.anyString(), Mockito.anyString())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess()).thenReturn(auditLogEvent);


        int recIndex = 0;
        String eventId = "eventId";
        
        //WHEN
        externalChannelService.sendCourtesyNotification(notification, courtesyDigitalAddress, recIndex, eventId);

        //THEN
        Mockito.verify(externalChannel).sendCourtesyNotification(notification, recipient,  courtesyDigitalAddress, eventId, aarKey, quickAccessToken);
        Mockito.verify( auditLogEvent).generateSuccess();
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(Mockito.any());

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendCourtesyNotification_SMS() {
        //GIVEN
        String iun = "IUN01";
        String taxId = "taxId";

        CourtesyDigitalAddressInt courtesyDigitalAddress = CourtesyDigitalAddressInt.builder()
                .address("3331234xxx")
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.SMS)
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

        String aarKey = "testKey";
        Mockito.when( externalChannelUtils.getAarKey(Mockito.anyString(), Mockito.anyInt()) ).thenReturn(aarKey);
        String quickAccessToken = "test";
        Map<String, String> quickLinkTestMap = Map.of(recipient.getInternalId(), quickAccessToken);
        Mockito.when(notificationService.getRecipientsQuickAccessLinkToken(iun)).thenReturn(quickLinkTestMap);
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_DA_SEND_SMS), Mockito.anyString(), Mockito.anyString())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess()).thenReturn(auditLogEvent);

        int recIndex = 0;
        String eventId = "eventId";

        //WHEN
        externalChannelService.sendCourtesyNotification(notification, courtesyDigitalAddress, recIndex, eventId);

        //THEN
        Mockito.verify(externalChannel).sendCourtesyNotification(notification, recipient,  courtesyDigitalAddress, eventId, aarKey, quickAccessToken);
        Mockito.verify( auditLogEvent).generateSuccess();
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(Mockito.any());
    }



    @ExtendWith(MockitoExtension.class)
    @Test
    void sendCourtesyNotification_SMS_fail() {
        //GIVEN
        String iun = "IUN01";
        String taxId = "taxId";

        CourtesyDigitalAddressInt courtesyDigitalAddress = CourtesyDigitalAddressInt.builder()
                .address("3331234xxx")
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.SMS)
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

        String aarKey = "testKey";
        Mockito.when( externalChannelUtils.getAarKey(Mockito.anyString(), Mockito.anyInt()) ).thenReturn(aarKey);
        String quickAccessToken = "test";
        Map<String, String> quickLinkTestMap = Map.of(recipient.getInternalId(), quickAccessToken);
        Mockito.when(notificationService.getRecipientsQuickAccessLinkToken(iun)).thenReturn(quickLinkTestMap);
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_DA_SEND_SMS), Mockito.anyString(), Mockito.anyString())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateFailure(Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);

        Mockito.doThrow(new PnInternalException("fake", "fake")).when(externalChannel).sendCourtesyNotification(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.anyString());

        int recIndex = 0;
        String eventId = "eventId";

        //WHEN
        Assertions.assertThrows(PnInternalException.class, () -> externalChannelService.sendCourtesyNotification(notification, courtesyDigitalAddress, recIndex, eventId));

        //THEN
        Mockito.verify(externalChannel).sendCourtesyNotification(notification, recipient,  courtesyDigitalAddress, eventId, aarKey, quickAccessToken);
        Mockito.verify( auditLogEvent, Mockito.never()).generateSuccess();
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent).generateFailure(Mockito.any(), Mockito.any());
    }

}