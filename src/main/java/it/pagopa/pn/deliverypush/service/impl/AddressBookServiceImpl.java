package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.userattributes.UserAttributesClient;
import it.pagopa.pn.deliverypush.service.AddressBookService;
import it.pagopa.pn.deliverypush.service.mapper.CourtesyCourtesyDigitalAddressMapper;
import it.pagopa.pn.deliverypush.service.mapper.LegalLegalDigitalAddressMapper;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.userattributes.model.CourtesyDigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.userattributes.model.LegalDigitalAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AddressBookServiceImpl implements AddressBookService {
    private final UserAttributesClient userAttributesClient;

    public AddressBookServiceImpl(UserAttributesClient userAttributesClient) {
        this.userAttributesClient = userAttributesClient;
    }

    @Override
    public Optional<LegalDigitalAddressInt> getPlatformAddresses(String recipientId, String senderId) {
      List<LegalDigitalAddress> legalDigitalAddresses = userAttributesClient.getLegalAddressBySender(recipientId, senderId);

      log.info("GetLegalAddress OK - senderId={}", senderId);
     
      
      if(legalDigitalAddresses != null && !legalDigitalAddresses.isEmpty()){
          
          if (legalDigitalAddresses.size() > 1){
              log.warn("Digital addresses list contains more than one element - senderId={}", senderId);
          }

          List<LegalDigitalAddressInt> digitalAddresses = legalDigitalAddresses.stream().map(
                  LegalLegalDigitalAddressMapper::externalToInternal
          ).toList();
                  
          for(LegalDigitalAddressInt address : digitalAddresses){
              log.debug("For senderId={} address type={} is available", senderId, address.getType());

              if(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC.equals(address.getType())){
                  return Optional.of(address);
              }
          }
      }

      log.debug("list legal address is empty - senderId={}", senderId);
      return Optional.empty();
        
    }

    @Override
    public Optional<List<CourtesyDigitalAddressInt>> getCourtesyAddress(String recipientId, String senderId) {
        List<CourtesyDigitalAddress> courtesyDigitalAddresses = userAttributesClient.getCourtesyAddressBySender(recipientId, senderId);
        
        try {
            if(courtesyDigitalAddresses != null && !courtesyDigitalAddresses.isEmpty()){
                log.info("getCourtesyAddress OK - senderId={}, recipientId={} courtesyListSize={}", senderId, recipientId, courtesyDigitalAddresses.size());
                return Optional.of(
                        courtesyDigitalAddresses.stream().map(
                                CourtesyCourtesyDigitalAddressMapper::externalToInternal
                        ).toList()
                );
            }
            log.info("getCourtesyAddress OK - senderId={}, recipientId={} courtesyListSize=Empty", senderId, recipientId);
        
            return Optional.empty();
        }catch (Exception ex){
            //Se la get dei messaggi di cortesia fallisce per un qualsiasi motivo il processo non si blocca. Viene fatto catch exception e loggata
            log.error("GetCourtesyAddress Failed ex={}- senderId={}, recipientId={}", ex, senderId, recipientId);
            return Optional.empty();
        }
    }

    @Override
    public Optional<PhysicalAddressInt> getResidentialAddress(String taxId, String senderId) {
        log.error("Call to Unsupported method, getResidentialAddress for sendId={} ", senderId);
        throw new UnsupportedOperationException("getResidentialAddress is not supported yet");
    }
}
