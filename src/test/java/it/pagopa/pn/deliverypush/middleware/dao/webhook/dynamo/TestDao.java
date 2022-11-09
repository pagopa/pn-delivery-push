package it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo;

import org.springframework.boot.test.context.SpringBootTest;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;

import java.util.concurrent.ExecutionException;


@SpringBootTest
class TestDao<T>  {

        DynamoDbAsyncTable<T> dbTable;

        protected Key getKeyBuild(String pk) {
            return getKeyBuild(pk, null);
        }

        protected Key getKeyBuild(String pk, String sk) {
            if (sk == null)
                return Key.builder().partitionValue(pk).build();
            else
                return Key.builder().partitionValue(pk).sortValue(sk).build();
        }

        protected Key getKeyBuild(String pk, int sk) {
            return Key.builder().partitionValue(pk).sortValue(sk).build();
        }

        public TestDao(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient, String table, Class<T> typeParameter)
        {
            this.dbTable = dynamoDbEnhancedAsyncClient.table(table, TableSchema.fromBean(typeParameter));
        }

        public T get(String pk, String sk) throws ExecutionException, InterruptedException {
            GetItemEnhancedRequest req = GetItemEnhancedRequest.builder()
                    .key(getKeyBuild(pk, sk))
                    .build();

            return (T)dbTable.getItem(req).get();
        }

        public void delete(String pk, String sk) throws ExecutionException, InterruptedException {

            DeleteItemEnhancedRequest req = DeleteItemEnhancedRequest.builder()
                    .key(getKeyBuild(pk, sk))
                    .build();

            dbTable.deleteItem(req).get();
        }


}