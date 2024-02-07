package it.pagopa.pn.deliverypush;

import it.pagopa.pn.commons.configs.listeners.TaskIdApplicationListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class PnDeliveryPushApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(PnDeliveryPushApplication.class);
		app.addListeners(new TaskIdApplicationListener());
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
