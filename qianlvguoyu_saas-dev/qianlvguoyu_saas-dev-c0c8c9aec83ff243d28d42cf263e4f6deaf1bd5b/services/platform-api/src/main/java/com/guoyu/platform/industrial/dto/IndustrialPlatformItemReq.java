package com.guoyu.platform.industrial.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record IndustrialPlatformItemReq(
    @NotBlank @Size(max = 200) String company,
    @NotBlank @Size(max = 200) String name,
    @Min(0) Integer nums,
    LocalDate date,
    @Size(max = 80) String state
) {
}
