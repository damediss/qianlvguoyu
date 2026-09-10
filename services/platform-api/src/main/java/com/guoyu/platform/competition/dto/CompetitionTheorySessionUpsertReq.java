package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record CompetitionTheorySessionUpsertReq(
    @NotBlank @Pattern(regexp = "[A-Za-z][A-Za-z0-9_-]{0,15}") String sessionCode,
    @NotBlank @Size(max = 128) String sessionName,
    @NotNull Long paperSnapshotId,
    @NotNull LocalDateTime startAt,
    @NotNull LocalDateTime endAt,
    @NotEmpty List<@NotNull Long> roomIds
) {
}
