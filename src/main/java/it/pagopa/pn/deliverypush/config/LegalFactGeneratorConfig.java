package it.pagopa.pn.deliverypush.config;

import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.legalfacts.*;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.templatesengine.TemplatesClientImpl;
import it.pagopa.pn.deliverypush.utils.PnSendModeUtils;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
public class LegalFactGeneratorConfig {

    private final CustomInstantWriter instantWriter;
    private final PhysicalAddressWriter physicalAddressWriter;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private final PnSendModeUtils pnSendModeUtils;
    private final TemplatesClientImpl templatesClient;

    private final DocumentComposition documentComposition;
    private final InstantNowSupplier instantNowSupplier;

    @Bean
    @ConditionalOnProperty(name = "pn.delivery-push.enableTemplatesEngine", havingValue = "true", matchIfMissing = true)
    public LegalFactGenerator legalFactGeneratorTemplatesClient() {
        return new LegalFactGeneratorTemplates(
                instantWriter,
                physicalAddressWriter,
                pnDeliveryPushConfigs,
                pnSendModeUtils,
                templatesClient
        );
    }

    @Bean
    @ConditionalOnProperty(name = "pn.delivery-push.enableTemplatesEngine", havingValue = "false")
    public LegalFactGenerator legalFactGeneratorDocComposition() {
        return new LegalFactGeneratorDocComposition(
                documentComposition,
                instantWriter,
                physicalAddressWriter,
                pnDeliveryPushConfigs,
                instantNowSupplier,
                pnSendModeUtils
        );
    }

}
