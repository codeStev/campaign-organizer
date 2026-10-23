package com.campaignorganizer.accounts.adapter.account.out.persistence;

import com.campaignorganizer.accounts.domain.apitoken.ApiToken;
import com.campaignorganizer.accounts.domain.apitoken.ApiTokenScope;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ApiTokenPersistenceMapper {

    String SCOPE_SEPARATOR = ",";

    ApiTokenJpaEntity toEntity(ApiToken token);

    default ApiToken toDomain(ApiTokenJpaEntity e) {
        if (e == null) {
            return null;
        }
        return ApiToken.reconstitute(e.getId(), e.getAccountId(), e.getAccountTokenVersion(), e.getName(), e.getTokenHash(), e.getPrefix(),
                scopesFromColumn(e.getScopes()), e.getCreatedAt(), e.getExpiresAt(), e.getLastUsedAt(),
                e.getRevokedAt());
    }

    default String scopesToColumn(Set<ApiTokenScope> scopes) {
        return scopes.stream().map(ApiTokenScope::wireValue).sorted().collect(Collectors.joining(SCOPE_SEPARATOR));
    }

    default Set<ApiTokenScope> scopesFromColumn(String column) {
        Set<ApiTokenScope> scopes = EnumSet.noneOf(ApiTokenScope.class);
        Arrays.stream(column.split(SCOPE_SEPARATOR)).map(ApiTokenScope::fromWireValue).forEach(scopes::add);
        return scopes;
    }
}
