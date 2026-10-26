package org.politicaaberta.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sinaliza que o processo HTTP esta no ar. Nao consulta banco,
 * a Camara nem outro servico.
 */
@RestController
@RequestMapping("/api/health")
public class SaudeController {

    @GetMapping
    public Map<String, String> consultar() {
        return Map.of("status", "UP");
    }
}
