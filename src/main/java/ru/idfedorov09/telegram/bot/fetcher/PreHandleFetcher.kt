
package ru.idfedorov09.telegram.bot.fetcher

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import ru.idfedorov09.telegram.bot.data.enums.Action
import ru.idfedorov09.telegram.bot.data.enums.ControlData
import ru.idfedorov09.telegram.bot.data.model.InputQuery
import ru.idfedorov09.telegram.bot.data.model.User
import ru.idfedorov09.telegram.bot.data.repo.UserRepository
import ru.idfedorov09.telegram.bot.executor.TelegramPollingBot
import ru.idfedorov09.telegram.bot.fetcher.general.GeneralFetcher
import ru.idfedorov09.telegram.bot.flow.ExpContainer
import ru.idfedorov09.telegram.bot.flow.InjectData
import ru.idfedorov09.telegram.bot.util.UpdatesUtil

@Component
class PreHandleFetcher(
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
        update: Update,
        bot: TelegramPollingBot,
        exp: ExpContainer,
    ): InputQuery? {
        val chatId = updatesUtil.getChatId(update) ?: return invalidQuery(exp)
        val userNick = update.message.from.userName
        val user = userRepository.findByTui(chatId) ?: User(tui = chatId)
            .copy(lastUserNick = userNick)

        if (user.isBanned) return invalidQuery(exp)

        if (!(chatId != ControlData.ADMINS_CHAT_ID || isValidByAdmin(update))) return invalidQuery(exp)
        // сохраняем с обновленным ником (или новой записью)
        userRepository.save(user)

        return InputQuery(
            authorId = user.id,
            action = chooseAction(update, chatId),
            chatId = chatId,
            messageId = update.message.messageId,
        )
    }

    private fun isValidByAdmin(
        update: Update,
    ): Boolean {
        return update.hasCallbackQuery() &&
            prefixes.any { update.callbackQuery.data.startsWith(it) }
    }
    private fun chooseAction(
        update: Update,
        chatId: String,
    ): Action {
        if (chatId != ControlData.ADMINS_CHAT_ID) return Action.SEND_QUESTION
        val callbackData = update.callbackQuery.data

        return when {
            callbackData.startsWith("ans") -> Action.GIVE_ME_ATTEMPT
            callbackData.startsWith("ignore") -> Action.IGNORE
            callbackData.startsWith("ban") -> Action.BLOCK_USER
            else -> Action.UNKNOWN
        }
    }
    private fun invalidQuery(
        exp: ExpContainer,
    ): InputQuery? {
        exp.isValid = false
        return null
    }
}
