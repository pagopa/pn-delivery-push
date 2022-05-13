package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo;

import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@DynamoDbBean
public class ActionEntity {
    //TODO Da rivedere i campi necessari e quelli eliminabili una volta passati alla v2
    
    public static final String FIELD_ACTION_ID = "actionId";

    private String actionId;
    private String iun;
    private Instant notBefore;
    private ActionType type;
    private Integer recipientIndex;
    /*
    private String taxId;
    private DigitalAddressSource digitalAddressSource;
    private Integer retryNumber;
    private PnExtChnProgressStatus responseStatus;
    private PhysicalAddressConv newPhysicalAddress;
    private List<String> attachmentKeys;
    */
    
    @DynamoDbPartitionKey
    @DynamoDbAttribute(value = FIELD_ACTION_ID )
    public String getActionId() {
        return actionId;
    }
    public void setActionId(String actionId) {
        this.actionId = actionId;
    }

    public String getIun() {
        return iun;
    }

    public void setIun(String iun) {
        this.iun = iun;
    }

    public Instant getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(Instant notBefore) {
        this.notBefore = notBefore;
    }

    public ActionType getType() {
        return type;
    }

    public void setType(ActionType type) {
        this.type = type;
    }

    public Integer getRecipientIndex() {
        return recipientIndex;
    }

    public void setRecipientIndex(Integer recipientIndex) {
        this.recipientIndex = recipientIndex;
    }
    
}
