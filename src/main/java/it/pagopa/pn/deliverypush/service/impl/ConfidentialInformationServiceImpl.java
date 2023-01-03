package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.BaseRecipientDto;
import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.ConfidentialTimelineElementDto;
import it.pagopa.pn.deliverypush.dto.ext.datavault.BaseRecipientDtoInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.ConfidentialTimelineElementDtoInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.datavault.PnDataVaultClient;
import it.pagopa.pn.deliverypush.service.ConfidentialInformationService;
import it.pagopa.pn.deliverypush.service.mapper.ConfidentialTimelineElementDtoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ConfidentialInformationServiceImpl implements ConfidentialInformationService {
    private final PnDataVaultClient pnDataVaultClient;

    public ConfidentialInformationServiceImpl(PnDataVaultClient pnDataVaultClient) {
        this.pnDataVaultClient = pnDataVaultClient;
    }

    @Override
    public void saveTimelineConfidentialInformation(TimelineElementInternal timelineElement) {
        String iun = timelineElement.getIun();

        if (checkPresenceConfidentialInformation(timelineElement)) {

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

    private boolean checkPresenceConfidentialInformation(TimelineElementInternal timelineElementInternal) {
        TimelineElementDetailsInt details = timelineElementInternal.getDetails();

        return details instanceof CourtesyAddressRelatedTimelineElement ||
                details instanceof DigitalAddressRelatedTimelineElement ||
                details instanceof PhysicalAddressRelatedTimelineElement ||
                details instanceof NewAddressRelatedTimelineElement;
    }
    
    public BaseRecipientDtoInt getRecipientDenominationByInternalId(String internalId) {
        List<BaseRecipientDto> baseRecipientDtoList = pnDataVaultClient.getRecipientDenominationByInternalId(List.of(internalId));

        List<BaseRecipientDtoInt> baseRecipientDtoIntList = baseRecipientDtoList
                .stream()
                .filter( el -> internalId.equals(el.getInternalId()))
                .map( el -> BaseRecipientDtoInt.builder()
                        .taxId(el.getTaxId())
                        .denomination(el.getDenomination())
                        .internalId(el.getInternalId())
                        .recipientType(el.getRecipientType() != null ? RecipientTypeInt.valueOf(el.getRecipientType().getValue()) : null)
                        .internalId(el.getInternalId())
                        .build()
                ).toList();
        
        if(baseRecipientDtoIntList != null){
            return baseRecipientDtoIntList.get(0);
        }
        
        return null;
    }
}
