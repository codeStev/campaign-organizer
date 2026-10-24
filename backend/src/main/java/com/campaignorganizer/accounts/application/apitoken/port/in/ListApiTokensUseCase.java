package com.campaignorganizer.accounts.application.apitoken.port.in;

import com.campaignorganizer.accounts.application.apitoken.port.published.ApiTokenView;
import java.util.List;
import java.util.UUID;

/** Lists the account's non-revoked API tokens, oldest first (ADR-0119). */
public interface ListApiTokensUseCase {

    List<ApiTokenView> list(UUID accountId);
}
