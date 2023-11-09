package it.pagopa.pn.deliverypush.action.it;

import it.pagopa.pn.deliverypush.action.choosedeliverymode.ChooseDeliveryModeUtils;
import it.pagopa.pn.deliverypush.action.it.mockbean.AddressManagerClientMock;
import it.pagopa.pn.deliverypush.action.it.mockbean.ExternalChannelMock;
import it.pagopa.pn.deliverypush.action.it.mockbean.PaperChannelMock;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action.startworkflow.StartWorkflowHandler;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator;
import it.pagopa.pn.deliverypush.logtest.ConsoleAppenderCustom;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelSendRequest;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.util.List;

import static org.awaitility.Awaitility.await;

class AnalogTestNormalizedAddressIT extends CommonTestConfiguration{
    @SpyBean
    LegalFactGenerator legalFactGenerator;
    @SpyBean
    PaperChannelMock paperChannelMock;
    @Autowired
    StartWorkflowHandler startWorkflowHandler;
    @Autowired
    TimelineService timelineService;
    @Autowired
    NotificationUtils notificationUtils;
    @Autowired
    NotificationService notificationService;

    @Test
    void sendAnalogSuccessWithNormalizedAddress() {
    /*
       - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)

       - Pa physical address presente con struttura indirizzo che porta al successo del primo invio
     */

        PhysicalAddressInt paPhysicalAddress = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS + " Via Nuova_" + AddressManagerClientMock.ADDRESS_MANAGER_TO_NORMALIZE)
                .build();

        String iun = TestUtils.getRandomIun();

        String taxId = "test_tax_id";
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withInternalId("ANON_"+taxId)
                .withPhysicalAddress(paPhysicalAddress)
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withIun(iun)
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .build();

        TestUtils.firstFileUploadFromNotification(listDocumentWithContent, safeStorageClientMock);
        
        pnDeliveryClientMock.addNotification(notification);

        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        PhysicalAddressInt paPhysicalAddressNormalized =  PhysicalAddressInt.builder()
                .address(paPhysicalAddress.getAddress() +"_NORMALIZED")
                .addressDetails(paPhysicalAddress.getAddressDetails() +"_NORMALIZED")
                .foreignState(paPhysicalAddress.getForeignState() +"_NORMALIZED")
                .province(paPhysicalAddress.getProvince() +"_NORMALIZED")
                .municipality(paPhysicalAddress.getMunicipality() +"_NORMALIZED")
                .zip(paPhysicalAddress.getZip() +"_NORMALIZED")
                .municipalityDetails(paPhysicalAddress.getMunicipalityDetails() +"_NORMALIZED")
                .fullname(recipient.getDenomination())
                .at(paPhysicalAddress.getAt())
                .build();
        
        addressManagerClientMock.addNormalizedAddress(iun, recIndex, paPhysicalAddressNormalized );

        
        //Start del workflow
        startWorkflowHandler.startWorkflow(iun);
        
        String timelineId = TimelineEventId.REFINEMENT.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build()
        );

        // Viene atteso fino a che l'ultimo elemento di timeline sia stato inserito per procedere con le successive verifiche
        await().untilAsserted(() ->
                Assertions.assertTrue(timelineService.getTimelineElement(iun, timelineId).isPresent())
        );
        
        //Ottengo la notifica normalizzata
        NotificationInt notificationNormalized = notificationService.getNotificationByIun(notification.getIun());
        //Ottengo il recpient normalizzato
        NotificationRecipientInt recipientNormalized = notificationUtils.getRecipientFromIndex(notificationNormalized, recIndex);
        

        //Viene verificato che gli indirizzi PLATFORM SPECIAL E GENERAL non siano presenti
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificata la presenza del primo invio verso external channel e che l'invio sia avvenuto con l'indirizzo fornito dalla PA
        TestUtils.checkSendPaperToExtChannel(iun, recIndex, paPhysicalAddressNormalized, 0, timelineService);
        
        Mockito.verify(paperChannelMock, Mockito.times(1)).send(Mockito.any(PaperChannelSendRequest.class));
        
        //Viene effettuato il check dei legalFacts generati
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(false)
                .build();

        TestUtils.checkGeneratedLegalFacts(
                notificationNormalized,
                recipientNormalized,
                recIndex,
                0,
                generatedLegalFactsInfo,
                EndWorkflowStatus.SUCCESS,
                legalFactGenerator,
                timelineService,
                null
        );

        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs();
    }

}
