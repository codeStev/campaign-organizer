package com.campaignorganizer.accounts.adapter.account.in.web;

import com.campaignorganizer.accounts.adapter.account.in.web.AccountWebDtos.RegisterRequest;
import com.campaignorganizer.accounts.adapter.account.in.web.AccountWebDtos.RegistrationAccepted;
import com.campaignorganizer.accounts.application.account.port.in.AccountCommands.RegisterAccountCommand;
import com.campaignorganizer.accounts.application.account.port.in.RegisterAccountUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, unauthenticated registration — split from {@link AccountAdminController} so the
 * one endpoint reachable without a token is obvious at a glance, not buried among admin-only ones.
 */
@RestController
@RequestMapping("/api/accounts")
public class RegisterController {

    private static final String ACCEPTED_MESSAGE =
            "If that email was available, your account has been created. Log in to continue.";

    private final RegisterAccountUseCase registerUseCase;

    public RegisterController(RegisterAccountUseCase registerUseCase) {
        this.registerUseCase = registerUseCase;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RegistrationAccepted register(@Valid @RequestBody RegisterRequest request) {
        registerUseCase.register(new RegisterAccountCommand(request.email(), request.password()));
        return new RegistrationAccepted(ACCEPTED_MESSAGE);
    }
}
