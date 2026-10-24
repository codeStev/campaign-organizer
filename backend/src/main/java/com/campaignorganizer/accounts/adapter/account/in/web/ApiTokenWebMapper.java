package com.campaignorganizer.accounts.adapter.account.in.web;

import com.campaignorganizer.accounts.adapter.account.in.web.ApiTokenWebDtos.ApiTokenSummary;
import com.campaignorganizer.accounts.adapter.account.in.web.ApiTokenWebDtos.CreatedApiToken;
import com.campaignorganizer.accounts.application.apitoken.port.in.CreateApiTokenUseCase;
import com.campaignorganizer.accounts.application.apitoken.port.published.ApiTokenView;
import com.campaignorganizer.accounts.domain.apitoken.ApiTokenScope;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ApiTokenWebMapper {

    ApiTokenSummary toSummary(ApiTokenView view);

    @Mapping(target = "token", source = "secret")
    @Mapping(target = "summary", source = "token")
    CreatedApiToken toCreated(CreateApiTokenUseCase.CreatedApiToken created);

    default String scopeToWire(ApiTokenScope scope) {
        return scope.wireValue();
    }

    default Set<ApiTokenScope> scopesFromWire(Set<String> wireValues) {
        return wireValues.stream().map(ApiTokenScope::fromWireValue).collect(Collectors.toSet());
    }
}
