package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.AddressDto;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.AnalogDomicile;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.BaseRecipientDto;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.ConfidentialTimelineElementDto;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.BaseRecipientDtoInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.ConfidentialTimelineElementDtoInt;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationViewedDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogDetailsInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.datavault.PnDataVaultClient;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.datavault.PnDataVaultClientReactive;
import it.pagopa.pn.deliverypush.service.ConfidentialInformationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfidentialInformationServiceImplTest {
    private ConfidentialInformationService confidentialInformationService;
    private PnDataVaultClient pnDataVaultClient;
    private PnDataVaultClientReactive pnDataVaultClientReactive;
    
    @BeforeEach
    void setup() {
        pnDataVaultClient = Mockito.mock( PnDataVaultClient.class );
        pnDataVaultClientReactive = Mockito.mock( PnDataVaultClientReactive.class );

        confidentialInformationService = new ConfidentialInformationServiceImpl(
                pnDataVaultClient,
                pnDataVaultClientReactive);

    }
    
    @Test
    void saveTimelineConfidentialInformation() {
        String iun = "testIun";
        String elementId = "testElementId";
        
        //GIVEN
        TimelineElementInternal element = getSendPaperDetailsTimelineElement(iun, elementId);
        
        //Mockito.when(pnDataVaultClient.updateNotificationTimelineByIunAndTimelineElementId(Mockito.anyString(), Mockito.any(ConfidentialTimelineElementDto.class)))
       // .doNothing();

        //WHEN
        confidentialInformationService.saveTimelineConfidentialInformation(element);
        
        //THEN
        ArgumentCaptor<ConfidentialTimelineElementDto> confDtoCaptor = ArgumentCaptor.forClass(ConfidentialTimelineElementDto.class);

        Mockito.verify(pnDataVaultClient).updateNotificationTimelineByIunAndTimelineElementId(Mockito.eq(iun), confDtoCaptor.capture());

        ConfidentialTimelineElementDto capturedDto = confDtoCaptor.getValue();
        Assertions.assertNotNull( capturedDto.getPhysicalAddress() );
        Assertions.assertEquals( ((SendAnalogDetailsInt) element.getDetails()).getPhysicalAddress().getAddress(), capturedDto.getPhysicalAddress().getAddress() );
    }

    @Test
    void saveTimelineConfidentialInformationNotificationViewed() {
        String iun = "testIun";
        String elementId = "testElementId";

        //GIVEN
        TimelineElementInternal element = notificationViewedDetails(iun, elementId);
        
        //WHEN
        confidentialInformationService.saveTimelineConfidentialInformation(element);

        //THEN
        ArgumentCaptor<ConfidentialTimelineElementDto> confDtoCaptor = ArgumentCaptor.forClass(ConfidentialTimelineElementDto.class);

        Mockito.verify(pnDataVaultClient).updateNotificationTimelineByIunAndTimelineElementId(Mockito.eq(iun), confDtoCaptor.capture());

        ConfidentialTimelineElementDto capturedDto = confDtoCaptor.getValue();
        Assertions.assertNotNull( capturedDto.getTaxId() );
        Assertions.assertNotNull( capturedDto.getDenomination() );
    }

    @Test
    void saveTimelineConfidentialInformationError() {
        String iun = "testIun";
        String elementId = "testElementId";

        //GIVEN
        TimelineElementInternal element = getSendPaperDetailsTimelineElement(iun, elementId);
        
        Mockito.doThrow(PnHttpResponseException.class).when(pnDataVaultClient).updateNotificationTimelineByIunAndTimelineElementId(Mockito.anyString(), Mockito.any(ConfidentialTimelineElementDto.class));
        

        //WHEN
        assertThrows(PnHttpResponseException.class, () -> {
            confidentialInformationService.saveTimelineConfidentialInformation(element);
        });
    }


    @Test
    void getTimelineElementConfidentialInformation() {
        //GIVEN
        String iun = "testIun";
        String elementId = "testElementId";

        ConfidentialTimelineElementDto elementDto = ConfidentialTimelineElementDto.builder()
                .digitalAddress(
                        AddressDto.builder()
                                .value("indirizzo@test.com")
                                .build()
                )
                .build();
        ConfidentialTimelineElementDto resp = elementDto;

        Mockito.when(pnDataVaultClient.getNotificationTimelineByIunAndTimelineElementId(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(resp);

        //WHEN
        Optional<ConfidentialTimelineElementDtoInt> optConf =  confidentialInformationService.getTimelineElementConfidentialInformation(iun, elementId);

        //THEN
        Assertions.assertTrue(optConf.isPresent());
        Assertions.assertEquals(optConf.get().getDigitalAddress(), elementDto.getDigitalAddress().getValue());
    }
    
    @Test
    void getTimelineElementConfidentialInformationKo() {
        //GIVEN
        String iun = "testIun";
        String elementId = "testElementId";

        Mockito.when(pnDataVaultClient.getNotificationTimelineByIunAndTimelineElementId(Mockito.anyString(), Mockito.anyString()))
                .thenThrow(PnHttpResponseException.class);
        
        //WHEN
        assertThrows(PnHttpResponseException.class, () -> {
            confidentialInformationService.getTimelineElementConfidentialInformation(iun, elementId);
        });
    }
    
    @Test
    void getTimelineConfidentialInformation() {
        //GIVEN
        String iun = "testIun";
        String elementId1 = "elementId1";
        String elementId2 = "elementId2";

        ConfidentialTimelineElementDto elementDto1 = ConfidentialTimelineElementDto.builder()
                .timelineElementId(elementId1)
                .digitalAddress(
                        AddressDto.builder()
                                .value("indirizzo@test.com")
                                .build()
                )
                .build();
        ConfidentialTimelineElementDto elementDto2 = ConfidentialTimelineElementDto.builder()
                .timelineElementId(elementId2)
                .newPhysicalAddress(AnalogDomicile.builder()
                        .cap("80010")
                        .province("NA")
                        .addressDetails("Scala 41")
                        .state("IT")
                        .municipality("MO")
                        .address("Via Vecchia")
                        .build())
                .build();
        List<ConfidentialTimelineElementDto> list = new ArrayList<>();
        list.add(elementDto1);
        list.add(elementDto2);
        

        Mockito.when(pnDataVaultClient.getNotificationTimelineByIunWithHttpInfo(Mockito.anyString()))
                .thenReturn(list);

        //WHEN
        Optional<Map<String, ConfidentialTimelineElementDtoInt>> mapOtp = confidentialInformationService.getTimelineConfidentialInformation(iun);
                
        //THEN
        Assertions.assertTrue(mapOtp.isPresent());

        Assertions.assertNotNull(mapOtp.get().get(elementId1));
        Assertions.assertEquals(mapOtp.get().get(elementId1).getDigitalAddress(), elementDto1.getDigitalAddress().getValue());

        Assertions.assertNotNull(mapOtp.get().get(elementId2));
        Assertions.assertEquals(mapOtp.get().get(elementId2).getNewPhysicalAddress().getAddress(), elementDto2.getNewPhysicalAddress().getAddress());
    }

    @Test
    void getRecipientInformationByInternalId() {
        //GIVEN
        String internalId = "internalId";
        String taxId = "testTaxId";
        String denomination = "denomination1";
        
        Flux<BaseRecipientDto> flux = Flux.just(BaseRecipientDto.builder()
                        .taxId(taxId)
                        .internalId(internalId)
                        .denomination(denomination)
                .build());
        Mockito.when(pnDataVaultClientReactive.getRecipientsDenominationByInternalId(Mockito.any())).thenReturn(flux);
        Mono<BaseRecipientDtoInt> monoBaseRec = confidentialInformationService.getRecipientInformationByInternalId(internalId);

        BaseRecipientDtoInt baseRecipientDto = monoBaseRec.block();
        Assertions.assertNotNull(baseRecipientDto);
        Assertions.assertEquals(taxId, baseRecipientDto.getTaxId());
        Assertions.assertEquals(denomination, baseRecipientDto.getDenomination());
    }
    
    @Test
    void getTimelineConfidentialInformationKo() {
        //GIVEN
        String iun = "testIun";

        Mockito.when(pnDataVaultClient.getNotificationTimelineByIunWithHttpInfo(Mockito.anyString()))
                .thenThrow(PnHttpResponseException.class);

        //WHEN
        assertThrows(PnHttpResponseException.class, () -> {
            confidentialInformationService.getTimelineConfidentialInformation(iun);
        });
    }
    
    private TimelineElementInternal getSendPaperDetailsTimelineElement(String iun, String elementId) {
         SendAnalogDetailsInt details =  SendAnalogDetailsInt.builder()
                .physicalAddress(
                        PhysicalAddressInt.builder()
                                .province("province")
                                .municipality("munic")
                                .at("at")
                                .build()
                )
                .relatedRequestId("abc")
                .analogCost(100)
                .recIndex(0)
                .sentAttemptMade(0)
                .build();
         
        return TimelineElementInternal.builder()
                .elementId(elementId)
                .iun(iun)
                .details( details )
                .build();
    }

    private TimelineElementInternal notificationViewedDetails(String iun, String elementId) {
        NotificationViewedDetailsInt details =  NotificationViewedDetailsInt.builder()
                .notificationCost(100)
                .recIndex(0)
                .raddTransactionId("154")
                .delegateInfo(DelegateInfoInt.builder()
                        .internalId("idInterno")
                        .denomination("test")
                        .taxId("prova")
                        .build())
                .build();

        return TimelineElementInternal.builder()
                .elementId(elementId)
                .iun(iun)
                .details( details )
                .build();
    }

}