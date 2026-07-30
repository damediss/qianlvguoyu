package com.guoyu.platform.certification.vo;

import java.util.List;

public record CertificationArchiveDetailVO(
    CertificationArchiveRecordVO record,
    CertificationArchiveBasicSnapshotVO basicSnapshot,
    List<CertificationArchiveMaterialVO> materials,
    List<CertificationArchiveImportBatchVO> importBatches
) {
}
