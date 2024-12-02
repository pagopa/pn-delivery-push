package it.pagopa.pn.deliverypush.config;

import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.api.TemplateApi;
import it.pagopa.pn.deliverypush.legalfacts.*;
import it.pagopa.pn.deliverypush.legalfacts.generatorfactory.LegalFactGeneratorFactory;
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
    private final TemplateApi templateEngineClient;

    private final DocumentComposition documentComposition;
    private final InstantNowSupplier instantNowSupplier;

    @Bean
    @ConditionalOnProperty(name = "features.templates.client")
    public LegalFactGeneratorFactory legalFactGeneratorTemplatesClient() {
        return new LegalFactGeneratorTemplatesClient(
                instantWriter,
                physicalAddressWriter,
                pnDeliveryPushConfigs,
                pnSendModeUtils,
                templateEngineClient
        );
    }

    @Bean
    @ConditionalOnProperty(name = "features.templates.docComposition", havingValue = "default", matchIfMissing = true)
    public LegalFactGeneratorFactory legalFactGeneratorDocComposition() {
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
