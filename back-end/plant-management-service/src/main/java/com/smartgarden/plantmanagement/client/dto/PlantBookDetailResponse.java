package com.smartgarden.plantmanagement.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class PlantBookDetailResponse {

  private String pid;

  @JsonProperty("display_pid")
  private String displayPid;

  private String alias;
  private String category;

  @JsonProperty("min_light_mmol")
  private Integer minLightMmol;
  @JsonProperty("max_light_mmol")
  private Integer maxLightMmol;
  @JsonProperty("min_light_lux")
  private Integer minLightLux;
  @JsonProperty("max_light_lux")
  private Integer maxLightLux;

  @JsonProperty("min_temp")
  private Double minTemp;
  @JsonProperty("max_temp")
  private Double maxTemp;

  @JsonProperty("min_env_humid")
  private Double minEnvHumid;
  @JsonProperty("max_env_humid")
  private Double maxEnvHumid;

  @JsonProperty("min_soil_moist")
  private Double minSoilMoist;
  @JsonProperty("max_soil_moist")
  private Double maxSoilMoist;

  @JsonProperty("min_soil_ec")
  private Double minSoilEc;
  @JsonProperty("max_soil_ec")
  private Double maxSoilEc;

  @JsonProperty("image_url")
  private String imageUrl;

  @JsonProperty("common_names")
  @JsonIgnore
  private List<Object> commonNames;
}
