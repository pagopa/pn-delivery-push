package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowUtils;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypush.action.utils.AarUtils;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.PaperChannelUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelSendClient;
import it.pagopa.pn.deliverypush.service.AuditLogService;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypush.service.PaperChannelService;
import it.pagopa.pn.deliverypush.service.SafeStorageService;
import it.pagopa.pn.deliverypush.utils.PaperSendModeUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

class PaperChannelServiceAltTest {
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
    private AuditLogService auditLogService;

    private PaperChannelService paperChannelService;

    private AttachmentUtils attachmentUtils;

    @Mock
    SafeStorageService safeStorageService;
    @Mock
    PnDeliveryPushConfigs pnDeliveryPushConfigs;
    @Mock
    NotificationProcessCostService notificationProcessCostService;
    @Mock
    PaperSendModeUtils paperSendModeUtils;

    @BeforeEach
    void setup() {
        attachmentUtils = new AttachmentUtils(
                safeStorageService,
                pnDeliveryPushConfigs,
                notificationProcessCostService,
                paperSendModeUtils
        );

        paperChannelService = new PaperChannelServiceImpl(
                paperChannelUtils,
                paperChannelSendClient,
                notificationUtils,
                aarUtils,
                timelineUtils,
                mvpParameterConsumer,
                analogWorkflowUtils,
                auditLogService,
                attachmentUtils);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationErrorNoConfiguration() {
        //GIVEN
        NotificationInt notificationInt = newNotification("taxid");

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);

        Mockito.when(paperChannelUtils.buildPrepareAnalogDomicileEventId(Mockito.any(), Mockito.anyInt(), Mockito.anyInt())).thenReturn("timeline_id_1");

        Mockito.when(paperSendModeUtils.getPaperSendMode(Mockito.any())).thenReturn(null);

        // WHEN
        Assertions.assertThrows(PnInternalException.class, () -> paperChannelService.prepareAnalogNotification(notificationInt, 0, 1));
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
}
