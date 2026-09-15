package com.campaignorganizer.accounts.application.mfa.port.out;

import com.campaignorganizer.accounts.domain.recoverycode.RecoveryCode;
import java.util.List;
import java.util.UUID;

public interface RecoveryCodeRepositoryPort {

    void saveAll(List<RecoveryCode> codes);

    void save(RecoveryCode code);

    List<RecoveryCode> findUnusedByAccountId(UUID accountId);

    /** Cheaper than {@code findUnusedByAccountId(id).size()} for the self-service status check. */
    int countUnusedByAccountId(UUID accountId);

    /** Discards any leftover codes before a fresh set is issued (a re-enrollment after recovery). */
    void deleteAllByAccountId(UUID accountId);
}
