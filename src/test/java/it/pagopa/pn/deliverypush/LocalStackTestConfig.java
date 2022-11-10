package it.pagopa.pn.deliverypush;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB;

/**
 * Classe che permette di creare un container Docker di LocalStack.
 * Il container (e quindi la classe) può essere condivisa tra più classi di test.
 * Per utilizzare questa classe, le classi di test dovranno essere annotate con
 * @Import(LocalStackTestConfig.class)
 */
@TestConfiguration
public class LocalStackTestConfig {

    static LocalStackContainer localStack =
            new LocalStackContainer(DockerImageName.parse(getImageName()).asCompatibleSubstituteFor("localstack/localstack"))
                    .withServices(DYNAMODB)
                    .withClasspathResourceMapping("testcontainers/init.sh",
                            "/docker-entrypoint-initaws.d/make-storages.sh", BindMode.READ_ONLY)
                    .withClasspathResourceMapping("testcontainers/credentials",
                            "/root/.aws/credentials", BindMode.READ_ONLY)
                    .withNetworkAliases("localstack")
                    .withNetwork(Network.builder().createNetworkCmdModifier(cmd -> cmd.withName("delivery-push-net")).build())
                    .waitingFor(Wait.forLogMessage(".*Initialization terminated.*", 1));

    static {
        localStack.start();
        System.setProperty("aws.endpoint-url", localStack.getEndpointOverride(DYNAMODB).toString());
        try {
            System.setProperty("aws.sharedCredentialsFile", new ClassPathResource("testcontainers/credentials").getFile().getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static String getImageName() {
        String image = System.getProperty("localstack_image_name", "public.ecr.aws/localstack/localstack:1.0.4");
        System.out.println("Docker image: " + image);
        return image;
    }
}
