package com.campaignorganizer.interchange.foundry.adapter.in.web;

import com.campaignorganizer.interchange.foundry.adapter.in.web.FoundryConnectionWebDtos.FoundryConnectionRequest;
import com.campaignorganizer.interchange.foundry.adapter.in.web.FoundryConnectionWebDtos.FoundryConnectionResponse;
import com.campaignorganizer.interchange.foundry.adapter.in.web.FoundryConnectionWebDtos.FoundryConnectionTestResponse;
import com.campaignorganizer.interchange.foundry.application.port.in.GetFoundryConnectionUseCase;
import com.campaignorganizer.interchange.foundry.application.port.in.SaveFoundryConnectionUseCase;
import com.campaignorganizer.interchange.foundry.application.port.in.TestFoundryConnectionUseCase;
import com.campaignorganizer.shared.domain.NotFoundException;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Per-world, user-supplied Foundry relay connection settings (ADR-0115).
 * There is no shared/bundled relay — every world's relay URL, API key, and
 * clientId are entered by that account's own user, pointing at a Foundry
 * relay they self-host. */
@RestController
@RequestMapping("/api/worlds/{worldId}/foundry-connection")
public class FoundryConnectionController {

    private final GetFoundryConnectionUseCase getUseCase;
    private final SaveFoundryConnectionUseCase saveUseCase;
    private final TestFoundryConnectionUseCase testUseCase;
    private final FoundryConnectionWebMapper mapper;

    public FoundryConnectionController(GetFoundryConnectionUseCase getUseCase,
                                       SaveFoundryConnectionUseCase saveUseCase,
                                       TestFoundryConnectionUseCase testUseCase,
                                       FoundryConnectionWebMapper mapper) {
        this.getUseCase = getUseCase;
        this.saveUseCase = saveUseCase;
        this.testUseCase = testUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public FoundryConnectionResponse get(@PathVariable UUID worldId) {
        return getUseCase.get(worldId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new NotFoundException("No Foundry connection configured for this world"));
    }

    @PutMapping
    public FoundryConnectionResponse save(@PathVariable UUID worldId, @RequestBody FoundryConnectionRequest request) {
        return mapper.toResponse(saveUseCase.save(worldId, mapper.toCommand(request)));
    }

    @PostMapping("/test")
    public FoundryConnectionTestResponse test(@PathVariable UUID worldId) {
        return mapper.toTestResponse(testUseCase.test(worldId));
    }
}
