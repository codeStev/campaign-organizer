package com.campaignorganizer.interchange.foundry.adapter.out.http;

import com.campaignorganizer.interchange.foundry.application.port.out.FoundryRelayPort;
import com.campaignorganizer.interchange.foundry.domain.FoundryRelayException;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class FoundryRelayAdapter implements FoundryRelayPort {

    @Override
    public List<String> listConnectedClients(Credentials credentials) {
        try {
            return new FoundryRelayClient(credentials.relayBaseUrl(), credentials.apiKey()).listClients();
        } catch (RestClientException e) {
            // Never interpolate the API key or a raw exception message here — some
            // HTTP client exceptions echo request details. Only the relay's own
            // base URL (never secret) and the failure's status/type are safe to surface.
            throw new FoundryRelayException(
                    "Could not reach the Foundry relay at " + credentials.relayBaseUrl() + " ("
                            + describe(e) + ")");
        }
    }

    private static String describe(RestClientException e) {
        if (e instanceof RestClientResponseException responseException) {
            return "HTTP " + responseException.getStatusCode().value();
        }
        return e.getClass().getSimpleName();
    }
}
