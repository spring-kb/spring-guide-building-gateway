package gateway;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

/*
 * Our test takes advantage of WireMock from Spring Cloud Contract stand up a server that can mock the APIs from HTTPBin. 
 * The first thing to notice is the use of @AutoConfigureWireMock(port = 0). This annotation starts WireMock on a random port for us.
 * Next, note that we take advantage of our UriConfiguration class and set the httpbin property in the @SpringBootTest annotation to the WireMock server running locally. 
 * Within the test, we then setup “stubs” for the HTTPBin APIs we call through the Gateway and mock the behavior we expect. 
 * Finally, we use WebTestClient to make requests to the Gateway and validate the responses.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "httpbin=http://localhost:${wiremock.server.port}" })
@AutoConfigureWireMock(port = 0)
public class ApplicationTest {

    @Autowired
    private WebTestClient webClient;

    @Test
    public void contextLoads() throws Exception {
        // Stubs
        stubFor(get(urlEqualTo("/get"))
                .willReturn(aResponse()
                        .withBody("{\"headers\":{\"Hello\":\"World\"}}")
                        .withHeader("Content-Type", "application/json")));
        stubFor(get(urlEqualTo("/delay/3"))
                .willReturn(aResponse()
                        .withBody("no fallback")
                        .withFixedDelay(3000)));

        webClient
                .get().uri("/get")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.headers.Hello").isEqualTo("World");

        webClient
                .get().uri("/delay/3")
                .header("Host", "www.circuitbreaker.com")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(
                        response -> assertThat(response.getResponseBody())
                                .isEqualTo("fallback".getBytes()));
    }
}