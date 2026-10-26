package org.politicaaberta.web;

import org.politicaaberta.importacao.deputados.FalhaImportacaoDeputados;
import org.politicaaberta.importacao.deputados.ImportacaoEmAndamento;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class TratamentoDeErros {

    @ExceptionHandler(RequisicaoInvalida.class)
    ResponseEntity<MensagemErro> requisicaoInvalida(RequisicaoInvalida erro) {
        return ResponseEntity.badRequest().body(new MensagemErro(erro.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<MensagemErro> tipoInvalido(MethodArgumentTypeMismatchException erro) {
        return ResponseEntity.badRequest().body(new MensagemErro("Parametro " + erro.getName() + " invalido."));
    }

    @ExceptionHandler(DeputadoNaoEncontrado.class)
    ResponseEntity<MensagemErro> ausente(DeputadoNaoEncontrado erro) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MensagemErro(erro.getMessage()));
    }

    @ExceptionHandler(ImportacaoEmAndamento.class)
    ResponseEntity<MensagemErro> conflito(ImportacaoEmAndamento erro) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new MensagemErro("Ja existe uma importacao de deputados em execucao."));
    }

    @ExceptionHandler(FalhaImportacaoDeputados.class)
    ResponseEntity<ExecucaoImportacaoResposta> importacaoFalhou(FalhaImportacaoDeputados erro) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ExecucaoImportacaoResposta.de(erro.execucao()));
    }
}
