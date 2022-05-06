package it.pagopa.pn.deliverypush.middleware.actiondao.dynamo;

import it.pagopa.pn.api.dto.events.PnExtChnProgressStatus;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;
import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@DynamoDbBean
public class FutureActionEntity {
    //TODO Da rivedere i campi necessari e quelli eliminabili una volta passati alla v2
    
    public static final String FIELD_TIME_SLOT = "timeSlot";
    public static final String FIELD_ACTION_ID = "actionId";

    private String timeSlot;
    private String actionId;
    private String iun;
    private Instant notBefore;
    private ActionType type;
    private Integer recipientIndex;
    private String taxId;
    private DigitalAddressSource digitalAddressSource;
    private Integer retryNumber;
    private PnExtChnProgressStatus responseStatus;
    private PhysicalAddressConv newPhysicalAddress;
    private List<String> attachmentKeys;

    @DynamoDbPartitionKey
    @DynamoDbAttribute(value = FIELD_TIME_SLOT )
    public String getTimeSlot() {
        return timeSlot;
    }
    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }

    @DynamoDbSortKey
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

    public DigitalAddressSource getDigitalAddressSource() {
        return digitalAddressSource;
    }

    public void setDigitalAddressSource(DigitalAddressSource digitalAddressSource) {
        this.digitalAddressSource = digitalAddressSource;
    }

    public Integer getRetryNumber() {
        return retryNumber;
    }

    public void setRetryNumber(Integer retryNumber) {
        this.retryNumber = retryNumber;
    }

    public PnExtChnProgressStatus getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(PnExtChnProgressStatus responseStatus) {
        this.responseStatus = responseStatus;
    }

    public PhysicalAddressConv getNewPhysicalAddress() {
        return newPhysicalAddress;
    }

    public void setNewPhysicalAddress(PhysicalAddressConv newPhysicalAddress) {
        this.newPhysicalAddress = newPhysicalAddress;
    }

    public List<String> getAttachmentKeys() {
        return attachmentKeys;
    }

    public void setAttachmentKeys(List<String> attachmentKeys) {
        this.attachmentKeys = attachmentKeys;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }
}
