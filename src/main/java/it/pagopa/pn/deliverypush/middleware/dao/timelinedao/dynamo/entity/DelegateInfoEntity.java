package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@DynamoDbBean
public class DelegateInfoEntity {
    @Getter(onMethod=@__({@DynamoDbAttribute("internalId")}))  private String internalId;
    @Getter(onMethod=@__({@DynamoDbAttribute("operatorUuid")}))  private String operatorUuid;
    @Getter(onMethod=@__({@DynamoDbAttribute("mandateId")}))  private String mandateId;
    @Getter(onMethod=@__({@DynamoDbAttribute("delegateType")}))  private DelegateTypeEntity delegateType;
    
    public enum DelegateTypeEntity {
        PF("PF"),

        PG("PG");

        private final String value;

        DelegateTypeEntity(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }
}
