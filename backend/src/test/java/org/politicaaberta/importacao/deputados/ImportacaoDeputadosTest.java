package org.politicaaberta.importacao.deputados;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.politicaaberta.importacao.camara.ClienteDeputadosCamara;
import org.politicaaberta.importacao.camara.DeputadoEmLista;
import org.politicaaberta.importacao.http.RespostaHttp;
import org.politicaaberta.importacao.http.TransporteHttp;
import org.politicaaberta.parlamentares.domain.Deputado;
import org.politicaaberta.parlamentares.domain.DeputadoRepositorio;
import org.politicaaberta.parlamentares.domain.FiltroDeputados;
import org.politicaaberta.parlamentares.domain.RecorteDeputados;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportacaoDeputadosTest {

    private static final URI PAGINA_2 = URI.create(
            "https://dadosabertos.camara.leg.br/api/v2/deputados?ordem=asc&ordenarPor=id&pagina=2&itens=100");
    private static final URI PAGINA_3 = URI.create(
            "https://dadosabertos.camara.leg.br/api/v2/deputados?ordem=asc&ordenarPor=id&pagina=3&itens=100");
    private static final Instant INICIO = Instant.parse("2026-10-26T18:00:00Z");

    @Test
    void converteCamposDaListaEIgnoraUriDoPartido() {
        Deputado deputado = ConversorDeputado.paraDeputado(
                new DeputadoEmLista(7, null, "Ana", null, "https://exemplo.test/partido", "AM", null, null, "ana@exemplo.test"),
                INICIO);

        assertEquals(7, deputado.idCamara());
        assertNull(deputado.uri());
        assertEquals("Ana", deputado.nome());
        assertNull(deputado.siglaPartido());
        assertEquals("AM", deputado.siglaUf());
        assertNull(deputado.idLegislatura());
        assertNull(deputado.urlFoto());
        assertEquals("ana@exemplo.test", deputado.email());
        assertEquals(INICIO, deputado.atualizadoEm());
    }

    @Test
    void percorreTodasAsPaginasEAtualizaSemDuplicar() {
        TransporteFixo transporte = paginasCompletas();
        DeputadosMemoria deputados = new DeputadosMemoria();
        deputados.salvar(new Deputado(999, null, "Antigo", null, null, null, null, null, INICIO));
        ExecucoesMemoria execucoes = new ExecucoesMemoria();
        RelogioManual relogio = new RelogioManual(INICIO);
        ImportacaoDeputados servico = new ImportacaoDeputados(
                new ClienteDeputadosCamara(transporte), deputados, execucoes, relogio);

        assertTrue(transporte.chamadas().isEmpty());
        ExecucaoImportacao primeira = servico.executar();
        relogio.avancar(Duration.ofMinutes(5));
        ExecucaoImportacao segunda = servico.executar();

        assertEquals(List.of(ClienteDeputadosCamara.URL_LISTA, PAGINA_2, PAGINA_3), transporte.chamadas().subList(0, 3));
        assertEquals(List.of(101L, 102L, 103L, 999L), deputados.ids());
        assertEquals("Pessoa Beta", deputados.buscarPorIdCamara(102).orElseThrow().nome());
        assertEquals(INICIO.plus(Duration.ofMinutes(5)), deputados.buscarPorIdCamara(101).orElseThrow().atualizadoEm());
        assertEquals(StatusImportacao.CONCLUIDA, primeira.status());
        assertEquals(3, primeira.quantidade());
        assertEquals(INICIO, primeira.iniciadaEm());
        assertEquals(INICIO, primeira.terminadaEm());
        assertEquals(3, segunda.quantidade());
        assertEquals(StatusImportacao.CONCLUIDA, segunda.status());
        assertEquals(2, execucoes.total());
    }

    @Test
    void falhaDePaginaPreservaOQueJaFoiGravadoENaoConclui() {
        TransporteFixo transporte = new TransporteFixo()
                .resposta(ClienteDeputadosCamara.URL_LISTA, 200, "3", fixture("pagina-1.json"))
                .resposta(PAGINA_2, 500, null, "erro");
        DeputadosMemoria deputados = new DeputadosMemoria();
        ExecucoesMemoria execucoes = new ExecucoesMemoria();
        ImportacaoDeputados servico = new ImportacaoDeputados(
                new ClienteDeputadosCamara(transporte), deputados, execucoes, new RelogioManual(INICIO));

        FalhaImportacaoDeputados erro = assertThrows(FalhaImportacaoDeputados.class, servico::executar);

        assertEquals(List.of(101L, 102L), deputados.ids());
        assertEquals(StatusImportacao.FALHOU, erro.execucao().status());
        assertEquals(2, erro.execucao().quantidade());
        assertEquals(INICIO, erro.execucao().terminadaEm());
        assertTrue(erro.execucao().mensagemErro().contains("HTTP 500"));
        assertTrue(execucoes.todas().stream().noneMatch(item -> item.status() == StatusImportacao.CONCLUIDA));
        assertEquals(List.of(PAGINA_2), transporte.chamadas().subList(1, 2));
        assertTrue(transporte.chamadas().stream().noneMatch(PAGINA_3::equals));
    }

    @Test
    void impedeExecucaoSimultanea() throws Exception {
        CountDownLatch dentro = new CountDownLatch(1);
        CountDownLatch seguir = new CountDownLatch(1);
        TransporteHttp transporte = uri -> {
            dentro.countDown();
            try {
                assertTrue(seguir.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException erro) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(erro);
            }
            return new RespostaHttp(200, Map.of("X-Total-Count", List.of("1")), fixture("sem-links.json"));
        };
        ExecucoesMemoria execucoes = new ExecucoesMemoria();
        ImportacaoDeputados servico = new ImportacaoDeputados(
                new ClienteDeputadosCamara(transporte), new DeputadosMemoria(), execucoes, new RelogioManual(INICIO));
        AtomicReference<ExecucaoImportacao> primeira = new AtomicReference<>();
        AtomicReference<Throwable> falhaDaPrimeira = new AtomicReference<>();
        Thread ocupada = new Thread(() -> {
            try {
                primeira.set(servico.executar());
            } catch (Throwable erro) {
                falhaDaPrimeira.set(erro);
            }
        });

        ocupada.start();
        assertTrue(dentro.await(5, TimeUnit.SECONDS));
        assertThrows(ImportacaoEmAndamento.class, servico::executar);
        seguir.countDown();
        ocupada.join(5000);

        assertNull(falhaDaPrimeira.get());
        assertEquals(StatusImportacao.CONCLUIDA, primeira.get().status());
        assertEquals(1, execucoes.total());
    }

    private static TransporteFixo paginasCompletas() {
        return new TransporteFixo()
                .resposta(ClienteDeputadosCamara.URL_LISTA, 200, "3", fixture("pagina-1.json"))
                .resposta(PAGINA_2, 200, "999", fixture("pagina-2.json"))
                .resposta(PAGINA_3, 200, "0", fixture("pagina-vazia.json"));
    }

    private static String fixture(String nome) {
        String caminho = "/fixtures/camara/deputados/" + nome;
        try (InputStream entrada = ImportacaoDeputadosTest.class.getResourceAsStream(caminho)) {
            if (entrada == null) {
                throw new IllegalStateException("fixture ausente: " + caminho);
            }
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException erro) {
            throw new IllegalStateException(erro);
        }
    }

    private static final class RelogioManual extends Clock {
        private Instant atual;

        private RelogioManual(Instant atual) {
            this.atual = atual;
        }

        private void avancar(Duration duracao) {
            atual = atual.plus(duracao);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return atual;
        }
    }

    private static final class DeputadosMemoria implements DeputadoRepositorio {
        private final Map<Long, Deputado> porId = new LinkedHashMap<>();

        @Override
        public void salvar(Deputado deputado) {
            porId.put(deputado.idCamara(), deputado);
        }

        @Override
        public Optional<Deputado> buscarPorIdCamara(long idCamara) {
            return Optional.ofNullable(porId.get(idCamara));
        }

        @Override
        public List<Deputado> listar() {
            return porId.values().stream().sorted(Comparator.comparingLong(Deputado::idCamara)).toList();
        }

        @Override
        public RecorteDeputados consultar(FiltroDeputados filtro, int deslocamento, int limite) {
            throw new UnsupportedOperationException();
        }

        private List<Long> ids() {
            return listar().stream().map(Deputado::idCamara).toList();
        }
    }

    private static final class ExecucoesMemoria implements ExecucaoImportacaoRepositorio {
        private final List<ExecucaoImportacao> itens = new ArrayList<>();
        private long sequencia = 1;

        @Override
        public synchronized ExecucaoImportacao iniciar(String tipo, Instant inicio) {
            boolean ativa = itens.stream().anyMatch(item ->
                    item.tipo().equals(tipo) && item.status() == StatusImportacao.EM_EXECUCAO);
            if (ativa) {
                throw new ImportacaoEmAndamento(tipo);
            }
            ExecucaoImportacao criada = new ExecucaoImportacao(
                    sequencia++, tipo, inicio, null, StatusImportacao.EM_EXECUCAO, 0, null);
            itens.add(criada);
            return criada;
        }

        @Override
        public synchronized ExecucaoImportacao concluir(long id, int quantidade, Instant termino) {
            return substituir(id, quantidade, termino, StatusImportacao.CONCLUIDA, null);
        }

        @Override
        public synchronized ExecucaoImportacao falhar(long id, int quantidade, Instant termino, String mensagem) {
            return substituir(id, quantidade, termino, StatusImportacao.FALHOU, mensagem);
        }

        private ExecucaoImportacao substituir(
                long id, int quantidade, Instant termino, StatusImportacao status, String mensagem) {
            for (int indice = 0; indice < itens.size(); indice++) {
                ExecucaoImportacao atual = itens.get(indice);
                if (atual.id() == id) {
                    ExecucaoImportacao nova = new ExecucaoImportacao(
                            atual.id(), atual.tipo(), atual.iniciadaEm(), termino, status, quantidade, mensagem);
                    itens.set(indice, nova);
                    return nova;
                }
            }
            throw new IllegalStateException("execucao ausente: " + id);
        }

        private int total() {
            return itens.size();
        }

        private List<ExecucaoImportacao> todas() {
            return List.copyOf(itens);
        }
    }

    private static final class TransporteFixo implements TransporteHttp {
        private final Map<URI, RespostaHttp> respostas = new LinkedHashMap<>();
        private final List<URI> chamadas = new ArrayList<>();

        private TransporteFixo resposta(URI uri, int status, String total, String corpo) {
            Map<String, List<String>> cabecalhos = total == null
                    ? Map.of()
                    : Map.of("X-Total-Count", List.of(total));
            respostas.put(uri, new RespostaHttp(status, cabecalhos, corpo));
            return this;
        }

        private List<URI> chamadas() {
            return List.copyOf(chamadas);
        }

        @Override
        public RespostaHttp get(URI uri) {
            chamadas.add(uri);
            RespostaHttp resposta = respostas.get(uri);
            if (resposta == null) {
                throw new AssertionError("URL nao prevista: " + uri);
            }
            return resposta;
        }
    }
}
