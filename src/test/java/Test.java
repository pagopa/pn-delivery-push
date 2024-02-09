import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.NormalizedAddressDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotHandledDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationCancelledDetailsInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetailsV23;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
import java.util.ArrayList;
import java.util.List;

public class Test {

    public static void main(String[] args){

        NormalizedAddressDetailsInt normalizedAddressDetailsInt = new NormalizedAddressDetailsInt();
        normalizedAddressDetailsInt.setRecIndex(1);
        var address = new PhysicalAddressInt();
        address.setFullname("ivan");
        normalizedAddressDetailsInt.setNewAddress(address);
        var x =SmartMapper.mapToClass(normalizedAddressDetailsInt, TimelineElementDetailsV23.class);


        NotificationCancelledDetailsInt source = new NotificationCancelledDetailsInt();
        List<Integer> list = new ArrayList<>();
        list.add(1);
        source.setNotRefinedRecipientIndexes(list);
        source.setNotificationCost(100);

        TimelineElementDetailsV23 ret = SmartMapper.mapToClass(source, TimelineElementDetailsV23.class);

        System.out.println("ret = "+ret);

        source.getNotRefinedRecipientIndexes().clear();
        ret = SmartMapper.mapToClass(source, TimelineElementDetailsV23.class);


        System.out.println("ret = "+ret);
        NotHandledDetailsInt altro = new NotHandledDetailsInt();
        altro.setReason("test");
        ret = SmartMapper.mapToClass(altro, TimelineElementDetailsV23.class);

        System.out.println("ret = "+ret);

    }



}
