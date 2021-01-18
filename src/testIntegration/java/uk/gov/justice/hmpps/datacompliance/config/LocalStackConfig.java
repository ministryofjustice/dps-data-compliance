package uk.gov.justice.hmpps.datacompliance.config;

import javassist.bytecode.stackmap.TypeData.ClassName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

@Configuration
@ConditionalOnProperty(name = "data.compliance.request.sqs.provider", havingValue = "embedded-localstack")
public class LocalStackConfig {

    private Logger log = LoggerFactory.getLogger(ClassName.class);

    @Bean
    public LocalStackContainer localStackContainer(){
        log.info("Starting localstack...");

        var logConsumer = new Slf4jLogConsumer(log).withPrefix("localstack");
        LocalStackContainer localStackContainer = new LocalStackContainer(DockerImageName.parse("localstack/localstack").withTag("0.11.2"))
            .withServices(Service.SQS, Service.SNS)
            .withClasspathResourceMapping("/localstack/setup-queues.sh", "/docker-entrypoint-initaws.d/setup-queues.sh", BindMode.READ_WRITE)
            .withEnv("HOSTNAME_EXTERNAL", "localhost")
            .withEnv("DEFAULT_REGION", "eu-west-2")
            .waitingFor(
                Wait.forLogMessage(".*All Ready.*", 1)
            );

        log.info("Started localstack");

        localStackContainer.start();
        localStackContainer.followOutput(logConsumer);

        return localStackContainer;
    }

}


