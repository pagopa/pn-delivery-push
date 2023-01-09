package it.pagopa.pn.deliverypush.service.impl;

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
import it.pagopa.pn.deliverypush.service.ExternalChannelService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

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

    private ExternalChannelService externalChannelService;

    @BeforeEach
    void setup() {
        externalChannelService = new ExternalChannelServiceImpl(
                externalChannelUtils,
                externalChannel,
                notificationUtils,
                digitalWorkFlowUtils,
                notificationService);
    }

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

        String aarKey = "testKey";
        Mockito.when( externalChannelUtils.getAarKey(Mockito.anyString(), Mockito.anyInt()) ).thenReturn(aarKey);
        
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
        
        Mockito.verify(externalChannel).sendLegalNotification(notification, recipient,  digitalDomicile, eventIdExpected, aarKey);
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

        String aarKey = "testKey";
        Mockito.when( externalChannelUtils.getAarKey(Mockito.anyString(), Mockito.anyInt()) ).thenReturn(aarKey);

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

        Mockito.verify(externalChannel).sendLegalNotification(notification, recipient,  digitalDomicile, eventIdExpected, aarKey);
        
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

        String aarKey = "testKey";
        Mockito.when( externalChannelUtils.getAarKey(Mockito.anyString(), Mockito.anyInt()) ).thenReturn(aarKey);

        int recIndex = 0;
        String eventId = "eventId";
        
        //WHEN
        externalChannelService.sendCourtesyNotification(notification, courtesyDigitalAddress, recIndex, eventId);

        //THEN
        Mockito.verify(externalChannel).sendCourtesyNotification(notification, recipient,  courtesyDigitalAddress, eventId, aarKey);
    }

}