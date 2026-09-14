package com.campaignorganizer.accounts.adapter.account.out.mfa;

import com.campaignorganizer.accounts.application.mfa.port.out.RecoveryCodePort;
import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RecoveryCodeAdapter implements RecoveryCodePort {

    private final RecoveryCodeGenerator generator = new RecoveryCodeGenerator();

    @Override
    public List<String> generateCodes(int count) {
        return List.of(generator.generateCodes(count));
    }
}
