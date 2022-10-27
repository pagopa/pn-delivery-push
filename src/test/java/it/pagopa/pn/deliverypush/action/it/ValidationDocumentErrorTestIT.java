package it.pagopa.pn.deliverypush.action.it;

import it.pagopa.pn.commons.configs.IsMVPParameterConsumer;
import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.*;
import it.pagopa.pn.deliverypush.action.it.mockbean.*;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action.utils.*;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
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
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.Collections;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        StartWorkflowHandler.class,
        StartWorkflowForRecipientHandler.class,
        PnAuditLogBuilder.class,
        AnalogWorkflowHandler.class,
        ChooseDeliveryModeHandler.class,
        DigitalWorkFlowHandler.class,
        DigitalWorkFlowExternalChannelResponseHandler.class,
        CompletionWorkFlowHandler.class,
        PublicRegistryResponseHandler.class,
        PublicRegistryServiceImpl.class,
        ExternalChannelServiceImpl.class,
        IoServiceImpl.class,
        NotificationCostServiceImpl.class,
        SafeStorageServiceImpl.class,
        ExternalChannelResponseHandler.class,
        RefinementHandler.class,
        DigitalWorkFlowUtils.class,
        CourtesyMessageUtils.class,
        AarUtils.class,
        CompletelyUnreachableUtils.class,
        ExternalChannelUtils.class,
        AnalogWorkflowUtils.class,
        ChooseDeliveryModeUtils.class,
        TimelineUtils.class,
        StatusUtils.class,
        PublicRegistryUtils.class,
        NotificationUtils.class,
        CompletionWorkflowUtils.class,
        NotificationServiceImpl.class,
        StatusServiceImpl.class,
        PaperNotificationFailedServiceImpl.class,
        TimeLineServiceImpl.class,
        ConfidentialInformationServiceImpl.class,
        AddressBookServiceImpl.class,
        AttachmentUtils.class,
        PaperNotificationFailedDaoMock.class,
        TimelineDaoMock.class,
        ExternalChannelMock.class,
        PaperNotificationFailedDaoMock.class,
        PnDataVaultClientMock.class,
        PnDeliveryPushConfigs.class,
        IsMVPParameterConsumer.class,
        ValidationDocumentErrorTestIT.SpringTestConfiguration.class
})
@TestPropertySource(
        locations ="classpath:/application-test.properties",
        properties = "pn.delivery-push.validation-document-test=true"
)
@EnableConfigurationProperties(value = PnDeliveryPushConfigs.class)
class ValidationDocumentErrorTestIT {
    
    @TestConfiguration
    static class SpringTestConfiguration extends AbstractWorkflowTestConfiguration {
    }

    @Autowired
    private StartWorkflowHandler startWorkflowHandler;

    @Autowired
    private TimelineService timelineService;

    @Autowired
    private InstantNowSupplier instantNowSupplier;
    
    @SpyBean
    private ExternalChannelMock externalChannelMock;

    @Autowired
    private SafeStorageClientMock safeStorageClientMock;


    @Autowired
    private PnDeliveryClientMock pnDeliveryClientMock;

    @Autowired
    private UserAttributesClientMock addressBookMock;

    @Autowired
    private PublicRegistryMock publicRegistryMock;

    @Autowired
    private TimelineDaoMock timelineDaoMock;

    @Autowired
    private PaperNotificationFailedDaoMock paperNotificationFailedDaoMock;

    @Autowired
    private NotificationUtils notificationUtils;

    @Autowired
    private PnDataVaultClientMock pnDataVaultClientMock;

    @BeforeEach
    public void setup() {
        //Mock for get current date
        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());
        
        //Clear mock
        safeStorageClientMock.clear();
        pnDeliveryClientMock.clear();
        addressBookMock.clear();
        publicRegistryMock.clear();
        timelineDaoMock.clear();
        paperNotificationFailedDaoMock.clear();
        pnDataVaultClientMock.clear();
    }

    @Test
    void workflowTest() throws PnIdConflictException {
        // GIVEN
        
        // Platform address is present and all sending attempts fail
        LegalDigitalAddressInt platformAddress = LegalDigitalAddressInt.builder()
                .address("platformAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();
        
        //Special address is present and all sending attempts fail
        LegalDigitalAddressInt digitalDomicile = LegalDigitalAddressInt.builder()
                .address("digitalDomicile@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();
        
        //General address is present and all sending attempts fail
        LegalDigitalAddressInt pbDigitalAddress = LegalDigitalAddressInt.builder()
                .address("pbDigitalAddress@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
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

        byte[] differentFileSha = "error".getBytes();
        TestUtils.firstFileUploadFromNotificationError(notification, safeStorageClientMock, differentFileSha);
        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));
        publicRegistryMock.addDigital(recipient.getTaxId(), pbDigitalAddress);

        String iun = notification.getIun();
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //WHEN the workflow start
        startWorkflowHandler.startWorkflow(iun);
        
        //THEN
        
        //Check worfklow is failed
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.REQUEST_REFUSED.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex)
                                .build())).isPresent());
        
        Mockito.verify(externalChannelMock, Mockito.times(0)).sendLegalNotification(Mockito.any(NotificationInt.class), Mockito.any(NotificationRecipientInt.class), Mockito.any(LegalDigitalAddressInt.class), Mockito.anyString());
        Mockito.verify(externalChannelMock, Mockito.times(0)).sendAnalogNotification(Mockito.any(NotificationInt.class), Mockito.any(NotificationRecipientInt.class), Mockito.any(PhysicalAddressInt.class), Mockito.anyString(), Mockito.any(), Mockito.anyString());
    }


}
