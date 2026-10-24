package com.campaignorganizer.accounts.adapter.account.in.web;

import com.campaignorganizer.accounts.adapter.account.in.web.ApiTokenWebDtos.ApiTokenSummary;
import com.campaignorganizer.accounts.adapter.account.in.web.ApiTokenWebDtos.CreateApiTokenRequest;
import com.campaignorganizer.accounts.adapter.account.in.web.ApiTokenWebDtos.CreatedApiToken;
import com.campaignorganizer.accounts.application.apitoken.port.in.CreateApiTokenUseCase;
import com.campaignorganizer.accounts.application.apitoken.port.in.CreateApiTokenUseCase.CreateApiTokenCommand;
import com.campaignorganizer.accounts.application.apitoken.port.in.ListApiTokensUseCase;
import com.campaignorganizer.accounts.application.apitoken.port.in.RevokeApiTokenUseCase;
import com.campaignorganizer.security.CurrentUserPort;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Self-service API token management for the current account (ADR-0119); needs a full MFA session. */
@RestController
@RequestMapping("/api/accounts/me/api-tokens")
public class ApiTokenController {

    private final CreateApiTokenUseCase createUseCase;
    private final ListApiTokensUseCase listUseCase;
    private final RevokeApiTokenUseCase revokeUseCase;
    private final CurrentUserPort currentUser;
    private final ApiTokenWebMapper mapper;

    public ApiTokenController(CreateApiTokenUseCase createUseCase, ListApiTokensUseCase listUseCase,
                              RevokeApiTokenUseCase revokeUseCase, CurrentUserPort currentUser,
                              ApiTokenWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.listUseCase = listUseCase;
        this.revokeUseCase = revokeUseCase;
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    @GetMapping
    public List<ApiTokenSummary> list() {
        return listUseCase.list(currentUser.currentAccountId()).stream().map(mapper::toSummary).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedApiToken create(@Valid @RequestBody CreateApiTokenRequest request) {
        CreateApiTokenCommand command = new CreateApiTokenCommand(currentUser.currentAccountId(), request.name(),
                mapper.scopesFromWire(request.scopes()), request.expiresAt());
        return mapper.toCreated(createUseCase.create(command));
    }

    @DeleteMapping("/{tokenId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID tokenId) {
        revokeUseCase.revoke(currentUser.currentAccountId(), tokenId);
    }
}
