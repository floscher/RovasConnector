package app.rovas.josm.fixture;

import java.lang.reflect.Field;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.Stubbing;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import app.rovas.josm.util.URIs;

public class WiremockExtension implements BeforeAllCallback, AfterEachCallback, AfterAllCallback, ParameterResolver {

  private static final Field domainField;
  static {
    try {
      domainField = URIs.class.getDeclaredField("domain");
      domainField.setAccessible(true);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  private WireMockServer server;
  private String previousDomain;

  @Override
  public void beforeAll(ExtensionContext context) throws NoSuchFieldException, IllegalAccessException {
    this.server = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    server.start();

    this.previousDomain = (String) domainField.get(URIs.getInstance());
    final String newDomain = "http://localhost:" + server.port();
    domainField.set(URIs.getInstance(), newDomain);
    System.out.println("Mocking requests to " + previousDomain + " → " + newDomain);
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    server.resetAll();
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    domainField.set(URIs.getInstance(), previousDomain);
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
    return Stubbing.class.isAssignableFrom(parameterContext.getParameter().getType());
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
    return server;
  }
}
