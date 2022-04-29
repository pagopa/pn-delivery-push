package it.pagopa.pn.deliverypush;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.dynamodb2.DynamoDBLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
@Configuration
public class PnDeliveryPushSchedulingConfiguration {

    @Bean
    public LockProvider lockProvider(software.amazon.awssdk.services.dynamodb.DynamoDbClient dynamoDB) {
        return new DynamoDBLockProvider(dynamoDB, "PnDeliveryPushShedLock");
    }
}
