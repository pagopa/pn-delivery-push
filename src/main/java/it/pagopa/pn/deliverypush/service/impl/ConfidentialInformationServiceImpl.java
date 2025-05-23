package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.dto.ext.datavault.BaseRecipientDtoInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.ConfidentialTimelineElementDtoInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.NotificationRecipientAddressesDtoInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.model.ConfidentialTimelineElementDto;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.model.NotificationRecipientAddressesDto;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.datavault.PnDataVaultClient;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.datavault.PnDataVaultClientReactive;
import it.pagopa.pn.deliverypush.service.ConfidentialInformationService;
import it.pagopa.pn.deliverypush.service.mapper.ConfidentialTimelineElementDtoMapper;
import it.pagopa.pn.deliverypush.service.mapper.NotificationRecipientAddressesDtoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ConfidentialInformationServiceImpl implements ConfidentialInformationService {
    private final PnDataVaultClient pnDataVaultClient;
    private final PnDataVaultClientReactive pnDataVaultClientReactive;
    
    public ConfidentialInformationServiceImpl(PnDataVaultClient pnDataVaultClient,
                                              PnDataVaultClientReactive pnDataVaultClientReactive) {
        this.pnDataVaultClient = pnDataVaultClient;
        this.pnDataVaultClientReactive = pnDataVaultClientReactive;
    }

    @Override
    public void saveTimelineConfidentialInformation(TimelineElementInternal timelineElement) {
        String iun = timelineElement.getIun();

        if (timelineElement.getDetails() instanceof ConfidentialInformationTimelineElement) {

            ConfidentialTimelineElementDtoInt dtoInt = getConfidentialDtoFromTimeline(timelineElement);

            ConfidentialTimelineElementDto dtoExt = ConfidentialTimelineElementDtoMapper.internalToExternal(dtoInt);

            pnDataVaultClient.updateNotificationTimelineByIunAndTimelineElementId(iun, dtoExt);

            log.debug("UpdateNotificationTimelineByIunAndTimelineElementId OK for - iun {} timelineElementId {}", iun, dtoInt.getTimelineElementId());
        }
    }

    private ConfidentialTimelineElementDtoInt getConfidentialDtoFromTimeline(TimelineElementInternal timelineElement) {
        TimelineElementDetailsInt details = timelineElement.getDetails();

        ConfidentialTimelineElementDtoInt.ConfidentialTimelineElementDtoIntBuilder builder = ConfidentialTimelineElementDtoInt.builder()
                .timelineElementId(timelineElement.getElementId());

        if (details instanceof CourtesyAddressRelatedTimelineElement courtesyDetails && courtesyDetails.getDigitalAddress() != null) {
            builder.digitalAddress(courtesyDetails.getDigitalAddress().getAddress());
        }

        if (details instanceof DigitalAddressRelatedTimelineElement digitalDetails && digitalDetails.getDigitalAddress() != null) {
            builder.digitalAddress(digitalDetails.getDigitalAddress().getAddress());
        }

        if (details instanceof PhysicalAddressRelatedTimelineElement physicalDetails && physicalDetails.getPhysicalAddress() != null) {
            builder.physicalAddress(physicalDetails.getPhysicalAddress());
        }

        if (details instanceof NewAddressRelatedTimelineElement newAddressDetails && newAddressDetails.getNewAddress() != null) {
            builder.newPhysicalAddress(newAddressDetails.getNewAddress());
        }
        
        if(details instanceof PersonalInformationRelatedTimelineElement personalInfoDetails){
            if(personalInfoDetails.getTaxId() != null){
                builder.taxId(personalInfoDetails.getTaxId());
            }
            if(personalInfoDetails.getDenomination() != null){
                builder.denomination(personalInfoDetails.getDenomination());
            }
        }

        return builder.build();
    }

    @Override
    public Optional<ConfidentialTimelineElementDtoInt> getTimelineElementConfidentialInformation(String iun, String timelineElementId) {
      ConfidentialTimelineElementDto dtoExt = pnDataVaultClient.getNotificationTimelineByIunAndTimelineElementId(iun, timelineElementId);


      log.debug("getTimelineElementConfidentialInformation OK for - iun {} timelineElementId {}", iun, timelineElementId);
         

      if (dtoExt != null) {
         return Optional.of(ConfidentialTimelineElementDtoMapper.externalToInternal(dtoExt));
      }

      log.debug("getTimelineElementConfidentialInformation haven't confidential information for - iun {} timelineElementId {}", iun, timelineElementId);
      return Optional.empty();
        
    }

    @Override
    public Optional<Map<String, ConfidentialTimelineElementDtoInt>> getTimelineConfidentialInformation(String iun) {
        List<ConfidentialTimelineElementDto> listDtoExt = pnDataVaultClient.getNotificationTimelineByIunWithHttpInfo(iun);

        log.debug("getTimelineConfidentialInformation OK for - iun {} ", iun);
      
        if (listDtoExt != null && !listDtoExt.isEmpty()) {
            return Optional.of(
                    listDtoExt.stream()
                            .map(ConfidentialTimelineElementDtoMapper::externalToInternal)
                            .collect(Collectors.toMap(ConfidentialTimelineElementDtoInt::getTimelineElementId, Function.identity()))
            );
        }
        log.debug("getTimelineConfidentialInformation haven't confidential information for - iun {} ", iun);
        return Optional.empty();
       
    }
    
    @Override
    public Mono<BaseRecipientDtoInt> getRecipientInformationByInternalId(String internalId) {
        return pnDataVaultClientReactive.getRecipientsDenominationByInternalId(List.of(internalId))
                .filter( el -> internalId.equals(el.getInternalId()))
                .map( el -> BaseRecipientDtoInt.builder()
                        .taxId(el.getTaxId())
                        .denomination(el.getDenomination())
                        .internalId(el.getInternalId())
                        .recipientType(el.getRecipientType() != null ? RecipientTypeInt.valueOf(el.getRecipientType().getValue()) : null)
                        .internalId(el.getInternalId())
                        .build()
                ).collectList()
                .map(list -> {
                    if(list != null && !list.isEmpty())
                        return list.get(0);
                    else 
                        return null;
                });
    }
    
    @Override
    public Mono<Void> updateNotificationAddresses(String iun, Boolean normalized, List<NotificationRecipientAddressesDtoInt> listAddressDtoInt){
        log.debug("Start updateNotificationAddresses - iun={}", iun);

        List<NotificationRecipientAddressesDto> listAddressExt = listAddressDtoInt.stream().map(
                NotificationRecipientAddressesDtoMapper::internalToExternal
        ).toList();
        
        return pnDataVaultClientReactive.updateNotificationAddressesByIun(iun, normalized, listAddressExt);
    }
}
