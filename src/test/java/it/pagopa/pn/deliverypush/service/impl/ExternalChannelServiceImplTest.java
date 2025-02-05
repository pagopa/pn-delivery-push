package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowUtils;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.F24ResolutionMode;
import it.pagopa.pn.deliverypush.action.utils.ExternalChannelUtils;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.SendInformation;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.EventCodeInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.SendAttachmentMode;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalchannel.ExternalChannelSendClient;
import it.pagopa.pn.deliverypush.service.AuditLogService;
import it.pagopa.pn.deliverypush.service.ExternalChannelService;
import it.pagopa.pn.deliverypush.service.NotificationService;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.utils.FeatureEnabledUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

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

    @Mock
    private TimelineUtils timelineUtils;

    @Mock
    private AttachmentUtils attachmentUtils;

    @Mock
    private TimelineService timelineService;

    private static final String SERCQ_ADDRESS = "x-pagopa-pn-sercq:send-self:notification-already-delivered";

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_INSTANT;

    private ExternalChannelService externalChannelService;

    @Mock
    private FeatureEnabledUtils featureEnabledUtils;

    @BeforeEach
    void setup() {
        externalChannelService = new ExternalChannelServiceImpl(
                externalChannelUtils,
                externalChannel,
                notificationUtils,
                digitalWorkFlowUtils,
                notificationService, auditLogService,
                timelineUtils, attachmentUtils, timelineService, featureEnabledUtils);
    }

    @Test
    @ExtendWith(MockitoExtension.class)
    void sendDigitalNotificationWithSercQAndPfWorflowEnabled() {
        when(featureEnabledUtils.isPfNewWorkflowEnabled(any())).thenReturn(true);
        //GIVEN
        String iun = "IUN01";
        String taxId = "taxId";
        String quickAccessToken = "test";

        LegalDigitalAddressInt digitalDomicile = LegalDigitalAddressInt.builder()
                .address(SERCQ_ADDRESS)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.SERCQ)
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
        
        when(notificationUtils.getRecipientFromIndex(any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);

        String aarKey = "testKey";
        String attachments = "test1";
        when( attachmentUtils.retrieveAttachments(any(),any(),any(),eq(F24ResolutionMode.RESOLVE_WITH_TIMELINE),any(),any()) ).thenReturn(Arrays.asList(aarKey,attachments));
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_DD_SEND), Mockito.anyString())).thenReturn(auditLogEvent);
        when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);

        Map<String, String> quickLinkTestMap = Map.of(recipient.getInternalId(), quickAccessToken);
        when(notificationService.getRecipientsQuickAccessLinkToken(iun)).thenReturn(quickLinkTestMap);

        DigitalAddressSourceInt addressSource = DigitalAddressSourceInt.PLATFORM;
        int recIndex = 0;
        int sentAttemptMade = 0;

        //WHEN        
        final boolean isFirstSendRetry = false;
        
        SendInformation sendInformation = SendInformation.builder()
                .digitalAddress(digitalDomicile)
                .digitalAddressSource(addressSource)
                .retryNumber(sentAttemptMade)
                .isFirstSendRetry(isFirstSendRetry)
                .relatedFeedbackTimelineId(null)
                .build();
        
        externalChannelService.sendDigitalNotification(notification, recIndex, false, sendInformation);
        
        //THEN
        String eventIdExpected = TimelineEventId.SEND_DIGITAL_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .source(addressSource)
                        .sentAttemptMade(sentAttemptMade)
                        .isFirstSendRetry(isFirstSendRetry)
                        .build()
        );

        ArgumentCaptor<LegalDigitalAddressInt> captor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        Mockito.verify(externalChannel).sendLegalNotification(eq(notification), eq(recipient), captor.capture(), eq(eventIdExpected), eq(Arrays.asList(aarKey, attachments)), eq(quickAccessToken));
        Assertions.assertEquals(SERCQ_ADDRESS, captor.getValue().getAddress());
        Mockito.verify(externalChannelUtils).addSendDigitalNotificationToTimeline(notification, recIndex, sendInformation, eventIdExpected);
        Mockito.verify( auditLogEvent).generateSuccess(Mockito.anyString(), any());
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendDigitalNotificationWithSercQAndPfWorflowDisabled() {
        when(featureEnabledUtils.isPfNewWorkflowEnabled(any())).thenReturn(false);

        //GIVEN
        String iun = "IUN01";
        String taxId = "taxId";
        String quickAccessToken = "test";

        LegalDigitalAddressInt digitalDomicile = LegalDigitalAddressInt.builder()
                .address(SERCQ_ADDRESS)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.SERCQ)
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

        when(notificationUtils.getRecipientFromIndex(any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);

        String aarKey = "testKey";
        String attachments = "test1";
        when( attachmentUtils.retrieveAttachments(any(),any(),any(),eq(F24ResolutionMode.RESOLVE_WITH_TIMELINE),any(),any()) ).thenReturn(Arrays.asList(aarKey,attachments));
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_DD_SEND), Mockito.anyString())).thenReturn(auditLogEvent);
        when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);

        Map<String, String> quickLinkTestMap = Map.of(recipient.getInternalId(), quickAccessToken);
        when(notificationService.getRecipientsQuickAccessLinkToken(iun)).thenReturn(quickLinkTestMap);

        DigitalAddressSourceInt addressSource = DigitalAddressSourceInt.PLATFORM;
        int recIndex = 0;
        int sentAttemptMade = 0;

        Instant now = Instant.now();
        mockGetTimelineElement(iun, recIndex, now);


        //WHEN
        final boolean isFirstSendRetry = false;

        SendInformation sendInformation = SendInformation.builder()
                .digitalAddress(digitalDomicile)
                .digitalAddressSource(addressSource)
                .retryNumber(sentAttemptMade)
                .isFirstSendRetry(isFirstSendRetry)
                .relatedFeedbackTimelineId(null)
                .build();

        externalChannelService.sendDigitalNotification(notification, recIndex, false, sendInformation);

        //THEN
        String eventIdExpected = TimelineEventId.SEND_DIGITAL_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .source(addressSource)
                        .sentAttemptMade(sentAttemptMade)
                        .isFirstSendRetry(isFirstSendRetry)
                        .build()
        );

        ArgumentCaptor<LegalDigitalAddressInt> captor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        Mockito.verify(externalChannel).sendLegalNotification(eq(notification), eq(recipient), captor.capture(), eq(eventIdExpected), eq(Arrays.asList(aarKey, attachments)), eq(quickAccessToken));
        Assertions.assertEquals(SERCQ_ADDRESS + "?timestamp=" + dateTimeFormatter.format(now), captor.getValue().getAddress());
        Mockito.verify(externalChannelUtils).addSendDigitalNotificationToTimeline(notification, recIndex, sendInformation, eventIdExpected);
        Mockito.verify( auditLogEvent).generateSuccess(Mockito.anyString(), any());
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(any());
    }


    @ExtendWith(MockitoExtension.class)
    @ParameterizedTest
    @MethodSource("sendDigitalNotificationParams")
    void sendDigitalNotification(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE channelType, String address) {
        //GIVEN
        String iun = "IUN01";
        String taxId = "taxId";
        String quickAccessToken = "test";

        LegalDigitalAddressInt digitalDomicile = LegalDigitalAddressInt.builder()
                .address(address)
                .type(channelType)
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

        when(notificationUtils.getRecipientFromIndex(any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);

        String aarKey = "testKey";
        String attachments = "test1";
        when( attachmentUtils.retrieveAttachments(any(),any(),any(),eq(F24ResolutionMode.RESOLVE_WITH_TIMELINE),any(),any()) ).thenReturn(Arrays.asList(aarKey,attachments));
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_DD_SEND), Mockito.anyString())).thenReturn(auditLogEvent);
        when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);

        Map<String, String> quickLinkTestMap = Map.of(recipient.getInternalId(), quickAccessToken);
        when(notificationService.getRecipientsQuickAccessLinkToken(iun)).thenReturn(quickLinkTestMap);

        DigitalAddressSourceInt addressSource = DigitalAddressSourceInt.PLATFORM;
        int recIndex = 0;
        int sentAttemptMade = 0;

        Instant now = Instant.now();
        if (channelType == LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.SERCQ) {
            mockGetTimelineElement(iun, recIndex, now);
        }

        //WHEN
        final boolean isFirstSendRetry = false;

        SendInformation sendInformation = SendInformation.builder()
                .digitalAddress(digitalDomicile)
                .digitalAddressSource(addressSource)
                .retryNumber(sentAttemptMade)
                .isFirstSendRetry(isFirstSendRetry)
                .relatedFeedbackTimelineId(null)
                .build();

        externalChannelService.sendDigitalNotification(notification, recIndex, false, sendInformation);

        //THEN
        String eventIdExpected = TimelineEventId.SEND_DIGITAL_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .source(addressSource)
                        .sentAttemptMade(sentAttemptMade)
                        .isFirstSendRetry(isFirstSendRetry)
                        .build()
        );

        ArgumentCaptor<LegalDigitalAddressInt> captor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        Mockito.verify(externalChannel).sendLegalNotification(eq(notification), eq(recipient), captor.capture(), eq(eventIdExpected), eq(Arrays.asList(aarKey, attachments)), eq(quickAccessToken));
        if (channelType == LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.SERCQ) {
            Assertions.assertEquals(SERCQ_ADDRESS + "?timestamp=" + dateTimeFormatter.format(now), captor.getValue().getAddress());
        } else Assertions.assertEquals(address, captor.getValue().getAddress());
        Mockito.verify(externalChannelUtils).addSendDigitalNotificationToTimeline(notification, recIndex, sendInformation, eventIdExpected);
        Mockito.verify( auditLogEvent).generateSuccess(Mockito.anyString(), any());
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(any());
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void sendDigitalNotification_SERCQ_TimelineElementNotFound() {
        //GIVEN
        String iun = "IUN01";
        String taxId = "taxId";
        String quickAccessToken = "test";

        LegalDigitalAddressInt digitalDomicile = LegalDigitalAddressInt.builder()
                .address(SERCQ_ADDRESS)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.SERCQ)
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

        when(notificationUtils.getRecipientFromIndex(any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);

        String aarKey = "testKey";
        String attachments = "test1";
        when( attachmentUtils.retrieveAttachments(any(),any(),any(),eq(F24ResolutionMode.RESOLVE_WITH_TIMELINE),any(),any()) ).thenReturn(Arrays.asList(aarKey,attachments));
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_DD_SEND), Mockito.anyString())).thenReturn(auditLogEvent);
        when(auditLogEvent.generateFailure(Mockito.anyString(), any())).thenReturn(auditLogEvent);

        Map<String, String> quickLinkTestMap = Map.of(recipient.getInternalId(), quickAccessToken);
        when(notificationService.getRecipientsQuickAccessLinkToken(iun)).thenReturn(quickLinkTestMap);

        DigitalAddressSourceInt addressSource = DigitalAddressSourceInt.PLATFORM;
        int recIndex = 0;
        int sentAttemptMade = 0;

        when(timelineService.getTimelineElement(eq(iun), anyString())).thenReturn(Optional.empty());

        //WHEN
        final boolean isFirstSendRetry = false;

        SendInformation sendInformation = SendInformation.builder()
                .digitalAddress(digitalDomicile)
                .digitalAddressSource(addressSource)
                .retryNumber(sentAttemptMade)
                .isFirstSendRetry(isFirstSendRetry)
                .relatedFeedbackTimelineId(null)
                .build();

        assertThrows(PnInternalException.class, () -> externalChannelService.sendDigitalNotification(notification, recIndex, false, sendInformation));

        //THEN
        String eventIdExpected = TimelineEventId.SEND_DIGITAL_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .source(addressSource)
                        .sentAttemptMade(sentAttemptMade)
                        .isFirstSendRetry(isFirstSendRetry)
                        .build()
        );

        Mockito.verify(externalChannel, Mockito.never()).sendLegalNotification(any(NotificationInt.class), any(NotificationRecipientInt.class), any(LegalDigitalAddressInt.class), anyString(), anyList(), anyString());
        Mockito.verify(externalChannelUtils, Mockito.never()).addSendDigitalNotificationToTimeline(notification, recIndex, sendInformation, eventIdExpected);
        Mockito.verify(auditLogEvent).log();
        Mockito.verify(auditLogEvent).generateFailure(any(), any());
    }

    @ExtendWith(MockitoExtension.class)
    @ParameterizedTest
    @MethodSource("sendDigitalNotificationParams")
    void sendLegalNotificationMoreAttach(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE channelType, String address) {
        //GIVEN
        String iun = "IUN01";
        String taxId = "taxId";
        String quickAccessToken = "test";

        LegalDigitalAddressInt digitalDomicile = LegalDigitalAddressInt.builder()
                .address(address)
                .type(channelType)
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

        when(notificationUtils.getRecipientFromIndex(any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);

        String aarKey = "testKey";
        String attachments = "test1";

        when( attachmentUtils.retrieveAttachments(any(),any(),any(),eq(F24ResolutionMode.RESOLVE_WITH_TIMELINE),any(), any()) ).thenReturn(Arrays.asList(aarKey,attachments));

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_DD_SEND), Mockito.anyString())).thenReturn(auditLogEvent);
        when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);

        Map<String, String> quickLinkTestMap = Map.of(recipient.getInternalId(), quickAccessToken);
        when(notificationService.getRecipientsQuickAccessLinkToken(iun)).thenReturn(quickLinkTestMap);

        DigitalAddressSourceInt addressSource = DigitalAddressSourceInt.PLATFORM;
        int recIndex = 0;
        int sentAttemptMade = 0;

        Instant now = Instant.now();
        if (channelType == LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.SERCQ) {
            mockGetTimelineElement(iun, recIndex, now);
        }

        //WHEN
        final boolean isFirstSendRetry = false;

        SendInformation sendInformation = SendInformation.builder()
                .digitalAddress(digitalDomicile)
                .digitalAddressSource(addressSource)
                .retryNumber(sentAttemptMade)
                .isFirstSendRetry(isFirstSendRetry)
                .relatedFeedbackTimelineId(null)
                .build();

        externalChannelService.sendDigitalNotification(notification, recIndex, false, sendInformation);

        //THEN
        String eventIdExpected = TimelineEventId.SEND_DIGITAL_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .source(addressSource)
                        .sentAttemptMade(sentAttemptMade)
                        .isFirstSendRetry(isFirstSendRetry)
                        .build()
        );

        Mockito.verify(externalChannelUtils, Mockito.never()).getAarKey(anyString(),anyInt());
        ArgumentCaptor<LegalDigitalAddressInt> captor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        Mockito.verify(externalChannel).sendLegalNotification(eq(notification), eq(recipient), captor.capture(), eq(eventIdExpected), eq(Arrays.asList(aarKey, attachments)), eq(quickAccessToken));
        if (channelType == LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.SERCQ) {
            Assertions.assertEquals(SERCQ_ADDRESS + "?timestamp=" + dateTimeFormatter.format(now), captor.getValue().getAddress());
        } else Assertions.assertEquals(address, captor.getValue().getAddress());
    }

    @ExtendWith(MockitoExtension.class)
    @ParameterizedTest
    @MethodSource("sendDigitalNotificationParams")
    void sendDigitalNotification_fail(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE channelType, String address) {
        //GIVEN
        String iun = "IUN01";
        String taxId = "taxId";
        String quickAccessToken = "test";

        LegalDigitalAddressInt digitalDomicile = LegalDigitalAddressInt.builder()
                .address(address)
                .type(channelType)
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

        when(notificationUtils.getRecipientFromIndex(any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);

        String aarKey = "testKey";
        when( attachmentUtils.retrieveAttachments(any(),any(),any(),eq(F24ResolutionMode.RESOLVE_WITH_TIMELINE),any(),any()) ).thenReturn(Collections.singletonList(aarKey));
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_DD_SEND), Mockito.anyString())).thenReturn(auditLogEvent);
        when(auditLogEvent.generateFailure(Mockito.anyString(), any())).thenReturn(auditLogEvent);

        Map<String, String> quickLinkTestMap = Map.of(recipient.getInternalId(), quickAccessToken);
        when(notificationService.getRecipientsQuickAccessLinkToken(iun)).thenReturn(quickLinkTestMap);

        Mockito.doThrow(new PnInternalException("fake", "fake")).when(externalChannel).sendLegalNotification(any(), any(), any(), any(), any(), any());

        DigitalAddressSourceInt addressSource = DigitalAddressSourceInt.PLATFORM;
        int recIndex = 0;
        int sentAttemptMade = 0;

        Instant now = Instant.now();
        if (channelType == LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.SERCQ) {
            mockGetTimelineElement(iun, recIndex, now);
        }

        //WHEN
        final boolean isFirstSendRetry = false;
        SendInformation sendInformation = SendInformation.builder()
                .digitalAddress(digitalDomicile)
                .digitalAddressSource(addressSource)
                .retryNumber(sentAttemptMade)
                .isFirstSendRetry(isFirstSendRetry)
                .relatedFeedbackTimelineId(null)
                .build();
        
        Assertions.assertThrows(PnInternalException.class, () -> externalChannelService.sendDigitalNotification(notification, recIndex,  false, sendInformation));

        //THEN
        String eventIdExpected = TimelineEventId.SEND_DIGITAL_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .source(addressSource)
                        .sentAttemptMade(sentAttemptMade)
                        .isFirstSendRetry(isFirstSendRetry)
                        .build()
        );
        
        ArgumentCaptor<LegalDigitalAddressInt> captor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        Mockito.verify(externalChannel).sendLegalNotification(eq(notification), eq(recipient), captor.capture(), eq(eventIdExpected), eq(Collections.singletonList(aarKey)), eq(quickAccessToken));
        if (channelType == LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.SERCQ) {
            Assertions.assertEquals(SERCQ_ADDRESS + "?timestamp=" + dateTimeFormatter.format(now), captor.getValue().getAddress());
        } else Assertions.assertEquals(address, captor.getValue().getAddress());
        Mockito.verify(externalChannelUtils, Mockito.never()).addSendDigitalNotificationToTimeline(notification, recIndex, sendInformation, eventIdExpected);
        Mockito.verify( auditLogEvent, Mockito.never()).generateSuccess();
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent).generateFailure(any(), any());
    }

    @ExtendWith(MockitoExtension.class)
    @ParameterizedTest
    @MethodSource("sendDigitalNotificationParams")
    void sendDigitalNotificationCancelled(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE channelType, String address) {
        //GIVEN
        String iun = "IUN-sendDigitalNotificationCancelled";
        String taxId = "taxId";
        String quickAccessToken = "test";

        LegalDigitalAddressInt digitalDomicile = LegalDigitalAddressInt.builder()
            .address(address)
            .type(channelType)
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

        String aarKey = "testKey";

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);

        when(timelineUtils.checkIsNotificationCancellationRequested(anyString())).thenReturn(true);

        DigitalAddressSourceInt addressSource = DigitalAddressSourceInt.PLATFORM;
        int recIndex = 0;
        int sentAttemptMade = 0;

        //WHEN
        final boolean isFirstSendRetry = false;

        SendInformation sendInformation = SendInformation.builder()
            .digitalAddress(digitalDomicile)
            .digitalAddressSource(addressSource)
            .retryNumber(sentAttemptMade)
            .isFirstSendRetry(isFirstSendRetry)
            .relatedFeedbackTimelineId(null)
            .build();

        externalChannelService.sendDigitalNotification(notification, recIndex, false, sendInformation);

        //THEN
        String eventIdExpected = TimelineEventId.SEND_DIGITAL_DOMICILE.buildEventId(
            EventId.builder()
                .iun(notification.getIun())
                .recIndex(recIndex)
                .source(addressSource)
                .sentAttemptMade(sentAttemptMade)
                .isFirstSendRetry(isFirstSendRetry)
                .build()
        );

        //THEN
        Mockito.verify(externalChannel, Mockito.never()).sendLegalNotification(notification, recipient,  digitalDomicile, eventIdExpected, Collections.singletonList(aarKey), quickAccessToken);
        Mockito.verify(externalChannelUtils, Mockito.never()).addSendDigitalNotificationToTimeline(notification, recIndex, sendInformation, eventIdExpected);
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(any());

    }

    @ExtendWith(MockitoExtension.class)
    @ParameterizedTest
    @MethodSource("sendDigitalNotificationParams")
    void sendDigitalNotification_AlreadyInProgress(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE channelType, String address) {
        //GIVEN
        String iun = "IUN01";
        String taxId = "taxId";
        String quickAccessToken = "test";

        LegalDigitalAddressInt digitalDomicile = LegalDigitalAddressInt.builder()
                .address(address)
                .type(channelType)
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

        when(notificationUtils.getRecipientFromIndex(any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);

        DigitalAddressSourceInt addressSource = DigitalAddressSourceInt.PLATFORM;
        int recIndex = 0;
        int sentAttemptMade = 0;

        Instant now = Instant.now();
        if (channelType == LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.SERCQ) {
            mockGetTimelineElement(iun, recIndex, now);
        }

        String aarKey = "testKey";
        when( attachmentUtils.retrieveAttachments(any(),any(), any(),eq(F24ResolutionMode.RESOLVE_WITH_TIMELINE),any(),any()) ).thenReturn(Collections.singletonList(aarKey));
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_DD_SEND), Mockito.anyString())).thenReturn(auditLogEvent);
        when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);

        Map<String, String> quickLinkTestMap = Map.of(recipient.getInternalId(), quickAccessToken);
        when(notificationService.getRecipientsQuickAccessLinkToken(iun)).thenReturn(quickLinkTestMap);

        //WHEN
        final boolean isFirstSendRetry = false;
        SendInformation sendInformation = SendInformation.builder()
                .digitalAddress(digitalDomicile)
                .digitalAddressSource(addressSource)
                .retryNumber(sentAttemptMade)
                .isFirstSendRetry(isFirstSendRetry)
                .relatedFeedbackTimelineId(null)
                .build();
        externalChannelService.sendDigitalNotification(notification,  recIndex, true, sendInformation);

        //THEN
        String eventIdExpected = TimelineEventId.SEND_DIGITAL_PROGRESS.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .source(addressSource)
                        .progressIndex(1)
                        .sentAttemptMade(0)
                        .isFirstSendRetry(isFirstSendRetry)
                        .build()
        );

        ArgumentCaptor<LegalDigitalAddressInt> captor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        Mockito.verify(externalChannel).sendLegalNotification(eq(notification), eq(recipient), captor.capture(), eq(eventIdExpected), eq(Collections.singletonList(aarKey)), eq(quickAccessToken));
        if (channelType == LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.SERCQ) {
            Assertions.assertEquals(SERCQ_ADDRESS + "?timestamp=" + dateTimeFormatter.format(now), captor.getValue().getAddress());
        } else Assertions.assertEquals(address, captor.getValue().getAddress());

        Mockito.verify(digitalWorkFlowUtils).addDigitalDeliveringProgressTimelineElement(
                eq(notification),
                eq(EventCodeInt.DP00),
                eq(recIndex),
                eq(isFirstSendRetry),
                eq(null),
                any(SendInformation.class)
        );

        Mockito.verify( auditLogEvent).generateSuccess(Mockito.anyString(), any());
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(any());
    }

    @ExtendWith(MockitoExtension.class)
    @ParameterizedTest
    @MethodSource("sendDigitalNotificationParams")
    void sendDigitalNotification_AlreadyInProgressCancelled(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE channelType, String address) {
        //GIVEN
        String iun = "IUN-sendDigitalNotification_AlreadyInProgressCancelled";
        String taxId = "taxId";
        String quickAccessToken = "test";

        LegalDigitalAddressInt digitalDomicile = LegalDigitalAddressInt.builder()
            .address(address)
            .type(channelType)
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

        DigitalAddressSourceInt addressSource = DigitalAddressSourceInt.PLATFORM;
        int recIndex = 0;
        int sentAttemptMade = 0;

        String aarKey = "testKey";

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        when(timelineUtils.checkIsNotificationCancellationRequested(anyString())).thenReturn(true);

        //WHEN
        final boolean isFirstSendRetry = false;
        SendInformation sendInformation = SendInformation.builder()
            .digitalAddress(digitalDomicile)
            .digitalAddressSource(addressSource)
            .retryNumber(sentAttemptMade)
            .isFirstSendRetry(isFirstSendRetry)
            .relatedFeedbackTimelineId(null)
            .build();
        externalChannelService.sendDigitalNotification(notification,  recIndex, true, sendInformation);

        //THEN
        String eventIdExpected = TimelineEventId.SEND_DIGITAL_PROGRESS.buildEventId(
            EventId.builder()
                .iun(notification.getIun())
                .recIndex(recIndex)
                .source(addressSource)
                .progressIndex(1)
                .sentAttemptMade(0)
                .isFirstSendRetry(isFirstSendRetry)
                .build()
        );

        Mockito.verify(externalChannel, Mockito.never()).sendLegalNotification(notification, recipient,  digitalDomicile, eventIdExpected, Collections.singletonList(aarKey), quickAccessToken);
        Mockito.verify(externalChannelUtils, Mockito.never()).addSendDigitalNotificationToTimeline(notification, recIndex, sendInformation, eventIdExpected);
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(any());

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

        when(notificationUtils.getRecipientFromIndex(any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);

        String aarKey = "testKey";
        when( externalChannelUtils.getAarKey(Mockito.anyString(), Mockito.anyInt()) ).thenReturn(aarKey);
        String quickAccessToken = "test";
        Map<String, String> quickLinkTestMap = Map.of(recipient.getInternalId(), quickAccessToken);
        when(notificationService.getRecipientsQuickAccessLinkToken(iun)).thenReturn(quickLinkTestMap);
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_DA_SEND_EMAIL), Mockito.anyString(), Mockito.anyString())).thenReturn(auditLogEvent);
        when(auditLogEvent.generateSuccess()).thenReturn(auditLogEvent);


        int recIndex = 0;
        String eventId = "eventId";
        
        //WHEN
        externalChannelService.sendCourtesyNotification(notification, courtesyDigitalAddress, recIndex, eventId);

        //THEN
        Mockito.verify(externalChannel).sendCourtesyNotification(notification, recipient,  courtesyDigitalAddress, eventId, aarKey, quickAccessToken);
        Mockito.verify(attachmentUtils, Mockito.never()).retrieveAttachments(any(NotificationInt.class),anyInt(),any(SendAttachmentMode.class), any(F24ResolutionMode.class), anyList(), anyBoolean());
        Mockito.verify( auditLogEvent).generateSuccess();
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(any());

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendCourtesyNotificationCancelled() {
        //GIVEN
        String iun = "IUN-sendCourtesyNotificationCancelled";

        CourtesyDigitalAddressInt courtesyDigitalAddress = CourtesyDigitalAddressInt.builder()
            .address("courtesyDigitalAddress@test.it")
            .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL)
            .build();

        NotificationInt notification = NotificationTestBuilder.builder()
            .withIun(iun)
            .withPaId("paId01")
            .build();

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);

        when(timelineUtils.checkIsNotificationCancellationRequested(anyString())).thenReturn(true);

        int recIndex = 0;
        String eventId = "eventId";

        //WHEN
        externalChannelService.sendCourtesyNotification(notification, courtesyDigitalAddress, recIndex, eventId);

        //THEN
        Mockito.verify(externalChannel, Mockito.never()).sendCourtesyNotification(any(), any(),  any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(any());
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

        when(notificationUtils.getRecipientFromIndex(any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);

        String aarKey = "testKey";
        when( externalChannelUtils.getAarKey(Mockito.anyString(), Mockito.anyInt()) ).thenReturn(aarKey);
        String quickAccessToken = "test";
        Map<String, String> quickLinkTestMap = Map.of(recipient.getInternalId(), quickAccessToken);
        when(notificationService.getRecipientsQuickAccessLinkToken(iun)).thenReturn(quickLinkTestMap);
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_DA_SEND_SMS), Mockito.anyString(), Mockito.anyString())).thenReturn(auditLogEvent);
        when(auditLogEvent.generateSuccess()).thenReturn(auditLogEvent);

        int recIndex = 0;
        String eventId = "eventId";

        //WHEN
        externalChannelService.sendCourtesyNotification(notification, courtesyDigitalAddress, recIndex, eventId);

        //THEN
        Mockito.verify(externalChannel).sendCourtesyNotification(notification, recipient,  courtesyDigitalAddress, eventId, aarKey, quickAccessToken);
        Mockito.verify( auditLogEvent).generateSuccess();
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(any());
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

        when(notificationUtils.getRecipientFromIndex(any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);

        String aarKey = "testKey";
        when( externalChannelUtils.getAarKey(Mockito.anyString(), Mockito.anyInt()) ).thenReturn(aarKey);
        String quickAccessToken = "test";
        Map<String, String> quickLinkTestMap = Map.of(recipient.getInternalId(), quickAccessToken);
        when(notificationService.getRecipientsQuickAccessLinkToken(iun)).thenReturn(quickLinkTestMap);
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_DA_SEND_SMS), Mockito.anyString(), Mockito.anyString())).thenReturn(auditLogEvent);
        when(auditLogEvent.generateFailure(Mockito.anyString(), any())).thenReturn(auditLogEvent);

        Mockito.doThrow(new PnInternalException("fake", "fake")).when(externalChannel).sendCourtesyNotification(any(), any(), any(), Mockito.anyString(), any(), Mockito.anyString());

        int recIndex = 0;
        String eventId = "eventId";

        //WHEN
        Assertions.assertThrows(PnInternalException.class, () -> externalChannelService.sendCourtesyNotification(notification, courtesyDigitalAddress, recIndex, eventId));

        //THEN
        Mockito.verify(externalChannel).sendCourtesyNotification(notification, recipient,  courtesyDigitalAddress, eventId, aarKey, quickAccessToken);
        Mockito.verify( auditLogEvent, Mockito.never()).generateSuccess();
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent).generateFailure(any(), any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendCourtesyNotification_SMS_Cancelled() {
        //GIVEN
        String iun = "IUN-sendCourtesyNotification_SMS_Cancelled";

        CourtesyDigitalAddressInt courtesyDigitalAddress = CourtesyDigitalAddressInt.builder()
            .address("3331234xxx")
            .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.SMS)
            .build();

        NotificationInt notification = NotificationTestBuilder.builder()
            .withIun(iun)
            .withPaId("paId01")
            .build();

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        when(timelineUtils.checkIsNotificationCancellationRequested(anyString())).thenReturn(true);

        int recIndex = 0;
        String eventId = "eventId";

        //WHEN
        externalChannelService.sendCourtesyNotification(notification, courtesyDigitalAddress, recIndex, eventId);

        //THEN
        Mockito.verify(externalChannel, Mockito.never()).sendCourtesyNotification(any(), any(),  any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(any());
    }

    private void mockGetTimelineElement(String iun, int recIndex, Instant timestamp) {
        String aarGenEventIdExpected = TimelineEventId.AAR_GENERATION.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build()
        );
        when(timelineService.getTimelineElement(iun, aarGenEventIdExpected)).thenReturn(Optional.of(TimelineElementInternal.builder().timestamp(timestamp).build()));
    }

    private static Stream<Arguments> sendDigitalNotificationParams() {
        return Stream.of(
                Arguments.of(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC, "digitalDomicile@test.it"),
                Arguments.of(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.SERCQ, SERCQ_ADDRESS));
    }
}