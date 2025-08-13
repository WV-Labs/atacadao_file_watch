package com.mercado.filemonitor.util;

import com.mercado.filemonitor.config.ClientConfig;
import com.mercado.filemonitor.dto.ProdutoDTO;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
public class ProdutoWebClient {
    private final WebClient webClient;
    private final ClientConfig config;
    
    public ProdutoWebClient(ClientConfig config) {
        this.config = config;
        this.webClient = WebClient.builder()
                .baseUrl(config.getHost() + ":" + config.getPort() + config.getPath() + config.getEndpoint())
                .build();
    }

    public void enviarProdutos(List<ProdutoDTO> listaProdutos) {
        webClient.post()
                .uri(
                        config.getHost() + ":" +
                        config.getPort() +
                        config.getPath() +
                        config.getEndpoint() +
                        config.getProdutos_endpoint()
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(listaProdutos)
                .retrieve()
                .bodyToMono(Map.class)
                .doOnNext(response -> {
                    System.out.println("Resposta: " + response);
                })
                .block(); // bloqueia até receber a resposta (para chamadas síncronas)
    }
}
