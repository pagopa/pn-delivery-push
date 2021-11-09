package it.pagopa.pn.deliverypush.webhook.configuration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.pagopa.pn.api.dto.webhook.WebhookConfigDto;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.webhook.WebhookConfigService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class InitConfWebhookFromFile {
    private static final String WEBHOOK_CONFIG_FILE_PATH = "/webhookconfig/webhookinit.json";
    private WebhookConfigService webhookInitConfigService;

    public InitConfWebhookFromFile(WebhookConfigService webhookInitConfigService) {
        this.webhookInitConfigService = webhookInitConfigService;
    }

    @PostConstruct
    public void initWebhookConfiguration() {
        log.info("Start initWebhookConfiguration");
        List<WebhookConfigDto> listDto = getWebhookInitConfDto();
        webhookInitConfigService.putConfigurations(listDto);
    }

    private List<WebhookConfigDto> getWebhookInitConfDto() {
        Resource resource = new ClassPathResource(WEBHOOK_CONFIG_FILE_PATH);
        ObjectMapper mapper = setObjectMapper();

        try {
            return mapper.readValue(resource.getFile(), new TypeReference<>() {
            });
        } catch (IOException e) {
            log.error("Cannot read webhook config file", e);
            throw new PnInternalException("Cannot read webhook config file", e);
        }
    }

    @NotNull
    private ObjectMapper setObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

}
