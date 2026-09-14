package com.campaignorganizer.accounts.adapter.account.out.persistence;

import com.campaignorganizer.accounts.domain.recoverycode.RecoveryCode;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RecoveryCodePersistenceMapper {

    AccountRecoveryCodeJpaEntity toEntity(RecoveryCode code);

    default RecoveryCode toDomain(AccountRecoveryCodeJpaEntity e) {
        if (e == null) {
            return null;
        }
        return RecoveryCode.reconstitute(e.getId(), e.getAccountId(), e.getCodeHash(), e.getUsedAt(),
                e.getCreatedAt());
    }
}
