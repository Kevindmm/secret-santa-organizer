package com.kdmm.secretsanta.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Render and uptime monitors often request the root path. Answering it keeps
 * the logs clean and confirms that the service is alive.
 */
@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, String> index() {
        return Map.of(
                "service", "Secret Santa Organizer",
                "status", "ok");
    }
}
