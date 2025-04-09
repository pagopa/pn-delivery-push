package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowUtils;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypush.action.utils.AarUtils;
import it.pagopa.pn.deliverypush.action.utils.ExternalChannelUtils;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.SendInformation;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.SendAttachmentMode;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarCreationRequestDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarGenerationDetailsInt;
import it.pagopa.pn.deliverypush.legalfacts.AarTemplateType;
import it.pagopa.pn.deliverypush.legalfacts.StaticAarTemplateChooseStrategy;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalchannel.ExternalChannelSendClient;
import it.pagopa.pn.deliverypush.service.*;
import it.pagopa.pn.deliverypush.service.utils.FileUtils;
import it.pagopa.pn.deliverypush.utils.FeatureEnabledUtils;
import it.pagopa.pn.deliverypush.utils.PnSendMode;
import it.pagopa.pn.deliverypush.utils.PnSendModeUtils;
import org.jetbrains.annotations.NotNull;
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

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static it.pagopa.pn.deliverypush.action.it.utils.TestUtils.getNotificationV2WithDocument;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class ExternalChannelServiceImplAttachmentTest {

    @Mock
    private SafeStorageService safeStorageService;
    @Mock
    private NotificationUtils notificationUtils;
    @Mock
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;
    @Mock
    private PnSendModeUtils pnSendModeUtils;
    @Mock
    private AarUtils aarUtils;
    @Mock
    private NotificationProcessCostService notificationProcessCostService;
    @Mock
    private TimelineService timelineService;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private ExternalChannelSendClient externalChannel;
    @Mock
    private DigitalWorkFlowUtils digitalWorkFlowUtils;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private FeatureEnabledUtils featureEnabledUtils;

    private ExternalChannelService externalChannelService;
    private static final String SERCQ_ADDRESS = "x-pagopa-pn-sercq:send-self:notification-already-delivered";
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_INSTANT;


    @BeforeEach
    void setup() {
        ExternalChannelUtils externalChannelUtils = new ExternalChannelUtils(timelineService, timelineUtils);
        AttachmentUtils attachmentUtils = new AttachmentUtils(
                safeStorageService,
                pnDeliveryPushConfigs,
                notificationProcessCostService,
                pnSendModeUtils,
                aarUtils,
                notificationUtils,
                timelineUtils
                );

        externalChannelService = new ExternalChannelServiceImpl(
                externalChannelUtils,
                externalChannel,
                notificationUtils,
                digitalWorkFlowUtils,
                notificationService,
                auditLogService,
                timelineUtils,
                attachmentUtils,
                timelineService, featureEnabledUtils);
    }



    @ExtendWith(MockitoExtension.class)
    @ParameterizedTest
    @MethodSource("sendDigitalNotificationParams")
    void sendLegalNotificationAAR_DOCUMENTS_PAYMENTS(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE channelType, String address) {
        //GIVEN
        String quickAccessToken = "test";
        String aarKey = "testKey";

        NotificationInt notification = getNotificationV2WithDocument(channelType, address);
        NotificationRecipientInt notificationRecipientInt = notification.getRecipients().get(0);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_DD_SEND), Mockito.anyString())).thenReturn(auditLogEvent);
        when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);
        Map<String, String> quickLinkTestMap = Map.of(notificationRecipientInt.getInternalId(), quickAccessToken);
        when(notificationService.getRecipientsQuickAccessLinkToken(notification.getIun())).thenReturn(quickLinkTestMap);

        DigitalAddressSourceInt addressSource = DigitalAddressSourceInt.PLATFORM;
        int recIndex = 0;
        int sentAttemptMade = 0;

        //GIVEN for attachmentUtils
        PnSendMode pnSendMode = PnSendMode.builder()
                .startConfigurationTime(Instant.now())
                .digitalSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS)
                .simpleRegisteredLetterSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS)
                .analogSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS)
                .aarTemplateTypeChooseStrategy(new StaticAarTemplateChooseStrategy(AarTemplateType.AAR_NOTIFICATION))
                .build();
        when(pnSendModeUtils.getPnSendMode(any())).thenReturn(pnSendMode);

        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl(aarKey).build();
        when(aarUtils.getAarGenerationDetails(any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);
        when(notificationUtils.getRecipientFromIndex(any(),anyInt())).thenReturn(notificationRecipientInt);

        when(aarUtils.getAarCreationRequestDetailsInt(any(NotificationInt.class),anyInt())).thenReturn(getAarCreationRequestDetailsInt(aarKey, recIndex));

        Instant now = Instant.now();
        if (channelType == LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.SERCQ) {
            mockGetTimelineElement(notification.getIun(), recIndex, now);
        }

        //WHEN
        final boolean isFirstSendRetry = false;

        SendInformation sendInformation = SendInformation.builder()
                .digitalAddress(notificationRecipientInt.getDigitalDomicile())
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

        //attachments result list
        List<String> attachmentsList = Arrays.asList(
                aarKey,
                "safestorage://"+notification.getDocuments().get(0).getRef().getKey(),
                "safestorage://"+notificationRecipientInt.getPayments().get(0).getPagoPA().getAttachment().getRef().getKey());

        ArgumentCaptor<LegalDigitalAddressInt> captor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        Mockito.verify(externalChannel).sendLegalNotification(eq(notification), eq(notificationRecipientInt), captor.capture(), eq(eventIdExpected), eq(attachmentsList), eq(quickAccessToken));
        if (channelType == LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.SERCQ) {
            Assertions.assertEquals(SERCQ_ADDRESS + "?timestamp=" + dateTimeFormatter.format(now), captor.getValue().getAddress());
        } else Assertions.assertEquals(address, captor.getValue().getAddress());
    }



    @ExtendWith(MockitoExtension.class)
    @ParameterizedTest
    @MethodSource("sendDigitalNotificationParams")
    void sendLegalNotificationAAR_DOCUMENTS(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE channelType, String address) {
        //GIVEN
        String quickAccessToken = "test";
        String aarKey = "testKey";

        NotificationInt notification = getNotificationV2WithDocument(channelType, address);
        NotificationRecipientInt notificationRecipientInt = notification.getRecipients().get(0);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_DD_SEND), Mockito.anyString())).thenReturn(auditLogEvent);
        when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);
        Map<String, String> quickLinkTestMap = Map.of(notificationRecipientInt.getInternalId(), quickAccessToken);
        when(notificationService.getRecipientsQuickAccessLinkToken(notification.getIun())).thenReturn(quickLinkTestMap);

        DigitalAddressSourceInt addressSource = DigitalAddressSourceInt.PLATFORM;
        int recIndex = 0;
        int sentAttemptMade = 0;

        //GIVEN for attachmentUtils
        PnSendMode pnSendMode = PnSendMode.builder()
                .startConfigurationTime(Instant.now())
                .digitalSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS)
                .simpleRegisteredLetterSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS)
                .analogSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS)
                .aarTemplateTypeChooseStrategy(new StaticAarTemplateChooseStrategy(AarTemplateType.AAR_NOTIFICATION))
                .build();
        when(pnSendModeUtils.getPnSendMode(any())).thenReturn(pnSendMode);

        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl(aarKey).build();
        when(aarUtils.getAarGenerationDetails(any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);
        when(notificationUtils.getRecipientFromIndex(any(),anyInt())).thenReturn(notificationRecipientInt);

        when(aarUtils.getAarCreationRequestDetailsInt(any(NotificationInt.class),anyInt())).thenReturn(getAarCreationRequestDetailsInt(aarKey, recIndex));

        Instant now = Instant.now();
        if (channelType == LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.SERCQ) {
            mockGetTimelineElement(notification.getIun(), recIndex, now);
        }

        //WHEN
        final boolean isFirstSendRetry = false;

        SendInformation sendInformation = SendInformation.builder()
                .digitalAddress(notificationRecipientInt.getDigitalDomicile())
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

        //attachments result list
        List<String> attachmentsList = Arrays.asList(
                aarKey,
                "safestorage://"+notification.getDocuments().get(0).getRef().getKey());

        ArgumentCaptor<LegalDigitalAddressInt> captor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        Mockito.verify(externalChannel).sendLegalNotification(eq(notification), eq(notificationRecipientInt), captor.capture(), eq(eventIdExpected), eq(attachmentsList), eq(quickAccessToken));
        if (channelType == LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.SERCQ) {
            Assertions.assertEquals(SERCQ_ADDRESS + "?timestamp=" + dateTimeFormatter.format(now), captor.getValue().getAddress());
        } else Assertions.assertEquals(address, captor.getValue().getAddress());
    }

    @ExtendWith(MockitoExtension.class)
    @ParameterizedTest
    @MethodSource("sendDigitalNotificationParams")
    void sendLegalNotificationAAR(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE channelType, String address) {
        //GIVEN
        String quickAccessToken = "test";
        String aarKey = "testKey";

        NotificationInt notification = getNotificationV2WithDocument(channelType, address);
        NotificationRecipientInt notificationRecipientInt = notification.getRecipients().get(0);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_DD_SEND), Mockito.anyString())).thenReturn(auditLogEvent);
        when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);
        Map<String, String> quickLinkTestMap = Map.of(notificationRecipientInt.getInternalId(), quickAccessToken);
        when(notificationService.getRecipientsQuickAccessLinkToken(notification.getIun())).thenReturn(quickLinkTestMap);

        DigitalAddressSourceInt addressSource = DigitalAddressSourceInt.PLATFORM;
        int recIndex = 0;
        int sentAttemptMade = 0;

        //GIVEN for attachmentUtils
        PnSendMode pnSendMode = PnSendMode.builder()
                .startConfigurationTime(Instant.now())
                .digitalSendAttachmentMode(SendAttachmentMode.AAR)
                .simpleRegisteredLetterSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS)
                .analogSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS)
                .aarTemplateTypeChooseStrategy(new StaticAarTemplateChooseStrategy(AarTemplateType.AAR_NOTIFICATION))
                .build();
        when(pnSendModeUtils.getPnSendMode(any())).thenReturn(pnSendMode);

        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl(aarKey).build();
        when(aarUtils.getAarGenerationDetails(any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);
        when(notificationUtils.getRecipientFromIndex(any(),anyInt())).thenReturn(notificationRecipientInt);

        when(aarUtils.getAarCreationRequestDetailsInt(any(NotificationInt.class),anyInt())).thenReturn(getAarCreationRequestDetailsInt(aarKey, recIndex));

        Instant now = Instant.now();
        if (channelType == LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.SERCQ) {
            mockGetTimelineElement(notification.getIun(), recIndex, now);
        }

        //WHEN
        final boolean isFirstSendRetry = false;

        SendInformation sendInformation = SendInformation.builder()
                .digitalAddress(notificationRecipientInt.getDigitalDomicile())
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

        //attachments result list
        List<String> attachmentsList = List.of(aarKey);
        ArgumentCaptor<LegalDigitalAddressInt> captor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);
        Mockito.verify(externalChannel).sendLegalNotification(eq(notification), eq(notificationRecipientInt), captor.capture(), eq(eventIdExpected), eq(attachmentsList), eq(quickAccessToken));
        if (channelType == LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.SERCQ) {
            Assertions.assertEquals(SERCQ_ADDRESS + "?timestamp=" + dateTimeFormatter.format(now), captor.getValue().getAddress());
        } else Assertions.assertEquals(address, captor.getValue().getAddress());
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void sendCourtesyNotification() {

        //GIVEN
        String quickAccessToken = "test";
        String aarKey = "testKey";
        int recIndex = 0;
        String eventId = "eventId";

        NotificationInt notification = getNotificationV2WithDocument();
        NotificationRecipientInt notificationRecipientInt = notification.getRecipients().get(0);

        when(notificationUtils.getRecipientFromIndex(any(NotificationInt.class), Mockito.anyInt())).thenReturn(notificationRecipientInt);

        CourtesyDigitalAddressInt courtesyDigitalAddress = CourtesyDigitalAddressInt.builder()
                .address("courtesyDigitalAddress@test.it")
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL)
                .build();

        Map<String, String> quickLinkTestMap = Map.of(notificationRecipientInt.getInternalId(), quickAccessToken);
        when(notificationService.getRecipientsQuickAccessLinkToken(notification.getIun())).thenReturn(quickLinkTestMap);
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_DA_SEND_EMAIL), Mockito.anyString(), Mockito.anyString())).thenReturn(auditLogEvent);
        when(auditLogEvent.generateSuccess()).thenReturn(auditLogEvent);

        //GIVEN for externalChannelUtils
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl(FileUtils.getKeyWithStoragePrefix(aarKey))
                .numberOfPages(1)
                .build();
        when(timelineService.getTimelineElementDetails(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(Optional.of( aarGenerationDetails ));


        //WHEN
        externalChannelService.sendCourtesyNotification(notification, courtesyDigitalAddress, recIndex, eventId);

        //THEN
        Mockito.verify(externalChannel).sendCourtesyNotification(notification, notificationRecipientInt,  courtesyDigitalAddress, eventId, FileUtils.getKeyWithStoragePrefix(aarKey), quickAccessToken);
        Mockito.verify( auditLogEvent).generateSuccess();
        Mockito.verify( auditLogEvent).log();
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

    private static AarCreationRequestDetailsInt getAarCreationRequestDetailsInt(String aarKey, int recIndex) {
        AarCreationRequestDetailsInt aarCreationRequestDetailsInt= new AarCreationRequestDetailsInt();
        aarCreationRequestDetailsInt.setAarKey(aarKey);
        aarCreationRequestDetailsInt.setAarTemplateType(AarTemplateType.AAR_NOTIFICATION_RADD);
        aarCreationRequestDetailsInt.setNumberOfPages(0);
        aarCreationRequestDetailsInt.setRecIndex(recIndex);
        return aarCreationRequestDetailsInt;
    }


}
