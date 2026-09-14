package com.campaignorganizer.accounts.adapter.account.in.web;

import com.campaignorganizer.accounts.adapter.account.in.web.AccountWebDtos.AccountResponse;
import com.campaignorganizer.accounts.application.account.port.published.AccountView;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountWebMapper {

    AccountResponse toResponse(AccountView view);
}
