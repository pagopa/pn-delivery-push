package it.pagopa.pn.deliverypush.action2.it.mockbean;

import it.pagopa.pn.deliverypush.externalclient.pnclient.userattributes.UserAttributes;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.service.mapper.CourtesyDigitalAddressMapper;
import it.pagopa.pn.deliverypush.service.mapper.LegalDigitalAddressMapper;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.model.CourtesyDigitalAddress;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.model.LegalDigitalAddress;
import org.springframework.http.ResponseEntity;

import java.util.*;
import java.util.stream.Collectors;

public class UserAttributesMock implements UserAttributes {
    private Map<String, Collection<LegalDigitalAddress>> mapLegalDigitalAddresses;
    private Map<String, Collection<CourtesyDigitalAddress>> mapCourtesyDigitalAddresses;

    public void clear() {
        this.mapLegalDigitalAddresses = new HashMap<>();
        this.mapCourtesyDigitalAddresses = new HashMap<>();
    }
    
    public void addLegalDigitalAddresses(String taxId, String senderId, List<DigitalAddress> listDigitalAddresses) {
        List<LegalDigitalAddress> legalDigitalAddressList = listDigitalAddresses.stream().map(
                LegalDigitalAddressMapper::internalToExternal
        ).collect(Collectors.toList());
        
        String id = getId(taxId, senderId);
        
        this.mapLegalDigitalAddresses.put(id, legalDigitalAddressList);
    }

    public void addCourtesyDigitalAddresses(String taxId, String senderId, List<DigitalAddress> courtesyDigitalAddresses) {
        List<CourtesyDigitalAddress> legalDigitalAddressList = courtesyDigitalAddresses.stream().map(
                CourtesyDigitalAddressMapper::internalToExternal
        ).collect(Collectors.toList());

        String id = getId(taxId, senderId);

        this.mapCourtesyDigitalAddresses.put(id, legalDigitalAddressList);
    }
    
    @Override
    public ResponseEntity<List<LegalDigitalAddress>> getLegalAddressBySender(String taxId, String senderId) {
        String id = getId(taxId, senderId);

        Collection<LegalDigitalAddress> collectionLegalDigitalAddresses = mapLegalDigitalAddresses.get(id);
        
        List<LegalDigitalAddress> legalDigitalAddress =  collectionLegalDigitalAddresses.stream()
                .filter(
                        digitalAddresses -> digitalAddresses.getRecipientId().equals(taxId)
                ).collect(Collectors.toList());
        
        return ResponseEntity.ok(legalDigitalAddress);
    }

    @Override
    public ResponseEntity<List<CourtesyDigitalAddress>> getCourtesyAddressBySender(String taxId, String senderId) {
        String id = getId(taxId, senderId);

        Collection<CourtesyDigitalAddress> collectionCourtesyDigitalAddresses = mapCourtesyDigitalAddresses.get(id);

        List<CourtesyDigitalAddress> courtesy =  collectionCourtesyDigitalAddresses.stream()
                .filter(
                        digitalAddresses -> digitalAddresses.getRecipientId().equals(taxId)
                ).collect(Collectors.toList());

        return ResponseEntity.ok(courtesy);
    }

    //TODO Aggiungere logica senderId dopo aver verificato che continua a funzionare così
    private String getId(String taxId, String senderId) {
        return taxId;
    }
}
