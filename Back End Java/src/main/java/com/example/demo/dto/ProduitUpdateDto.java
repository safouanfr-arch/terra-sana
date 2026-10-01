package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(value = "stockActuel", allowGetters = true)
public class ProduitUpdateDto extends ProduitDto {
}
