package gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

/*
 * To enable ConfigurationProperties, we need to also add a class-level annotation to Application.java.
 * @EnableConfigurationProperties(UriConfiguration.class)
 */
@SpringBootApplication
@RestController
@EnableConfigurationProperties(UriConfiguration.class)
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
	/*
	 * Creating A Simple Route
	 * The Spring Cloud Gateway uses routes to process requests to downstream
	 * services. In this guide, we route all of our requests to HTTPBin. Routes can
	 * be configured a number of ways, but, for this guide, we use the Java API
	 * provided by the Gateway.
	 * 
	 * To get started, create a new Bean of type RouteLocator in Application.java.
	 * 
	 * The myRoutes method takes in a RouteLocatorBuilder that can be used to create
	 * routes. In addition to creating routes, RouteLocatorBuilder lets you add
	 * predicates and filters to your routes so that you can route handle based on
	 * certain conditions as well as alter the request/response as you see fit.
	 */

	@Bean
	public RouteLocator myRoutes(RouteLocatorBuilder builder, UriConfiguration uriConfiguration) {
		String httpUri = uriConfiguration.getHttpbin();
		return builder.routes()
				/*
				 * Now we can create a route that routes a request to https://httpbin.org/get
				 * when a request is made to the Gateway at /get. In our configuration of this
				 * route, we add a filter that adds the Hello request header with a value of
				 * World to the request before it is routed:
				 */
				/*
				 * To test our simple Gateway, we can run Application.java on port 8080. Once
				 * the application is running, make a request to http://localhost:8080/get. You
				 * can do so by using the following cURL command in your terminal:
				 * 
				 * $ curl http://localhost:8080/get
				 */
				.route(p -> p
						.path("/get")
						.filters(f -> f.addRequestHeader("Hello", "World"))
						.uri(httpUri))
				/*
				 * Using Spring Cloud CircuitBreaker
				 * Now we can do something a little more interesting. Since the services behind
				 * the Gateway could potentially behave poorly and affect our clients, we might
				 * want to wrap the routes we create in circuit breakers. You can do so in the
				 * Spring Cloud Gateway by using the Resilience4J Spring Cloud CircuitBreaker
				 * implementation. This is implemented through a simple filter that you can add
				 * to your requests. We can create another route to demonstrate this.
				 * 
				 * In the next example, we use HTTPBin’s delay API, which waits a certain number
				 * of seconds before sending a response. Since this API could potentially take a
				 * long time to send its response, we can wrap the route that uses this API in a
				 * circuit breaker. The following listing adds a new route to our RouteLocator
				 * object:
				 */
				/*
				 * There are some differences between this new route configuration and the
				 * previous one we created. For one, we use the host predicate instead of the
				 * path predicate. This means that, as long as the host is circuitbreaker.com,
				 * we route the request to HTTPBin and wrap that request in a circuit breaker.
				 * We do so by applying a filter to the route. We can configure the circuit
				 * breaker filter by using a configuration object. In this example, we give the
				 * circuit breaker a name of mycmd.
				 * 
				 * Now we can test this new route. To do so, we need to start the application,
				 * but, this time, we are going to make a request to /delay/3. It is also
				 * important that we include a Host header that has a host of
				 * circuitbreaker.com. Otherwise, the request is not routed. We can use the
				 * following cURL command:
				 * 
				 * $ curl --dump-header - --header 'Host: www.circuitbreaker.com'
				 * http://localhost:8080/delay/3
				 */
				/*
				 * As you can see the circuit breaker timed out while waiting for the response
				 * from HTTPBin. When the circuit breaker times out, we can optionally provide a
				 * fallback so that clients do not receive a 504 but something more meaningful.
				 * In a production scenario, you might return some data from a cache, for
				 * example, but, in our simple example, we return a response with the body
				 * fallback instead.
				 * 
				 * To do so, we can modify our circuit breaker filter to provide a URL to call
				 * in the case of a timeout:
				 */
				.route(p -> p
						.host("*.circuitbreaker.com")
						.filters(f -> f.circuitBreaker(config -> config.setName("mycmd")
								.setFallbackUri("forward:/fallback")))
						.uri(httpUri))
				.build();
	}

	/*
	 * Now, when the circuit breaker wrapped route times out, it calls /fallback in
	 * the Gateway application. Now we can add the /fallback endpoint to our
	 * application.
	 * 
	 * In Application.java, we add the @RestController class level annotation and
	 * then add the following @RequestMapping to the class:
	 */
	@RequestMapping("/fallback")
	public Mono<String> fallback() {
		return Mono.just("fallback");
	}
}
