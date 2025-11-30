package com.aml.analyzer.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressCheckRequest {

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Asset is required")
    private String asset;

    @NotBlank(message = "Network is required")
    private String network;

    @Builder.Default
    private boolean includeCluster = true;

    private String webhookUrl;

    private String requestId;

    @Builder.Default
    private boolean generateReport = true;
}
