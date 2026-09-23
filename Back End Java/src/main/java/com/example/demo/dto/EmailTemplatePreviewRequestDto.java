package com.example.demo.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailTemplatePreviewRequestDto {

    private String objet;
    private String contenu;
    private Boolean active;
    private Map<String, String> variables;
}
