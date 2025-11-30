package com.aml.analyzer.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * BestChange-compatible request format.
 * Matches what their platform sends to partner analyzers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BestChangeRequest {

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Asset is required")
    private String asset;

    @Email(message = "Valid email required")
    private String email;

    // Optional: BestChange may send network explicitly
    private String network;
}
