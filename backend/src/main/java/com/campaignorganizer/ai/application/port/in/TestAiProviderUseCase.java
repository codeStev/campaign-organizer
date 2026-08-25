package com.campaignorganizer.ai.application.port.in;

/**
 * Sends a trivial completion request to one provider as a connectivity check
 * for the Settings UI. A failed test is a normal, expected outcome of a
 * diagnostic — it's reported as data ({@link ProviderTestView#error}), not as
 * an exception, so the UI can show ✗ with the reason next to the provider.
 */
public interface TestAiProviderUseCase {

    ProviderTestView test(String providerId);

    /**
     * @param error null when {@code ok} is true; otherwise why the test failed
     *     (unconfigured key, HTTP error, empty answer).
     */
    record ProviderTestView(boolean ok, String model, long latencyMs, String error) {
    }
}
