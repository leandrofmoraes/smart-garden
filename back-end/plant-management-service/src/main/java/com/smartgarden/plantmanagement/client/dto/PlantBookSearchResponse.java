package com.smartgarden.plantmanagement.client.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class PlantBookSearchResponse {
    private Integer count;
    private String next;
    private String previous;
    private List<PlantBookSearchResult> results;
}
