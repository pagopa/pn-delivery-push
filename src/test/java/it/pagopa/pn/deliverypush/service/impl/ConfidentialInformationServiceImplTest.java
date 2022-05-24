package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.AddressDto;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.AnalogDomicile;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.ConfidentialTimelineElementDto;
import it.pagopa.pn.deliverypush.dto.ext.datavault.ConfidentialTimelineElementDtoInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.externalclient.pnclient.datavault.PnDataVaultClient;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.SendPaperDetails;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetails;
import it.pagopa.pn.deliverypush.service.ConfidentialInformationService;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfidentialInformationServiceImplTest {
    private ConfidentialInformationService confidentialInformationService;
    private PnDataVaultClient pnDataVaultClient;
    
    @BeforeEach
    void setup() {
        pnDataVaultClient = Mockito.mock( PnDataVaultClient.class );

        confidentialInformationService = new ConfidentialInformationServiceImpl(
                pnDataVaultClient
        );

    }
    
    @Test
    void saveTimelineConfidentialInformation() {
        String iun = "testIun";
        String elementId = "testElementId";
        
        //GIVEN
        TimelineElementInternal element = getSendPaperDetailsTimelineElement(iun, elementId);
        
        Mockito.when(pnDataVaultClient.updateNotificationTimelineByIunAndTimelineElementId(Mockito.anyString(), Mockito.any(ConfidentialTimelineElementDto.class)))
                .thenReturn(ResponseEntity.ok(null));

        //WHEN
        confidentialInformationService.saveTimelineConfidentialInformation(element);
        
        //THEN
        ArgumentCaptor<ConfidentialTimelineElementDto> confDtoCaptor = ArgumentCaptor.forClass(ConfidentialTimelineElementDto.class);

        Mockito.verify(pnDataVaultClient).updateNotificationTimelineByIunAndTimelineElementId(Mockito.eq(iun), confDtoCaptor.capture());

        ConfidentialTimelineElementDto capturedDto = confDtoCaptor.getValue();
        Assertions.assertNotNull( capturedDto.getPhysicalAddress() );
        Assertions.assertEquals( element.getDetails().getPhysicalAddress().getAddress(), capturedDto.getPhysicalAddress().getAddress() );
    }

    @Test
    void saveTimelineConfidentialInformationError() {
        String iun = "testIun";
        String elementId = "testElementId";

        //GIVEN
        TimelineElementInternal element = getSendPaperDetailsTimelineElement(iun, elementId);
        
        Mockito.when(pnDataVaultClient.updateNotificationTimelineByIunAndTimelineElementId(Mockito.anyString(), Mockito.any(ConfidentialTimelineElementDto.class)))
                .thenReturn( ResponseEntity.status(500).body(null) );

        //WHEN
        assertThrows(PnInternalException.class, () -> {
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
        ResponseEntity<ConfidentialTimelineElementDto> resp = ResponseEntity.ok(elementDto);

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
                .thenReturn(ResponseEntity.status(400).body(null));
        
        //WHEN
        assertThrows(PnInternalException.class, () -> {
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
        
        ResponseEntity<List<ConfidentialTimelineElementDto>> resp = ResponseEntity.ok(list);

        Mockito.when(pnDataVaultClient.getNotificationTimelineByIunWithHttpInfo(Mockito.anyString()))
                .thenReturn(resp);

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
    void getTimelineConfidentialInformationKo() {
        //GIVEN
        String iun = "testIun";

        Mockito.when(pnDataVaultClient.getNotificationTimelineByIunWithHttpInfo(Mockito.anyString()))
                .thenReturn(ResponseEntity.status(500).body(null));

        //WHEN
        assertThrows(PnInternalException.class, () -> {
            confidentialInformationService.getTimelineConfidentialInformation(iun);
        });
    }
    
    private TimelineElementInternal getSendPaperDetailsTimelineElement(String iun, String elementId) {
        SendPaperDetails details = SendPaperDetails.builder()
                .physicalAddress(
                        PhysicalAddress.builder()
                                .province("province")
                                .municipality("munic")
                                .at("at")
                                .build()
                )
                .investigation(true)
                .recIndex(0)
                .sentAttemptMade(0)
                .build();
        return TimelineElementInternal.timelineInternalBuilder()
                .elementId(elementId)
                .iun(iun)
                .details(SmartMapper.mapToClass(details, TimelineElementDetails.class))
                .build();
    }

}