package it.pagopa.pn.deliverypush.pnclient.delivery;

import it.pagopa.pn.api.dto.status.RequestUpdateStatusDto;
import it.pagopa.pn.api.dto.status.ResponseUpdateStatusDto;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class PnDeliveryClientImpl implements PnDeliveryClient {
    private final RestTemplate restTemplate;
    private final PnDeliveryPushConfigs cfg;
    
    private static final String UPDATE_STATUS_URL ="/notifications/update-status";

    public PnDeliveryClientImpl(RestTemplate restTemplate, PnDeliveryPushConfigs cfg) {
        this.restTemplate = restTemplate;
        this.cfg = cfg;
    }

    public ResponseEntity<ResponseUpdateStatusDto> updateState(RequestUpdateStatusDto dto) {
        log.debug("Start update status call for iun {}", dto.getIun());

        final String baseUrl = cfg.getDeliveryBaseUrl() + UPDATE_STATUS_URL;
        HttpEntity<RequestUpdateStatusDto> entity = new HttpEntity<>(dto, null);

        ResponseEntity<ResponseUpdateStatusDto> resp = restTemplate.exchange(baseUrl, HttpMethod.POST, entity, ResponseUpdateStatusDto.class);

        log.debug("Response update state for iun {} is {}", dto.getIun(), resp);

        return resp;
    }
}
