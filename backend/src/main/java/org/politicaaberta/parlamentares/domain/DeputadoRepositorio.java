package org.politicaaberta.parlamentares.domain;

import java.util.List;
import java.util.Optional;

public interface DeputadoRepositorio {

    void salvar(Deputado deputado);

    Optional<Deputado> buscarPorIdCamara(long idCamara);

    List<Deputado> listar();

    RecorteDeputados consultar(FiltroDeputados filtro, int deslocamento, int limite);
}
