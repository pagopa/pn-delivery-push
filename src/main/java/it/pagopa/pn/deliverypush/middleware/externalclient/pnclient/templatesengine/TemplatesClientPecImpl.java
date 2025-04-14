package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.templatesengine;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.config.msclient.TemplateApiConfiguration;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.ApiClient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.model.LanguageEnum;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.model.NotificationAarForPec;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@CustomLog
public class TemplatesClientPecImpl implements TemplatesClientPec{
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private final TemplateApiConfiguration templateApiConfiguration;

    @Override
    public String parametrizedNotificationAarForPec(LanguageEnum language, NotificationAarForPec notificationAarForPec) {
        log.info("parametrizedNotificationAarForPec - {} / {}", language, notificationAarForPec );
        Object postBody = notificationAarForPec;
        ApiClient apiClient = templateApiConfiguration.templateApiConfig(new RestTemplate(), pnDeliveryPushConfigs).getApiClient();

        // verify the required parameter 'xLanguage' is set
        if (language == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'xLanguage' when calling notificationAarForPec");
        }

        // verify the required parameter 'notificationAarForPec' is set
        if (notificationAarForPec == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'notificationAarForPec' when calling notificationAarForPec");
        }


        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        headerParams.add("x-language", apiClient.parameterToString(language));

        final String[] localVarAccepts = {
                "text/html", "application/json", "application/problem+json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] contentTypes = {
                "application/json"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(contentTypes);

        String[] authNames = new String[] {  };

        ParameterizedTypeReference<String> returnType = new ParameterizedTypeReference<String>() {};
        return  apiClient.invokeAPI(pnDeliveryPushConfigs.getTemplateURLforPEC(), HttpMethod.PUT, Collections.<String, Object>emptyMap(), queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, authNames, returnType).getBody();
    }
}
