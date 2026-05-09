package com.smartgarden.plantmanagement.client;

import com.smartgarden.plantmanagement.client.dto.PlantBookDetailResponse;
import com.smartgarden.plantmanagement.client.dto.PlantBookSearchResponse;
import com.smartgarden.plantmanagement.client.dto.PlantBookSearchResult;
import com.smartgarden.plantmanagement.exception.PlantBookIntegrationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

/**
 * Cliente HTTP para a Open PlantBook API.
 *
 * <p>
 * Fluxo de uso:
 * <ol>
 * <li>Chamar {@link #search(String)} com o alias científico traduzido</li>
 * <li>Pegar o {@code pid} do primeiro resultado</li>
 * <li>Chamar {@link #getDetail(String)} para obter os parâmetros de
 * cuidado</li>
 * </ol>
 */
@Slf4j
@Component
public class PlantBookClient {

  private final RestClient restClient;
  private final int searchLimit;

  public PlantBookClient(
      RestClient.Builder builder,
      @Value("${plantbook.api.base-url}") String baseUrl,
      @Value("${plantbook.api.token}") String token,
      @Value("${plantbook.api.search-limit:10}") int searchLimit) {

    this.searchLimit = searchLimit;
    this.restClient = builder
        .baseUrl(baseUrl)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Token " + token)
        .build();
  }

  /**
   * Busca plantas pelo alias científico/inglês.
   *
   * @param alias nome científico ou inglês da planta
   * @return resposta com lista de resultados
   */
  public Optional<PlantBookSearchResponse> search(String alias) {
    log.info("PlantBook search [alias={}]", alias);
    try {
      PlantBookSearchResponse response = restClient.get()
          .uri("/plant/search?alias={alias}&limit={limit}", alias, searchLimit)
          .retrieve()
          .body(PlantBookSearchResponse.class);

      if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
        log.warn("PlantBook returned no results for alias='{}'", alias);
        return Optional.empty();
      }
      log.info("PlantBook search found {} result(s) for alias='{}'",
          response.getResults().size(), alias);
      return Optional.of(response);

    } catch (RestClientException e) {
      log.error("PlantBook search failed for alias='{}': {}", alias, e.getMessage(), e);
      throw new PlantBookIntegrationException(
          "Failed to search PlantBook for alias: " + alias, e);
    }
  }

  /**
   * Obtém detalhes completos de uma planta pelo pid.
   *
   * @param pid identificador retornado pela busca
   * @return detalhes da planta com parâmetros de cuidado
   */
  public Optional<PlantBookDetailResponse> getDetail(String pid) {
    log.info("PlantBook detail [pid={}]", pid);
    try {
      PlantBookDetailResponse response = restClient.get()
          .uri("/plant/detail/{pid}/", pid)
          .retrieve()
          .body(PlantBookDetailResponse.class);

      if (response == null) {
        log.warn("PlantBook detail returned null for pid='{}'", pid);
        return Optional.empty();
      }
      return Optional.of(response);

    } catch (RestClientException e) {
      log.error("PlantBook detail failed for pid='{}': {}", pid, e.getMessage(), e);
      throw new PlantBookIntegrationException(
          "Failed to get PlantBook detail for pid: " + pid, e);
    }
  }

  /**
   * Busca e já obtém o detalhe do primeiro resultado para um alias.
   * Conveniência para o fluxo de cadastro de planta.
   */
  public Optional<PlantBookDetailResponse> searchAndGetDetail(String alias) {
    return search(alias)
        .flatMap(resp -> resp.getResults().stream()
            .map(PlantBookSearchResult::getPid)
            .findFirst())
        .flatMap(this::getDetail);
  }
}
