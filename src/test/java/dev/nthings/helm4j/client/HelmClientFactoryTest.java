package dev.nthings.helm4j.client;

import dev.nthings.helm4j.bindings.NativeHelmBindings;
import dev.nthings.helm4j.model.ShowMode;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HelmClientFactoryTest {

  @Test
  void factoryRejectsNullMapper() {
    assertThrows(
        NullPointerException.class, () -> HelmClientFactory.create().withObjectMapper(null));
  }

  @Test
  void factoryRejectsNullBindings() {
    assertThrows(
        NullPointerException.class, () -> HelmClientFactory.create().withNativeBindings(null));
  }

  @Test
  void factoryUsesProvidedMapper() throws Exception {
    var mapper = JsonMapper.builder().build();
    var client = HelmClientFactory.create().withObjectMapper(mapper).newClient();

    assertSame(mapper, extractMapper(client));
  }

  @Test
  void factoryUsesProvidedBindings() throws Exception {
    var bindings = new NoOpBindings();
    var client = HelmClientFactory.create().withNativeBindings(bindings).newClient();

    assertSame(bindings, extractBindings(client));
  }

  @Test
  void factoryProvidesDefaultMapper() throws Exception {
    var client = HelmClientFactory.create().newClient();
    var mapper = extractMapper(client);

    assertNotNull(mapper);
    var response =
        mapper.readValue(
            "{\"charts\":[],\"extra\":\"ignored\"}",
            dev.nthings.helm4j.model.SearchResultSet.class);
    assertNotNull(response);
    assertTrue(response.isEmpty());
  }

  private static ObjectMapper extractMapper(HelmClient client) throws Exception {
    var mapperField = HelmClient.class.getDeclaredField("mapper");
    mapperField.setAccessible(true);
    return (ObjectMapper) mapperField.get(client);
  }

  private static NativeHelmBindings extractBindings(HelmClient client) throws Exception {
    var bindingsField = HelmClient.class.getDeclaredField("bindings");
    bindingsField.setAccessible(true);
    return (NativeHelmBindings) bindingsField.get(client);
  }

  private static final class NoOpBindings implements NativeHelmBindings {
    @Override
    public String show(ShowMode mode, String chartReference, String optionsJson) {
      return "{}";
    }

    @Override
    public String search(String optionsJson) {
      return "{\"results\":[]}";
    }

    @Override
    public String repoAdd(String optionsJson) {
      return "{\"name\":\"demo\",\"url\":\"https://example.com/charts\"}";
    }

    @Override
    public String repoUpdate(String optionsJson) {
      return "{\"repositories\":[]}";
    }

    @Override
    public String repoList(String optionsJson) {
      return "{\"repositories\":[]}";
    }

    @Override
    public String repoRemove(String optionsJson) {
      return "{\"removed\":[]}";
    }
  }
}
