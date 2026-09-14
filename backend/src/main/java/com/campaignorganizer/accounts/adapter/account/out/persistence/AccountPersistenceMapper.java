package com.campaignorganizer.accounts.adapter.account.out.persistence;

import com.campaignorganizer.accounts.domain.account.Account;
import com.campaignorganizer.accounts.domain.account.MfaMethod;
import com.campaignorganizer.accounts.domain.account.Role;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountPersistenceMapper {

    AccountJpaEntity toEntity(Account account);

    default Account toDomain(AccountJpaEntity e) {
        if (e == null) {
            return null;
        }
        return Account.reconstitute(e.getId(), e.getEmail(), e.getPasswordHash(), Role.valueOf(e.getRole()),
                e.isEnabled(), e.getTokenVersion(), e.getFailedAttempts(), e.getLockedUntil(),
                MfaMethod.valueOf(e.getMfaMethod()), e.getTotpSecretEncrypted(), e.getTotpSecretPendingEncrypted(),
                e.getCreatedAt(), e.getUpdatedAt());
    }
}
