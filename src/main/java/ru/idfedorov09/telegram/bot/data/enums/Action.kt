package ru.idfedorov09.telegram.bot.data.enums

enum class Action {
    GIVE_ME_ATTEMPT, // админ вызвался ответить на вопрос
    BLOCK_USER, // админ блокирует пользователя (удаляя все пришедшие от него запросы)
    IGNORE, // админ игнорирует вопрос
    SEND_QUESTION, // пользователь задает вопрос

    UNKNOWN, // пришло в результате ошибки
}
