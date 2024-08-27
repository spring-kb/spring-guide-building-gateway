package gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

/*
 * As a good developer, we should write some tests to make sure our Gateway is doing what we expect it should. In most cases, we want to limit 
 * our dependencies on outside resources, especially in unit tests, so we should not depend on HTTPBin. 
 * One solution to this problem is to make the URI in our routes configurable, so we can change the URI if we need to.
 * To do so, in Application.java, we can create a new class called UriConfiguration:
 */
@ConfigurationProperties
class UriConfiguration {

    private String httpbin = "http://httpbin.org:80";

    public String getHttpbin() {
        return httpbin;
    }

    public void setHttpbin(String httpbin) {
        this.httpbin = httpbin;
    }
}