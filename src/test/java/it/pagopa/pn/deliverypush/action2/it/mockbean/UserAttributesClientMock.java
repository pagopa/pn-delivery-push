package it.pagopa.pn.deliverypush.action2.it.mockbean;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.externalclient.pnclient.userattributes.UserAttributesClient;
import it.pagopa.pn.deliverypush.service.mapper.CourtesyDigitalAddressMapper;
import it.pagopa.pn.deliverypush.service.mapper.LegalLegalDigitalAddressMapper;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.model.CourtesyDigitalAddress;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.model.LegalDigitalAddress;
import org.springframework.http.ResponseEntity;

import java.util.*;
import java.util.stream.Collectors;

public class UserAttributesClientMock implements UserAttributesClient {
    private Map<String, Collection<LegalDigitalAddress>> mapLegalDigitalAddresses;
    private Map<String, Collection<CourtesyDigitalAddress>> mapCourtesyDigitalAddresses;

    public void clear() {
        this.mapLegalDigitalAddresses = new HashMap<>();
        this.mapCourtesyDigitalAddresses = new HashMap<>();
    }
    
    public void addLegalDigitalAddresses(String taxId, String senderId, List<LegalDigitalAddressInt> listDigitalAddresses) {
        List<LegalDigitalAddress> legalDigitalAddressList = listDigitalAddresses.stream().map(
                LegalLegalDigitalAddressMapper::internalToExternal
        ).collect(Collectors.toList());
        
        String id = getId(taxId, senderId);
        
        this.mapLegalDigitalAddresses.put(id, legalDigitalAddressList);
    }

    public void addCourtesyDigitalAddresses(String taxId, String senderId, List<CourtesyDigitalAddressInt> courtesyDigitalAddresses) {
        List<CourtesyDigitalAddress> legalDigitalAddressList = courtesyDigitalAddresses.stream().map(
                CourtesyDigitalAddressMapper::internalToExternal
        ).collect(Collectors.toList());

        String id = getId(taxId, senderId);

        this.mapCourtesyDigitalAddresses.put(id, legalDigitalAddressList);
    }
    
    @Override
    public ResponseEntity<List<LegalDigitalAddress>> getLegalAddressBySender(String taxId, String senderId) {
        String id = getId(taxId, senderId);

        List<LegalDigitalAddress> listLegalDigitalAddress = new ArrayList<>();

        Collection<LegalDigitalAddress> collectionLegalDigitalAddresses = mapLegalDigitalAddresses.get(id);
        
        if(collectionLegalDigitalAddresses != null && !collectionLegalDigitalAddresses.isEmpty()){
            listLegalDigitalAddress = new ArrayList<>(collectionLegalDigitalAddresses);
        }

        return ResponseEntity.ok(listLegalDigitalAddress);
    }

    @Override
    public ResponseEntity<List<CourtesyDigitalAddress>> getCourtesyAddressBySender(String taxId, String senderId) {
        String id = getId(taxId, senderId);
        List<CourtesyDigitalAddress> listCourtesyDigitalAddress = new ArrayList<>();
        
        Collection<CourtesyDigitalAddress> collectionCourtesyDigitalAddresses = mapCourtesyDigitalAddresses.get(id);
        
        if(collectionCourtesyDigitalAddresses != null && !collectionCourtesyDigitalAddresses.isEmpty()){
            listCourtesyDigitalAddress = new ArrayList<>(collectionCourtesyDigitalAddresses);
        }
        
        return ResponseEntity.ok(listCourtesyDigitalAddress);
    }

    //TODO Aggiungere logica senderId dopo aver verificato che continua a funzionare così
    private String getId(String taxId, String senderId) {
        return taxId + "_" + senderId;
    }
}
