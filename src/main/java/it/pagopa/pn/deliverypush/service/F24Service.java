package it.pagopa.pn.deliverypush.service;

import java.util.List;
import java.util.Map;

public interface F24Service {

    void preparePDF(String iun);
    void handleF24PrepareResponse(String iun, Map<Integer, List<String>> generatedUrls); //Rivedere struttura dati

}
