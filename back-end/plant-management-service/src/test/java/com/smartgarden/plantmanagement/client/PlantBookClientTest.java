package com.smartgarden.plantmanagement.client;

import com.smartgarden.plantmanagement.client.dto.PlantBookDetailResponse;
import com.smartgarden.plantmanagement.client.dto.PlantBookSearchResponse;
import com.smartgarden.plantmanagement.client.dto.PlantBookSearchResult;
import com.smartgarden.plantmanagement.exception.PlantBookIntegrationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlantBookClientTest {

  @Mock
  RestClient.Builder builder;
  @Mock
  RestClient restClient;
  @Mock
  RestClient.RequestHeadersUriSpec requestSpec;
  @Mock
  RestClient.RequestHeadersSpec headersSpec;
  @Mock
  RestClient.ResponseSpec responseSpec;

  private PlantBookClient client;

  @BeforeEach
  void setUp() {
    when(builder.baseUrl(any(String.class))).thenReturn(builder);
    when(builder.defaultHeader(any(), any())).thenReturn(builder);
    when(builder.build()).thenReturn(restClient);
    client = new PlantBookClient(builder, "https://open.plantbook.io/api/v1", "test-token", 10);
  }

  @Test
  void search_shouldReturnResultsWhenApiResponds() {
    PlantBookSearchResult result = new PlantBookSearchResult();
    result.setPid("nephrolepis-exaltata");
    result.setDisplayPid("Nephrolepis exaltata");

    PlantBookSearchResponse response = new PlantBookSearchResponse();
    response.setCount(1);
    response.setResults(List.of(result));

    when(restClient.get()).thenReturn(requestSpec);
    when(requestSpec.uri(any(String.class), any(), any())).thenReturn(headersSpec);
    when(headersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.body(PlantBookSearchResponse.class)).thenReturn(response);

    Optional<PlantBookSearchResponse> found = client.search("nephrolepis exaltata");
    assertThat(found).isPresent();
    assertThat(found.get().getResults()).hasSize(1);
  }

  @Test
  void search_shouldReturnEmptyWhenNoResults() {
    PlantBookSearchResponse response = new PlantBookSearchResponse();
    response.setCount(0);
    response.setResults(List.of());

    when(restClient.get()).thenReturn(requestSpec);
    when(requestSpec.uri(any(String.class), any(), any())).thenReturn(headersSpec);
    when(headersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.body(PlantBookSearchResponse.class)).thenReturn(response);

    assertThat(client.search("unknown-plant")).isEmpty();
  }

  @Test
  void search_shouldThrowIntegrationExceptionOnRestFailure() {
    when(restClient.get()).thenReturn(requestSpec);
    when(requestSpec.uri(any(String.class), any(), any())).thenReturn(headersSpec);
    when(headersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.body(PlantBookSearchResponse.class))
        .thenThrow(new RestClientException("connection refused"));

    assertThatThrownBy(() -> client.search("aloe vera"))
        .isInstanceOf(PlantBookIntegrationException.class)
        .hasMessageContaining("aloe vera");
  }
}
