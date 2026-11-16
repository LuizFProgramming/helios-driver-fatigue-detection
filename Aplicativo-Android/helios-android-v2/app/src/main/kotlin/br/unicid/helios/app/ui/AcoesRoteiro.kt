package br.unicid.helios.app.ui

import br.unicid.helios.app.Ajustes

/** Toques da tela do roteiro de teste. */
class AcoesRoteiro(
    val mudarAjustes: (Ajustes) -> Unit,
    val comecar: () -> Unit,
    val cancelar: () -> Unit,
    val voltar: () -> Unit,
)
