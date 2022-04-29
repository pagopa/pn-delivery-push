package it.pagopa.pn.deliverypush.pnclient.delivery;

import it.pagopa.pn.api.dto.notification.Notification;
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

import java.util.Optional;

@Slf4j
@Component
public class PnDeliveryClientImpl implements PnDeliveryClient {

    private static final String UPDATE_STATUS_URL ="/delivery-private/notifications/update-status";
    private static final String GET_NOTIFICATION_URL = "/delivery-private/notifications";

    private final RestTemplate restTemplate;
    private final PnDeliveryPushConfigs cfg;
    private final String baseUrl;


    public PnDeliveryClientImpl( @Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryPushConfigs cfg) {
        this.restTemplate = restTemplate;
        this.cfg = cfg;
        this.baseUrl = cfg.getDeliveryBaseUrl();
    }

    public ResponseEntity<ResponseUpdateStatusDto> updateState(RequestUpdateStatusDto dto) {
        String url = baseUrl + UPDATE_STATUS_URL;

        log.debug("Start update status call for iun {}, url: {}", dto.getIun(), url);

        HttpEntity<RequestUpdateStatusDto> entity = new HttpEntity<>(dto, null);

        ResponseEntity<ResponseUpdateStatusDto> resp = restTemplate.exchange(url, HttpMethod.POST, entity, ResponseUpdateStatusDto.class);

        log.debug("Response update state for iun {} is {}", dto.getIun(), resp);

        return resp;
    }

    @Override
    public Optional<Notification> getNotificationInfo(String iun, boolean withTimeline) {

        String url = baseUrl + GET_NOTIFICATION_URL + "/" + iun;
        if (!withTimeline) {
            url += "?with_timeline=false";
        }
        log.debug( "Start get notification info for iun {}, url {} ", iun, url );
        return Optional.ofNullable( restTemplate.getForObject( url, Notification.class ) );
    }
}
