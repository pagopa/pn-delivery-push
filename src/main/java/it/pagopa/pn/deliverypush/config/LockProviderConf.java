package it.pagopa.pn.deliverypush.config;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.dynamodb2.DynamoDBLockProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
@Slf4j
public class LockProviderConf {

    @Bean
    public LockProvider lockProvider(DynamoDbClient dynamoDB, PnDeliveryPushConfigs cfg) {
        String lockTableName = cfg.getTimelineShedlockDao().getTableName();
        log.info("Shared Lock tableName={}", lockTableName);
        return new DynamoDBLockProvider(dynamoDB, lockTableName);
    }
}
