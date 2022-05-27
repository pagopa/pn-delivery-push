package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.externalclient.pnclient.userattributes.UserAttributesClient;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress;
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
            log.debug("GetLegalAddress OK - taxId {} senderId {}", taxId, senderId);
            List<LegalDigitalAddress> legalDigitalAddresses = resp.getBody();
            
            if(legalDigitalAddresses != null && !legalDigitalAddresses.isEmpty()){
                
                if (legalDigitalAddresses.size() > 1){
                    log.warn("Digital addresses list contains more than one element ");
                }

                List<LegalDigitalAddressInt> digitalAddresses = legalDigitalAddresses.stream().map(
                        LegalLegalDigitalAddressMapper::externalToInternal
                ).collect(Collectors.toList());
                        
                for(LegalDigitalAddressInt address : digitalAddresses){
                    log.debug("For taxId {} and senderId {} address type {} is available", taxId, senderId, address.getType());

                    if(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC.equals(address.getType())){
                        return Optional.of(address);
                    }
                }
            }

            log.debug("list legal address is empty - taxId {} senderId {}", taxId, senderId);

            return Optional.empty();
        } else {
            log.error("GetLegalAddress Failed taxId {} senderId {}", taxId, senderId);
            throw new PnInternalException("GetLegalAddress Failed taxId "+ taxId +" senderId "+ senderId);
        }
    }

    @Override
    public Optional<List<CourtesyDigitalAddressInt>> getCourtesyAddress(String taxId, String senderId) {
        ResponseEntity<List<CourtesyDigitalAddress>> resp = userAttributesClient.getCourtesyAddressBySender(taxId, senderId);

        if (resp.getStatusCode().is2xxSuccessful()) {
            log.debug("getCourtesyAddress OK - taxId {} senderId {}", taxId, senderId);
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
            log.error("getCourtesyAddress Failed taxId {} senderId {}", taxId, senderId);
            throw new PnInternalException("getCourtesyAddress Failed taxId "+ taxId +" senderId "+ senderId);
        }
    }

    @Override
    public Optional<PhysicalAddress> getResidentialAddress(String taxId, String senderId) {
        log.error("Call to Unsupported method, getResidentialAddress for taxId {} sendId {} ", taxId, senderId);
        throw new UnsupportedOperationException("getResidentialAddress is not supported yet");
    }
}
