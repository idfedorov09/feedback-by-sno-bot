package ru.idfedorov09.telegram.bot.data.model

import ru.idfedorov09.telegram.bot.data.enums.Action

data class InputQuery(
    val author: User,
    val action: Action,
    val chatId: String?,
    val messageId: Int?,
)
