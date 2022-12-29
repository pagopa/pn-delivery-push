package it.pagopa.pn.deliverypush.springbootcfg;


import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.handlers.RequestHandler2;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import it.pagopa.pn.commons.configs.aws.AwsConfigs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class SQSConfig {

    private final AwsConfigs awsConfigs;

    public SQSConfig(AwsConfigs awsConfigs) {
        this.awsConfigs = awsConfigs;
    }

    /**
     * Si è reso necessario fornire un bean di AmazonSQSAsync perchè AmazonSQSBufferedAsyncClient
     * utilizzato di default dalla libreria spring-cloud-aws non supportava le code FIFO
     *
     * https://docs.awspring.io/spring-cloud-aws/docs/2.4.2/reference/html/index.html#fifo-queue-support
     * @return bean per le code
     */
    @Bean
    public AmazonSQSAsync amazonSQS() {
        if (StringUtils.hasText(awsConfigs.getEndpointUrl()))
            return AmazonSQSAsyncClientBuilder.standard()
                    .withRequestHandlers(new CustomAWSRequestHandler())
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(awsConfigs.getEndpointUrl(), awsConfigs.getRegionCode()))
                    .build();
        else
            return AmazonSQSAsyncClientBuilder.standard()
                    .withRegion(awsConfigs.getRegionCode())
                    .build();
    }

    /**
     * Questa Handler cattura tutte le richieste, risposte ed errori di oggetti di AWS come {@link AmazonSQSAsync}.
     * È possibile catturare gli eventi sovrascrivendo i metodi di {@link RequestHandler2}.
     * <p>
     * È possibile associare l'Handler durante la creazione dell'oggetto AWS, ad esempio:
     * <p>
     * AmazonSQSAsyncClientBuilder.standard()
     *             .withRequestHandlers(new CustomAWSRequestHandler())
     *             .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(awsConfigs.getEndpointUrl(), awsConfigs.getRegionCode()))
     *             .build();
     *
     */
    @Slf4j
    static class CustomAWSRequestHandler extends RequestHandler2 {

        @Override
        public void afterError(Request<?> request, Response<?> response, Exception e) {
            log.error("Exception for: " + request.getOriginalRequestObject(), e);
        }
    }
}