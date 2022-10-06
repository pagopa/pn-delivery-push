package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.time.Instant;
import java.util.List;

@Builder( toBuilder = true )
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
@DynamoDbBean
public class TimelineElementDetailsEntity {

    @Getter(onMethod=@__({@DynamoDbAttribute("recIndex")}))  private Integer recIndex;
    @Getter(onMethod=@__({@DynamoDbAttribute("physicalAddress")}))  private PhysicalAddressEntity physicalAddress;
    @Getter(onMethod=@__({@DynamoDbAttribute("digitalAddress")}))  private DigitalAddressEntity digitalAddress;
    @Getter(onMethod=@__({@DynamoDbAttribute("digitalAddressSource")}))  private DigitalAddressSourceEntity digitalAddressSource;
    @Getter(onMethod=@__({@DynamoDbAttribute("isAvailable")})) private Boolean isAvailable;
    @Getter(onMethod=@__({@DynamoDbAttribute("attemptDate")})) private Instant attemptDate;
    @Getter(onMethod=@__({@DynamoDbAttribute("deliveryMode")})) private DeliveryModeEntity deliveryMode;
    @Getter(onMethod=@__({@DynamoDbAttribute("contactPhase")})) private ContactPhaseEntity contactPhase;
    @Getter(onMethod=@__({@DynamoDbAttribute("sentAttemptMade")})) private Integer sentAttemptMade;
    @Getter(onMethod=@__({@DynamoDbAttribute("sendDate")})) private Instant sendDate;
    @Getter(onMethod=@__({@DynamoDbAttribute("errors")})) private List<String> errors = null;
    @Getter(onMethod=@__({@DynamoDbAttribute("lastAttemptDate")})) private Instant lastAttemptDate;
    @Getter(onMethod=@__({@DynamoDbAttribute("retryNumber")})) private Integer retryNumber;
    @Getter(onMethod=@__({@DynamoDbAttribute("downstreamId")})) private DownstreamIdEntity downstreamId;
    @Getter(onMethod=@__({@DynamoDbAttribute("responseStatus")})) private ResponseStatusEntity responseStatus;
    @Getter(onMethod=@__({@DynamoDbAttribute("notificationDate")})) private Instant notificationDate;
    @Getter(onMethod=@__({@DynamoDbAttribute("serviceLevel")})) private ServiceLevelEntity serviceLevel;
    @Getter(onMethod=@__({@DynamoDbAttribute("investigation")})) private Boolean investigation;
    @Getter(onMethod=@__({@DynamoDbAttribute("newAddress")})) private PhysicalAddressEntity newAddress;
    @Getter(onMethod=@__({@DynamoDbAttribute("generatedAarUrl")})) private String generatedAarUrl;
    @Getter(onMethod=@__({@DynamoDbAttribute("numberOfPages")})) private Integer numberOfPages;
    @Getter(onMethod=@__({@DynamoDbAttribute("reasonCode")})) private String reasonCode;
    @Getter(onMethod=@__({@DynamoDbAttribute("reason")})) private String reason;
    @Getter(onMethod=@__({@DynamoDbAttribute("notificationCost")})) private Integer notificationCost;
    @Getter(onMethod=@__({@DynamoDbAttribute("sendingReceipts")})) private List<SendingReceiptEntity> sendingReceipts;
    @Getter(onMethod=@__({@DynamoDbAttribute("eventCode")})) private String eventCode;
    @Getter(onMethod=@__({@DynamoDbAttribute("shouldRetry")})) private Boolean shouldRetry;
    @Getter(onMethod=@__({@DynamoDbAttribute("raddType")})) private String raddType;
    @Getter(onMethod=@__({@DynamoDbAttribute("raddTransactionId")})) private String raddTransactionId;
}
