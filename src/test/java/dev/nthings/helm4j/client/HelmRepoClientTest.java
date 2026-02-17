package dev.nthings.helm4j.client;

import java.util.List;

import dev.nthings.helm4j.bindings.NativeHelmBindings;
import dev.nthings.helm4j.exceptions.HelmException;
import dev.nthings.helm4j.model.ShowMode;
import dev.nthings.helm4j.options.RepoAddOptions;
import dev.nthings.helm4j.options.RepoRemoveOptions;
import dev.nthings.helm4j.options.RepoUpdateOptions;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HelmRepoClientTest {

  @Test
  void repoMethodsDecodePayloads() {
    var bindings = new StubBindings();
    bindings.repoAddResponse =
        "{\"name\":\"bitnami\",\"url\":\"https://charts.bitnami.com/bitnami\"}";
    bindings.repoUpdateResponse =
        """
        {"repositories":[{"name":"bitnami","status":"ok"}]}
        """;
    bindings.repoListResponse =
        """
        {"repositories":[{"name":"bitnami","url":"https://charts.bitnami.com/bitnami"}]}
        """;
    bindings.repoRemoveResponse = "{\"removed\":[\"bitnami\"]}";

    var client = new HelmClient(JsonMapper.builder().build(), bindings);
    var repo = client.repo();

    var add =
        repo.add(
            RepoAddOptions.builder()
                .name("bitnami")
                .url("https://charts.bitnami.com/bitnami")
                .forceUpdate(true)
                .build());
    assertEquals("bitnami", add.name());
    assertEquals("https://charts.bitnami.com/bitnami", add.url());

    var update = repo.update(RepoUpdateOptions.builder().names("bitnami").build());
    assertEquals(1, update.size());
    assertEquals("bitnami", update.repositories().getFirst().name());
    assertEquals("ok", update.repositories().getFirst().status());

    var list = repo.list();
    assertEquals(1, list.size());
    assertEquals("bitnami", list.repositories().getFirst().name());

    var remove = repo.remove(RepoRemoveOptions.builder().names("bitnami").build());
    assertEquals(1, remove.size());
    assertEquals("bitnami", remove.removed().getFirst());
  }

  @Test
  void convenienceOverloadsSerializeExpectedPayloads() {
    var bindings = new StubBindings();
    var client = new HelmClient(JsonMapper.builder().build(), bindings);
    var repo = client.repo();

    repo.add("bitnami", "https://charts.bitnami.com/bitnami");
    assertTrue(bindings.lastRepoAddOptionsJson.contains("\"name\":\"bitnami\""));
    assertTrue(
        bindings.lastRepoAddOptionsJson.contains("\"url\":\"https://charts.bitnami.com/bitnami\""));

    repo.updateAll();
    assertTrue(bindings.lastRepoUpdateOptionsJson.contains("\"names\":[]"));

    repo.list();
    assertEquals("{}", bindings.lastRepoListOptionsJson);

    repo.remove(List.of("bitnami", "stable"));
    assertTrue(bindings.lastRepoRemoveOptionsJson.contains("\"names\":[\"bitnami\",\"stable\"]"));
  }

  @Test
  void repoErrorPayloadThrowsHelmException() {
    var bindings = new StubBindings();
    bindings.repoListResponse =
        """
        {"error":"boom","stage":"runOperation","operation":"repo list"}
        """;

    var client = new HelmClient(JsonMapper.builder().build(), bindings);

    var ex = assertThrows(HelmException.class, () -> client.repo().list());
    assertEquals("boom", ex.getMessage());
    assertEquals("runOperation", ex.stage());
    assertEquals("repo list", ex.operation());
  }

  @Test
  void invalidNativeJsonFailsFast() {
    var bindings = new StubBindings();
    bindings.repoUpdateResponse = "not-json";

    var client = new HelmClient(JsonMapper.builder().build(), bindings);

    assertThrows(IllegalStateException.class, () -> client.repo().updateAll());
  }

  @Test
  void nullInputsAreRejected() {
    var bindings = new StubBindings();
    var client = new HelmClient(JsonMapper.builder().build(), bindings);

    assertThrows(NullPointerException.class, () -> client.repo().add((RepoAddOptions) null));
    assertThrows(NullPointerException.class, () -> client.repo().update(null));
    assertThrows(NullPointerException.class, () -> client.repo().list(null));
    assertThrows(NullPointerException.class, () -> client.repo().remove((List<String>) null));
    assertThrows(NullPointerException.class, () -> client.repo().remove((RepoRemoveOptions) null));
  }

  @Test
  void immutableCollectionsReturnedToConsumers() {
    var bindings = new StubBindings();
    bindings.repoUpdateResponse =
        """
        {"repositories":[{"name":"bitnami","status":"ok"}]}
        """;
    bindings.repoListResponse =
        """
        {"repositories":[{"name":"bitnami","url":"https://charts.bitnami.com/bitnami"}]}
        """;
    bindings.repoRemoveResponse = "{\"removed\":[\"bitnami\"]}";

    var client = new HelmClient(JsonMapper.builder().build(), bindings);

    var update = client.repo().updateAll();
    assertFalse(update.repositories().isEmpty());
    assertThrows(UnsupportedOperationException.class, () -> update.repositories().clear());

    var list = client.repo().list();
    assertFalse(list.repositories().isEmpty());
    assertThrows(UnsupportedOperationException.class, () -> list.repositories().clear());

    var remove = client.repo().remove(List.of("bitnami"));
    assertFalse(remove.removed().isEmpty());
    assertThrows(UnsupportedOperationException.class, () -> remove.removed().clear());
  }

  private static final class StubBindings implements NativeHelmBindings {
    private String repoAddResponse = "{\"name\":\"demo\",\"url\":\"https://example.com/charts\"}";
    private String repoUpdateResponse = "{\"repositories\":[]}";
    private String repoListResponse = "{\"repositories\":[]}";
    private String repoRemoveResponse = "{\"removed\":[]}";

    private String lastRepoAddOptionsJson;
    private String lastRepoUpdateOptionsJson;
    private String lastRepoListOptionsJson;
    private String lastRepoRemoveOptionsJson;

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
      lastRepoAddOptionsJson = optionsJson;
      return repoAddResponse;
    }

    @Override
    public String repoUpdate(String optionsJson) {
      lastRepoUpdateOptionsJson = optionsJson;
      return repoUpdateResponse;
    }

    @Override
    public String repoList(String optionsJson) {
      lastRepoListOptionsJson = optionsJson;
      return repoListResponse;
    }

    @Override
    public String repoRemove(String optionsJson) {
      lastRepoRemoveOptionsJson = optionsJson;
      return repoRemoveResponse;
    }
  }
}
