package it.pagopa.pn.deliverypush.springbootcfg;


import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import it.pagopa.pn.commons.configs.aws.AwsConfigs;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
        return AmazonSQSAsyncClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(awsConfigs.getEndpointUrl(), awsConfigs.getRegionCode()))
                .build();
    }
}