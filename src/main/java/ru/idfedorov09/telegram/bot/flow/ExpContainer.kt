package ru.idfedorov09.telegram.bot.flow

/**
 * Объект контекста флоу, содержащий информацию о работающих фичах, режимах и тд и тп
 */
@Mutable
data class ExpContainer(
    var isValid: Boolean = true
)
