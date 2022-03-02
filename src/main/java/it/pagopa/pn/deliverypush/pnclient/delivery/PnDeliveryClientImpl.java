package it.pagopa.pn.deliverypush.pnclient.delivery;

import it.pagopa.pn.api.dto.status.RequestUpdateStatusDto;
import it.pagopa.pn.api.dto.status.ResponseUpdateStatusDto;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class PnDeliveryClientImpl implements PnDeliveryClient {

    private static final String UPDATE_STATUS_URL ="/notifications/update-status";

    private final RestTemplate restTemplate;
    private final PnDeliveryPushConfigs cfg;
    private final String baseUrl;


    public PnDeliveryClientImpl( @Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryPushConfigs cfg) {
        this.restTemplate = restTemplate;
        this.cfg = cfg;
        this.baseUrl = cfg.getDeliveryBaseUrl() + UPDATE_STATUS_URL;
    }

    public ResponseEntity<ResponseUpdateStatusDto> updateState(RequestUpdateStatusDto dto) {

        log.info("Start update status call for iun {}, url: {}", dto.getIun(), baseUrl);

        HttpEntity<RequestUpdateStatusDto> entity = new HttpEntity<>(dto, null);

        ResponseEntity<ResponseUpdateStatusDto> resp = restTemplate.exchange(baseUrl, HttpMethod.POST, entity, ResponseUpdateStatusDto.class);

        log.debug("Response update state for iun {} is {}", dto.getIun(), resp);

        return resp;
    }
}
