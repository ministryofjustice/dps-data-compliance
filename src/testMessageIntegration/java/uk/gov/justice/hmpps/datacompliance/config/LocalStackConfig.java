package uk.gov.justice.hmpps.datacompliance.config;


import javassist.bytecode.stackmap.TypeData.ClassName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.ServerSocket;

public class LocalStackConfig {

    private final static Logger log = LoggerFactory.getLogger(ClassName.class);


    public static LocalStackContainer instance() {
        return startLocalstackIfNotRunning();
    }

    public static void setLocalStackProperties(LocalStackContainer localStackContainer, DynamicPropertyRegistry registry) {

        final var endpointConfiguration = localStackContainer.getEndpointConfiguration(Service.SNS);

        registry.add("hmpps.sqs.localstackUrl", endpointConfiguration::getServiceEndpoint);
        registry.add("hmpps.sqs.regio", endpointConfiguration::getSigningRegion);
    }


    private static LocalStackContainer startLocalstackIfNotRunning() {

        if (isLocalstackRunning()) return null;

        log.info("Creating a new local stack container");
        final var logConsumer = new Slf4jLogConsumer(log).withPrefix("localStack");

        final var localStackContainer = new LocalStackContainer(DockerImageName.parse("localstack/localstack").withTag("0.12.10"));
        localStackContainer
            .withServices(Service.SNS, Service.SQS)
            .withEnv("HOSTNAME_EXTERNAL", "localhost")
            .withEnv("DEFAULT_REGION", "eu-west-2")
            .waitingFor((
                Wait.forLogMessage(".*Ready.*", 1)
            ));

        localStackContainer.start();
        localStackContainer.followOutput(logConsumer);

        return localStackContainer;
    }

    private static Boolean isLocalstackRunning() {
        try {
            return new ServerSocket(4566).getLocalPort() == 0;
        } catch (IOException error) {
            return true;
        }
    }

}





