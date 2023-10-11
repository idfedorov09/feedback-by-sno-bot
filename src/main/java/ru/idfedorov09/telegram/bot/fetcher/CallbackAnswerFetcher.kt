
package ru.idfedorov09.telegram.bot.fetcher

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.executor.TelegramPollingBot
import ru.idfedorov09.telegram.bot.fetcher.general.GeneralFetcher
import ru.idfedorov09.telegram.bot.flow.InjectData

@Component
class CallbackAnswerFetcher() : GeneralFetcher() {
    companion object {
        private val log = LoggerFactory.getLogger(this.javaClass)
    }

    @InjectData
    fun doFetch(
        update: Update,
        bot: TelegramPollingBot,
    ) {
        if (!update.hasCallbackQuery()) return

        bot.execute(
            AnswerCallbackQuery().also {
                it.callbackQueryId = update.callbackQuery.id
                it.showAlert = false
            },
        )
    }
}
