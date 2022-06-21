package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.userattributes.UserAttributesClient;
import it.pagopa.pn.deliverypush.service.AddressBookService;
import it.pagopa.pn.deliverypush.service.mapper.CourtesyCourtesyDigitalAddressMapper;
import it.pagopa.pn.deliverypush.service.mapper.LegalLegalDigitalAddressMapper;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.model.CourtesyDigitalAddress;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.model.LegalDigitalAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
    public Optional<LegalDigitalAddressInt> getPlatformAddresses(String taxId, String senderId) {
        ResponseEntity<List<LegalDigitalAddress>> resp = userAttributesClient.getLegalAddressBySender(taxId, senderId);

        if (resp.getStatusCode().is2xxSuccessful()) {
            log.debug("GetLegalAddress OK - senderId {}", senderId);
            List<LegalDigitalAddress> legalDigitalAddresses = resp.getBody();
            
            if(legalDigitalAddresses != null && !legalDigitalAddresses.isEmpty()){
                
                if (legalDigitalAddresses.size() > 1){
                    log.warn("Digital addresses list contains more than one element ");
                }

                List<LegalDigitalAddressInt> digitalAddresses = legalDigitalAddresses.stream().map(
                        LegalLegalDigitalAddressMapper::externalToInternal
                ).collect(Collectors.toList());
                        
                for(LegalDigitalAddressInt address : digitalAddresses){
                    log.debug("For senderId {} address type {} is available", senderId, address.getType());

                    if(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC.equals(address.getType())){
                        return Optional.of(address);
                    }
                }
            }

            log.debug("list legal address is empty - senderId {}", senderId);

            return Optional.empty();
        } else {
            log.error("GetLegalAddress Failed  senderId {}", senderId);
            throw new PnInternalException("GetLegalAddress Failed taxId "+ taxId +" senderId "+ senderId);
        }
    }

    @Override
    public Optional<List<CourtesyDigitalAddressInt>> getCourtesyAddress(String taxId, String senderId) {
        ResponseEntity<List<CourtesyDigitalAddress>> resp = userAttributesClient.getCourtesyAddressBySender(taxId, senderId);

        if (resp.getStatusCode().is2xxSuccessful()) {
            log.debug("getCourtesyAddress OK - senderId {}", senderId);
            List<CourtesyDigitalAddress> courtesyDigitalAddresses = resp.getBody();

            if(courtesyDigitalAddresses != null && !courtesyDigitalAddresses.isEmpty()){
                return Optional.of(
                        courtesyDigitalAddresses.stream().map(
                                CourtesyCourtesyDigitalAddressMapper::externalToInternal
                        ).collect(Collectors.toList())
                );
            }

            return Optional.empty();
        } else {
            log.error("getCourtesyAddress Failed senderId {}", senderId);
            throw new PnInternalException("getCourtesyAddress Failed taxId "+ taxId +" senderId "+ senderId);
        }
    }

    @Override
    public Optional<PhysicalAddressInt> getResidentialAddress(String taxId, String senderId) {
        log.error("Call to Unsupported method, getResidentialAddress for sendId {} ", senderId);
        throw new UnsupportedOperationException("getResidentialAddress is not supported yet");
    }
}
