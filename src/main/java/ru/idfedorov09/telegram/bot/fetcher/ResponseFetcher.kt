
package ru.idfedorov09.telegram.bot.fetcher

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.idfedorov09.telegram.bot.data.enums.Action
import ru.idfedorov09.telegram.bot.data.model.InputQuery
import ru.idfedorov09.telegram.bot.data.repo.UserRepository
import ru.idfedorov09.telegram.bot.executor.TelegramPollingBot
import ru.idfedorov09.telegram.bot.fetcher.general.GeneralFetcher
import ru.idfedorov09.telegram.bot.flow.ExpContainer
import ru.idfedorov09.telegram.bot.flow.InjectData
import ru.idfedorov09.telegram.bot.util.UpdatesUtil

@Component
class ResponseFetcher(
    private val updatesUtil: UpdatesUtil,
    private val userRepository: UserRepository,
) : GeneralFetcher() {
    companion object {
        private val log = LoggerFactory.getLogger(this.javaClass)
        private val prefixes = listOf(
            "ans",
            "ignore",
            "ban",
        )
    }

    @InjectData
    fun doFetch(
        inputQuery: InputQuery,
        bot: TelegramPollingBot,
        exp: ExpContainer,
    ) {
        when (inputQuery.action) {
            Action.SEND_QUESTION -> sendQuestion(inputQuery, bot, exp)
            Action.BLOCK_USER -> blockUser(inputQuery, bot, exp)
            Action.GIVE_ME_ATTEMPT -> giveMeAttempt(inputQuery, bot, exp)
            Action.IGNORE -> ignoreQuestion(inputQuery, bot, exp)
            else -> return
        }
    }

    private fun sendQuestion(
        inputQuery: InputQuery,
        bot: TelegramPollingBot,
        exp: ExpContainer,
    ) {
        // TODO()
    }

    private fun blockUser(
        inputQuery: InputQuery,
        bot: TelegramPollingBot,
        exp: ExpContainer,
    ) {
        // TODO()
    }

    private fun giveMeAttempt(
        inputQuery: InputQuery,
        bot: TelegramPollingBot,
        exp: ExpContainer,
    ) {
        // TODO()
    }

    private fun ignoreQuestion(
        inputQuery: InputQuery,
        bot: TelegramPollingBot,
        exp: ExpContainer,
    ) {
        // TODO()
    }
}
