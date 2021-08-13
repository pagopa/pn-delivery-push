package it.pagopa.pn.deliverypush;

import it.pagopa.pn.commons.configs.PnSpringBootApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@PnSpringBootApplication
@EnableScheduling
public class PnDeliveryPushApplication {

	public static void main(String[] args) {
		SpringApplication.run(PnDeliveryPushApplication.class, args);
	}

	@RestController
	public static class HomeController {

		@GetMapping("")
		public String home() {
			return "Sono Vivo";
		}
	}
}
