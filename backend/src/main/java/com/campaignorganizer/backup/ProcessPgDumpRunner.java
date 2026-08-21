package com.campaignorganizer.backup;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Shells out to the real {@code pg_dump} binary (ADR-0055; requires
 * {@code postgresql-client-16} in the runtime image, see backend/Dockerfile).
 * Connection parameters are parsed from the same {@code spring.datasource.url}
 * used for the JDBC connection.
 */
@Component
public class ProcessPgDumpRunner implements PgDumpRunner {

    private static final Logger log = LoggerFactory.getLogger(ProcessPgDumpRunner.class);

    private final String host;
    private final String port;
    private final String database;
    private final String username;
    private final String password;

    public ProcessPgDumpRunner(
            @Value("${spring.datasource.url}") String jdbcUrl,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password) {
        URI uri = URI.create(jdbcUrl.substring("jdbc:".length()));
        this.host = uri.getHost();
        this.port = String.valueOf(uri.getPort() < 0 ? 5432 : uri.getPort());
        this.database = uri.getPath().substring(1); // drop leading '/'
        this.username = username;
        this.password = password;
    }

    @Override
    public InputStream dump() throws IOException {
        // Plain SQL (-Fp), not custom format: pg_dump's custom-format archive has its
        // own internal version that a same-or-newer *server* connection doesn't help
        // with — an older pg_restore (e.g. bundled with postgres:16-alpine) can fail
        // to read an archive written by a newer pg_dump (verified: v18 dump -> v16
        // pg_restore fails "unsupported version in file header"). Plain SQL has no
        // such archive-format versioning; it restores anywhere via `psql` (ADR-0055).
        ProcessBuilder builder = new ProcessBuilder(
                "pg_dump", "-h", host, "-p", port, "-U", username, "-d", database, "-Fp");
        builder.environment().put("PGPASSWORD", password);
        Process process = builder.start();
        process.getOutputStream().close();
        drainStderrAsync(process);
        return new ProcessExitCheckingStream(process.getInputStream(), process);
    }

    private void drainStderrAsync(Process process) {
        Thread.ofVirtual().start(() -> {
            try (InputStream err = process.getErrorStream()) {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                err.transferTo(buf);
                if (buf.size() > 0) {
                    log.warn("pg_dump stderr: {}", buf.toString(StandardCharsets.UTF_8));
                }
            } catch (IOException e) {
                log.warn("Failed to drain pg_dump stderr", e);
            }
        });
    }

    /** Waits for the process and logs a non-zero exit code once the stream is closed. */
    private static final class ProcessExitCheckingStream extends FilterInputStream {
        private final Process process;

        ProcessExitCheckingStream(InputStream in, Process process) {
            super(in);
            this.process = process;
        }

        @Override
        public void close() throws IOException {
            super.close();
            try {
                int exit = process.waitFor();
                if (exit != 0) {
                    log.error("pg_dump exited with status {} — the downloaded backup is incomplete/corrupt", exit);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
