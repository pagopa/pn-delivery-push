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
import it.pagopa.pn.deliverypush.dto.address.SendInformation;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.SendAttachmentMode;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarGenerationDetailsInt;
import it.pagopa.pn.deliverypush.legalfacts.AarTemplateType;
import it.pagopa.pn.deliverypush.legalfacts.StaticAarTemplateChooseStrategy;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalchannel.ExternalChannelSendClient;
import it.pagopa.pn.deliverypush.service.*;
import it.pagopa.pn.deliverypush.service.utils.FileUtils;
import it.pagopa.pn.deliverypush.utils.PnSendMode;
import it.pagopa.pn.deliverypush.utils.PnSendModeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static it.pagopa.pn.deliverypush.action.it.utils.TestUtils.getNotificationV2WithDocument;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

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
    private ExternalChannelService externalChannelService;


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
                attachmentUtils);
    }



    @ExtendWith(MockitoExtension.class)
    @Test
    void sendLegalNotificationAAR_DOCUMENTS_PAYMENTS() {
        //GIVEN
        String quickAccessToken = "test";
        String aarKey = "testKey";

        NotificationInt notification = getNotificationV2WithDocument();
        NotificationRecipientInt notificationRecipientInt = notification.getRecipients().get(0);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_DD_SEND), Mockito.anyString())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);
        Map<String, String> quickLinkTestMap = Map.of(notificationRecipientInt.getInternalId(), quickAccessToken);
        Mockito.when(notificationService.getRecipientsQuickAccessLinkToken(notification.getIun())).thenReturn(quickLinkTestMap);

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
        Mockito.when(pnSendModeUtils.getPnSendMode(any())).thenReturn(pnSendMode);

        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl(aarKey).build();
        Mockito.when(aarUtils.getAarGenerationDetails(any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);
        Mockito.when(notificationUtils.getRecipientFromIndex(any(),anyInt())).thenReturn(notificationRecipientInt);


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

        Mockito.verify(externalChannel).sendLegalNotification(notification, notificationRecipientInt,  notificationRecipientInt.getDigitalDomicile(), eventIdExpected, attachmentsList, quickAccessToken);
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void sendLegalNotificationAAR_DOCUMENTS() {
        //GIVEN
        String quickAccessToken = "test";
        String aarKey = "testKey";

        NotificationInt notification = getNotificationV2WithDocument();
        NotificationRecipientInt notificationRecipientInt = notification.getRecipients().get(0);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_DD_SEND), Mockito.anyString())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);
        Map<String, String> quickLinkTestMap = Map.of(notificationRecipientInt.getInternalId(), quickAccessToken);
        Mockito.when(notificationService.getRecipientsQuickAccessLinkToken(notification.getIun())).thenReturn(quickLinkTestMap);

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
        Mockito.when(pnSendModeUtils.getPnSendMode(any())).thenReturn(pnSendMode);

        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl(aarKey).build();
        Mockito.when(aarUtils.getAarGenerationDetails(any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);
        Mockito.when(notificationUtils.getRecipientFromIndex(any(),anyInt())).thenReturn(notificationRecipientInt);


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

        Mockito.verify(externalChannel).sendLegalNotification(notification, notificationRecipientInt,  notificationRecipientInt.getDigitalDomicile(), eventIdExpected, attachmentsList, quickAccessToken);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendLegalNotificationAAR() {
        //GIVEN
        String quickAccessToken = "test";
        String aarKey = "testKey";

        NotificationInt notification = getNotificationV2WithDocument();
        NotificationRecipientInt notificationRecipientInt = notification.getRecipients().get(0);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_DD_SEND), Mockito.anyString())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);
        Map<String, String> quickLinkTestMap = Map.of(notificationRecipientInt.getInternalId(), quickAccessToken);
        Mockito.when(notificationService.getRecipientsQuickAccessLinkToken(notification.getIun())).thenReturn(quickLinkTestMap);

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
        Mockito.when(pnSendModeUtils.getPnSendMode(any())).thenReturn(pnSendMode);

        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl(aarKey).build();
        Mockito.when(aarUtils.getAarGenerationDetails(any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);
        Mockito.when(notificationUtils.getRecipientFromIndex(any(),anyInt())).thenReturn(notificationRecipientInt);


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
        Mockito.verify(externalChannel).sendLegalNotification(notification, notificationRecipientInt,  notificationRecipientInt.getDigitalDomicile(), eventIdExpected, attachmentsList, quickAccessToken);
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

        Mockito.when(notificationUtils.getRecipientFromIndex(any(NotificationInt.class), Mockito.anyInt())).thenReturn(notificationRecipientInt);

        CourtesyDigitalAddressInt courtesyDigitalAddress = CourtesyDigitalAddressInt.builder()
                .address("courtesyDigitalAddress@test.it")
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL)
                .build();

        Map<String, String> quickLinkTestMap = Map.of(notificationRecipientInt.getInternalId(), quickAccessToken);
        Mockito.when(notificationService.getRecipientsQuickAccessLinkToken(notification.getIun())).thenReturn(quickLinkTestMap);
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_DA_SEND_EMAIL), Mockito.anyString(), Mockito.anyString())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess()).thenReturn(auditLogEvent);

        //GIVEN for externalChannelUtils
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl(FileUtils.getKeyWithStoragePrefix(aarKey))
                .numberOfPages(1)
                .build();
        Mockito.when(timelineService.getTimelineElementDetails(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(Optional.of( aarGenerationDetails ));


        //WHEN
        externalChannelService.sendCourtesyNotification(notification, courtesyDigitalAddress, recIndex, eventId);

        //THEN
        Mockito.verify(externalChannel).sendCourtesyNotification(notification, notificationRecipientInt,  courtesyDigitalAddress, eventId, FileUtils.getKeyWithStoragePrefix(aarKey), quickAccessToken);
        Mockito.verify( auditLogEvent).generateSuccess();
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(any());

    }

}
