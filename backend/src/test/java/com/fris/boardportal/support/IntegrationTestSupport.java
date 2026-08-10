package com.fris.boardportal.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Runs against the Postgres started by the project's docker-compose.yml
 * (docker compose up -d postgres) rather than a Testcontainers-managed
 * container. Testcontainers' docker-java client can't complete its Docker
 * handshake against some Docker Desktop builds (confirmed: docker/docker
 * compose CLI work fine against the same daemon), so tests instead reuse
 * the same Postgres already used for local dev, both here and in CI.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
public abstract class IntegrationTestSupport {

    @Autowired
    protected TestRestTemplate restTemplate;
}
