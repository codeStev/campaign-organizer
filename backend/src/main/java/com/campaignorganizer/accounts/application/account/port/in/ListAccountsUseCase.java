package com.campaignorganizer.accounts.application.account.port.in;

import com.campaignorganizer.accounts.application.account.port.published.AccountView;
import java.util.List;

public interface ListAccountsUseCase {

    List<AccountView> list();
}
