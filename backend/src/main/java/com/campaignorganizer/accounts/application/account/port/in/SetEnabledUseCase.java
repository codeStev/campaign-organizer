package com.campaignorganizer.accounts.application.account.port.in;

import com.campaignorganizer.accounts.application.account.port.published.AccountView;
import java.util.UUID;

public interface SetEnabledUseCase {

    AccountView setEnabled(UUID accountId, boolean enabled);
}
