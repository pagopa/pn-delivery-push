package it.pagopa.pn.deliverypush;

import it.pagopa.pn.commons.PnAutoConfigurationImportSelector;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.result.view.RedirectView;

@Import(PnAutoConfigurationImportSelector.class)
@SpringBootApplication(scanBasePackages = {"it.pagopa.pn.commons", "it.pagopa.pn.deliverypush"})
@EnableScheduling
public class PnDeliveryPushApplication {

	public static void main(String[] args) {
		SpringApplication.run(PnDeliveryPushApplication.class, args);
	}


}
