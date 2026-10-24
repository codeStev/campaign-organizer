package com.campaignorganizer.accounts.application.apitoken.service;

import com.campaignorganizer.accounts.application.apitoken.port.published.ApiTokenView;
import com.campaignorganizer.accounts.domain.apitoken.ApiToken;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ApiTokenViewMapper {

    ApiTokenView toView(ApiToken token);
}
