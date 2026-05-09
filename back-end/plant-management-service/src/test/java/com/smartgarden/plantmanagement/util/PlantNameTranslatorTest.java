package com.smartgarden.plantmanagement.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PlantNameTranslatorTest {

  private PlantNameTranslator translator;

  @BeforeEach
  void setUp() {
    translator = new PlantNameTranslator();
  }

  @ParameterizedTest
  @CsvSource({
      "samambaia, nephrolepis exaltata",
      "orquídea, phalaenopsis amabilis",
      "orquidea, phalaenopsis amabilis",
      "babosa, aloe vera",
      "Babosa, aloe vera",
      "BABOSA, aloe vera",
      "monstera, monstera deliciosa",
      "costela-de-adão, monstera deliciosa",
      "jiboia, epipremnum aureum",
      "língua-de-sogra, sansevieria trifasciata"
  })
  void shouldTranslateKnownPortugueseNames(String input, String expected) {
    assertThat(translator.translate(input)).isEqualTo(expected);
  }

  @Test
  void shouldReturnSameNameWhenNotInMap() {
    assertThat(translator.translate("nephrolepis exaltata"))
        .isEqualTo("nephrolepis exaltata");
  }

  @Test
  void shouldReturnSameNameForEnglishInput() {
    assertThat(translator.translate("aloe vera")).isEqualTo("aloe vera");
  }

  @Test
  void shouldHandleNullGracefully() {
    assertThat(translator.translate(null)).isNull();
  }

  @Test
  void shouldHandleBlankGracefully() {
    assertThat(translator.translate("  ")).isBlank();
  }

  @Test
  void translateOptional_shouldReturnEmptyForNull() {
    assertThat(translator.translateOptional(null)).isEmpty();
  }

  @Test
  void translateOptional_shouldReturnTranslatedValue() {
    Optional<String> result = translator.translateOptional("samambaia");
    assertThat(result).isPresent().contains("nephrolepis exaltata");
  }
}
