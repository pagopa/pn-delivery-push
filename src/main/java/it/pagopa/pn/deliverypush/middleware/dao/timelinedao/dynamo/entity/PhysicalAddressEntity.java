package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@DynamoDbBean
public class PhysicalAddressEntity {
    @Getter(onMethod=@__({@DynamoDbAttribute("at")}))  private String at;
    @Getter(onMethod=@__({@DynamoDbAttribute("address")}))  private String address;
    @Getter(onMethod=@__({@DynamoDbAttribute("addressDetails")}))  private String addressDetails;
    @Getter(onMethod=@__({@DynamoDbAttribute("zip")}))  private String zip;
    @Getter(onMethod=@__({@DynamoDbAttribute("municipality")}))  private String municipality;
    @Getter(onMethod=@__({@DynamoDbAttribute("municipalityDetails")}))  private String municipalityDetails;
    @Getter(onMethod=@__({@DynamoDbAttribute("province")}))  private String province;
    @Getter(onMethod=@__({@DynamoDbAttribute("foreignState")}))  private String foreignState;
}
