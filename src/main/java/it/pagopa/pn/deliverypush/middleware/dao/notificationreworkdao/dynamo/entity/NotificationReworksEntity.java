package it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Data
@DynamoDbBean
public class NotificationReworksEntity {
    public static final String FIELD_IUN = "iun";
    public static final String FIELD_REWORK_ID = "reworkId";
    public static final String FIELD_INVALIDATED_TIMELINE_ELEMENT_IDS = "invalidatedTimelineElementIds";
    public static final String FIELD_REASON = "reason";
    public static final String FIELD_IDX = "idx";
    public static final String FIELD_EXPECTED_STATUS_CODES = "expectedStatusCodes";
    public static final String FIELD_RECEIVED_STATUS_CODES = "receivedStatusCodes";
    public static final String FIELD_EXPECTED_DELIVERY_FAILURE_CAUSE = "expectedDeliveryFailureCause";
    public static final String FIELD_EXPECTED_FINAL_STATUS = "expectedFinalStatus";
    public static final String FILED_STATUS = "status";
    public static final String FIELD_ERRORS = "errors";
    public static final String FIELD_CREATED_AT = "createdAt";
    public static final String FIELD_UPDATED_AT = "updatedAt";
    public static final String FIELD_ATTEMPTID = "attemptId";
    public static final String FIELD_PCRETRY = "pcRetry";
    public static final String FIELD_RECINDEX = "recIndex";
    public static final String FIELD_REQUEST_TYPE = "requestType";

    @Getter(onMethod = @__({@DynamoDbPartitionKey, @DynamoDbAttribute(FIELD_IUN)}))
    private String iun;

    @Getter(onMethod = @__({@DynamoDbSortKey, @DynamoDbAttribute(FIELD_REWORK_ID)}))
    private String reworkId;

    @Getter(onMethod = @__({@DynamoDbAttribute(FIELD_INVALIDATED_TIMELINE_ELEMENT_IDS)}))
    private List<String> invalidatedTimelineElementIds;

    @Getter(onMethod = @__({@DynamoDbAttribute(FIELD_REASON)}))
    private String reason;

    @Getter(onMethod = @__({@DynamoDbAttribute(FIELD_IDX)}))
    private Integer idx;

    @Getter(onMethod = @__({@DynamoDbAttribute(FIELD_EXPECTED_STATUS_CODES)}))
    private List<StatusCodeEntity> expectedStatusCodes;

    @Getter(onMethod = @__({@DynamoDbAttribute(FIELD_RECEIVED_STATUS_CODES)}))
    private List<StatusCodeEntity> receivedStatusCodes;

    @Getter(onMethod = @__({@DynamoDbAttribute(FIELD_EXPECTED_DELIVERY_FAILURE_CAUSE)}))
    private String expectedDeliveryFailureCause;

    @Getter(onMethod = @__({@DynamoDbAttribute(FIELD_EXPECTED_FINAL_STATUS)}))
    private String expectedFinalStatus;

    @Getter(onMethod = @__({@DynamoDbAttribute(FILED_STATUS)}))
    private ReworkRequestStatus status;

    @Getter(onMethod = @__({@DynamoDbAttribute(FIELD_ERRORS)}))
    private List<NotificationReworksErrorEntity> errors;

    @Getter(onMethod = @__({@DynamoDbAttribute(FIELD_CREATED_AT)}))
    private Instant createdAt;

    @Getter(onMethod = @__({@DynamoDbAttribute(FIELD_UPDATED_AT)}))
    private Instant updatedAt;

    @Getter(onMethod = @__({@DynamoDbAttribute(FIELD_ATTEMPTID)}))
    private String attemptId;

    @Getter(onMethod = @__({@DynamoDbAttribute(FIELD_PCRETRY)}))
    private String pcRetry;

    @Getter(onMethod = @__({@DynamoDbAttribute(FIELD_RECINDEX)}))
    private String recIndex;

    @Getter(onMethod = @__({@DynamoDbAttribute(FIELD_REQUEST_TYPE)}))
    private RequestTypeEnum requestType;

    public static class ReworkIdBuilder {
        private static final Pattern TRY_PATTERN = Pattern.compile("TRY_(\\d+)");
        private static final Pattern REWORK_PATTERN = Pattern.compile("REWORK_(\\d+)");

        public static Integer extractTryIdx(String reworkId) {
            Matcher matcher = TRY_PATTERN.matcher(reworkId);
            return matcher.find() ? Optional.ofNullable(matcher.group(1)).map(Integer::parseInt).orElse(0) : 0;
        }
        public static Integer extractReworkIdx(String reworkId) {
            Matcher matcher = REWORK_PATTERN.matcher(reworkId);
            return matcher.find() ? Optional.ofNullable(matcher.group(1)).map(Integer::parseInt).orElse(0) : 0;
        }

        public static String build(Integer reworkIdx, Integer tryIdx, String recIndex) {
            return String.format("REWORK_%d.TRY_%d.%s", reworkIdx, tryIdx, recIndex);
        }
    }

}
