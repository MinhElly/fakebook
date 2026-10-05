package com.minh.fakebook.gateway.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import com.minh.fakebook.gateway.IntegrationTest;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.brave.bridge.BraveTracer;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MimeTypeUtils;
import zipkin2.reporter.BytesMessageSender;
import zipkin2.reporter.brave.AsyncZipkinSpanHandler;

@IntegrationTest
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_TIMEOUT)
@WithMockUser
@ImportAutoConfiguration(TestChannelBinderConfiguration.class)
@ActiveProfiles({ "kafka" })
class GatewayKafkaResourceIT {

    private static String KAFKA_API = "/api/gateway-kafka/{command}";

    @Autowired
    private WebTestClient client;

    @Autowired
    private InputDestination input;

    @Autowired
    private OutputDestination output;

    @Autowired
    private Environment environment;

    @Autowired
    private Tracer tracer;

    @Autowired
    private BytesMessageSender zipkinSender;

    @Autowired
    private AsyncZipkinSpanHandler zipkinSpanHandler;

    @BeforeEach
    void setupCsrf() {
        client = client.mutateWith(csrf());
    }

    @Test
    void producesMessages() throws InterruptedException {
        client
            .post()
            .uri(uriBuilder -> uriBuilder.path(KAFKA_API).queryParam("message", "value-produce").build("publish"))
            .exchange()
            .expectStatus()
            .isNoContent();
        assertThat(output.receive(1000, "binding-out-0").getPayload()).isEqualTo("value-produce".getBytes());
    }

    @Test
    void doesNotAutoBindGeneratedSupplier() {
        assertThat(environment.getProperty("spring.cloud.function.definition")).doesNotContain("kafkaProducer");
        assertThat(environment.getProperty("spring.cloud.stream.bindings.kafkaProducer-out-0.content-type"))
            .isNull();
    }

    @Test
    void configuresZipkinTracing() {
        assertThat(tracer).isInstanceOf(BraveTracer.class);
        assertThat(zipkinSender).isNotNull();
        assertThat(zipkinSpanHandler).isNotNull();
    }

    @Test
    void consumesMessages() {
        Map<String, Object> map = new HashMap<>();
        map.put(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN_VALUE);
        MessageHeaders headers = new MessageHeaders(map);
        Message<String> testMessage = new GenericMessage<>("value-consume", headers);
        input.send(testMessage);
        String value = client
            .get()
            .uri(KAFKA_API, "consume")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
            .returnResult(String.class)
            .getResponseBody()
            .blockFirst(Duration.ofSeconds(10));
        assertThat(value).isEqualTo("value-consume");
    }
}
