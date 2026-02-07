package dev.nthings.helm4j.client;

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
  void factoryUsesProvidedMapper() throws Exception {
    var mapper = JsonMapper.builder().build();
    var client = HelmClientFactory.create().withObjectMapper(mapper).newClient();

    assertSame(mapper, extractMapper(client));
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
}
