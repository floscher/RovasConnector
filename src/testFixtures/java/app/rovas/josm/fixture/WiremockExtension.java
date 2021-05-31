package app.rovas.josm.fixture;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.Stubbing;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import app.rovas.josm.util.UrlProvider;

public class WiremockExtension implements BeforeAllCallback, AfterEachCallback, ParameterResolver {
  private WireMockServer server;

  @Override
  public void beforeAll(ExtensionContext context) {
    this.server = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    server.start();
  }

  @Override
  public void afterEach(ExtensionContext context) {
    server.resetAll();
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
    return Stubbing.class.isAssignableFrom(parameterContext.getParameter().getType())
      || UrlProvider.class.isAssignableFrom(parameterContext.getParameter().getType());
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
    return Stubbing.class.isAssignableFrom(parameterContext.getParameter().getType()) ? server : new WiremockUrlProvider();
  }

  public class WiremockUrlProvider extends UrlProvider {
    @Override
    protected final String getBaseUrl() {
      return "http://localhost:" + server.port();
    }
  }
}
