package com.campaignorganizer.ai.adapter.in.web;

import com.campaignorganizer.ai.application.port.in.GetAiSettingsUseCase;
import com.campaignorganizer.ai.application.port.in.TestAiProviderUseCase;
import com.campaignorganizer.ai.application.port.in.TestAiProviderUseCase.ProviderTestView;
import com.campaignorganizer.ai.application.port.in.UpdateAiSettingsUseCase;
import com.campaignorganizer.ai.application.port.in.UpdateAiSettingsUseCase.UpdateAiSettingsCommand;
import com.campaignorganizer.ai.application.port.in.UpdateAiSettingsUseCase.UpdateAiSettingsCommand.ProviderSettingInput;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Instance-global settings (ADR-0065) - no worldId; this isn't nested under
 * any world, in the API or in the frontend.
 */
@RestController
@RequestMapping("/api/ai/settings")
public class AiSettingsController {

    private final GetAiSettingsUseCase getUseCase;
    private final UpdateAiSettingsUseCase updateUseCase;
    private final TestAiProviderUseCase testUseCase;
    private final AiSettingsWebMapper mapper;

    public AiSettingsController(
            GetAiSettingsUseCase getUseCase,
            UpdateAiSettingsUseCase updateUseCase,
            TestAiProviderUseCase testUseCase,
            AiSettingsWebMapper mapper) {
        this.getUseCase = getUseCase;
        this.updateUseCase = updateUseCase;
        this.testUseCase = testUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<AiProviderSettingResponse> get() {
        return getUseCase.get().stream().map(mapper::toResponse).toList();
    }

    @PutMapping
    public List<AiProviderSettingResponse> update(@Valid @RequestBody AiSettingsRequest request) {
        List<ProviderSettingInput> inputs = request.providers().stream()
                .map(p -> new ProviderSettingInput(p.providerId(), p.model()))
                .toList();
        return updateUseCase.update(new UpdateAiSettingsCommand(inputs)).stream().map(mapper::toResponse).toList();
    }

    @PostMapping("/{providerId}/test")
    public AiProviderTestResponse test(@PathVariable String providerId) {
        ProviderTestView view = testUseCase.test(providerId);
        return new AiProviderTestResponse(view.ok(), view.model(), view.latencyMs(), view.error());
    }
}
