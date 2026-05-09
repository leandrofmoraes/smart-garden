package com.smartgarden.plantmanagement.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PlantBookSearchResult {
    private String pid;

    @JsonProperty("display_pid")
    private String displayPid;

    private String alias;
    private String category;

    @JsonProperty("image_url")
    private String imageUrl;
}
