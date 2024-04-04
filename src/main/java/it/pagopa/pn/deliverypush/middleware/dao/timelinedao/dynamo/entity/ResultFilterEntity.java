package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity;

import com.fasterxml.jackson.annotation.JsonValue;
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
@ToString
public class ResultFilterEntity {
    @Getter(onMethod=@__({@DynamoDbAttribute("fileKey")})) private String fileKey;
    @Getter(onMethod=@__({@DynamoDbAttribute("result")})) private ResultFilterEnumEntity result;
    @Getter(onMethod=@__({@DynamoDbAttribute("reasonCode")})) private String reasonCode;
    @Getter(onMethod=@__({@DynamoDbAttribute("reasonDescription")})) private String reasonDescription;

    public enum ResultFilterEnumEntity {
        SUCCESS("SUCCESS"),

        DISCARD("DISCARD"),

        NEXT("NEXT");

        private final String value;

        ResultFilterEnumEntity(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

}