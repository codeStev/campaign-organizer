package com.campaignorganizer.accounts.adapter.account.in.web;

import com.campaignorganizer.accounts.adapter.account.in.web.MfaWebDtos.RecoverPasswordRequest;
import com.campaignorganizer.accounts.application.mfa.port.in.RecoverPasswordUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, unauthenticated password recovery — split out from {@link MfaController} the same
 * way {@link RegisterController} is split from {@link AccountAdminController}, so the one
 * endpoint reachable without any token is obvious at a glance (ADR-0111).
 */
@RestController
@RequestMapping("/api/auth")
public class RecoverPasswordController {

    private final RecoverPasswordUseCase recoverPasswordUseCase;

    public RecoverPasswordController(RecoverPasswordUseCase recoverPasswordUseCase) {
        this.recoverPasswordUseCase = recoverPasswordUseCase;
    }

    @PostMapping("/recover-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recoverPassword(@Valid @RequestBody RecoverPasswordRequest request) {
        recoverPasswordUseCase.recoverPassword(request.email(), request.recoveryCode(), request.newPassword());
    }
}
