package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.userattributes.UserAttributesClient;
import it.pagopa.pn.deliverypush.service.mapper.CourtesyCourtesyDigitalAddressMapper;
import it.pagopa.pn.deliverypush.service.mapper.LegalLegalDigitalAddressMapper;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.model.CourtesyDigitalAddress;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.model.LegalDigitalAddress;

import java.util.*;
import java.util.stream.Collectors;

public class UserAttributesClientMock implements UserAttributesClient {
    private int getLegalAddressCalledTimes = 0;
    
    private Map<String, Collection<LegalDigitalAddress>> mapLegalDigitalAddresses; //Ciò che viene restituito al primo tentativo
    private Map<String, Collection<LegalDigitalAddress>> mapSecondCycleLegalDigitalAddresses; //Ciò che viene restituito al secondo tentativo
    private Map<String, Collection<CourtesyDigitalAddress>> mapCourtesyDigitalAddresses;

    public void clear() {
        this.mapLegalDigitalAddresses = new HashMap<>();
        this.mapSecondCycleLegalDigitalAddresses = new HashMap<>();
        this.mapCourtesyDigitalAddresses = new HashMap<>();
        this.getLegalAddressCalledTimes = 0;
    }

    private void addAddressToMap(String taxId, 
                                 String senderId, 
                                 List<LegalDigitalAddressInt> listDigitalAddresses, 
                                 Map<String, Collection<LegalDigitalAddress>> mapToAdd) {
        List<LegalDigitalAddress> legalDigitalAddressList = listDigitalAddresses.stream().map(
                LegalLegalDigitalAddressMapper::internalToExternal
        ).collect(Collectors.toList());

        String id = getId(taxId, senderId);

        mapToAdd.put(id, legalDigitalAddressList);
    }

    public void addLegalDigitalAddresses(String taxId, String senderId, List<LegalDigitalAddressInt> listDigitalAddresses) {
        addAddressToMap(taxId, senderId, listDigitalAddresses, this.mapLegalDigitalAddresses);
        addAddressToMap(taxId, senderId, listDigitalAddresses, this.mapSecondCycleLegalDigitalAddresses);
        //La mappa dei secondi tentativi viene valorizzata uguale alla mappa dei primi tentativi, in tal modo di default restituirà sempre lo stesso indirizzo
    }

    public void addSecondCycleLegalDigitalAddress(String taxId, String senderId, List<LegalDigitalAddressInt> listSecondCycleDigitalAddress) {
        addAddressToMap(taxId, senderId, listSecondCycleDigitalAddress, this.mapSecondCycleLegalDigitalAddresses);
        //si può effettuare l'override dei secondi tentativi grazie a questo metodo
    }

    public void addCourtesyDigitalAddresses(String taxId, String senderId, List<CourtesyDigitalAddressInt> courtesyDigitalAddresses) {
        List<CourtesyDigitalAddress> legalDigitalAddressList = courtesyDigitalAddresses.stream().map(
                CourtesyCourtesyDigitalAddressMapper::internalToExternal
        ).collect(Collectors.toList());

        String id = getId(taxId, senderId);

        this.mapCourtesyDigitalAddresses.put(id, legalDigitalAddressList);
    }
    
    @Override
    public List<LegalDigitalAddress> getLegalAddressBySender(String taxId, String senderId) {
        String id = getId(taxId, senderId);
        
        Collection<LegalDigitalAddress> collectionLegalDigitalAddresses;
        if(getLegalAddressCalledTimes == 0){
            collectionLegalDigitalAddresses = mapLegalDigitalAddresses.get(id);
        }else {
            collectionLegalDigitalAddresses = mapSecondCycleLegalDigitalAddresses.get(id);
        }

        List<LegalDigitalAddress> listLegalDigitalAddress = new ArrayList<>();
        if(collectionLegalDigitalAddresses != null && !collectionLegalDigitalAddresses.isEmpty()){
            listLegalDigitalAddress = new ArrayList<>(collectionLegalDigitalAddresses);
        }
        
        getLegalAddressCalledTimes += 1;
        return listLegalDigitalAddress;
    }

    @Override
    public List<CourtesyDigitalAddress> getCourtesyAddressBySender(String taxId, String senderId) {
        String id = getId(taxId, senderId);
        List<CourtesyDigitalAddress> listCourtesyDigitalAddress = new ArrayList<>();
        
        Collection<CourtesyDigitalAddress> collectionCourtesyDigitalAddresses = mapCourtesyDigitalAddresses.get(id);
        
        if(collectionCourtesyDigitalAddresses != null && !collectionCourtesyDigitalAddresses.isEmpty()){
            listCourtesyDigitalAddress = new ArrayList<>(collectionCourtesyDigitalAddresses);
        }
        
        return listCourtesyDigitalAddress;
    }

    private String getId(String taxId, String senderId) {
        return taxId + "_" + senderId;
    }
}
