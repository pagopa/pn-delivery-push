package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class TimelineElementDetailsEntity {
    private Integer recIndex;
    private String category;
    private PhysicalAddressEntity physicalAddress;
    private DigitalAddressEntity digitalAddress;
    private DigitalAddressSourceEntity digitalAddressSource;
    private Boolean isAvailable;
    private Instant attemptDate;
    private DeliveryModeEntity deliveryMode;
    private ContactPhaseEntity contactPhase;
    private Integer sentAttemptMade;
    private Instant sendDate;
    private List<String> errors = null;
    private Instant lastAttemptDate;
    private Integer retryNumber;
    private DownstreamIdEntity downstreamId;
    private ResponseStatusEntity responseStatus;
    private Instant notificationDate;
    private ServiceLevel serviceLevel;
    private Boolean investigation;
    private PhysicalAddressEntity newAddress;
}
