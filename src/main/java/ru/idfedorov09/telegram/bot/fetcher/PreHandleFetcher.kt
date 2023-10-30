
package ru.idfedorov09.telegram.bot.fetcher

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
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
        if (!(chatId != ControlData.ADMINS_CHAT_ID || isValidByAdmin(update))) return invalidQuery(exp)
        var userNick = when {
            update.hasMessage() -> update.message.from.userName
            update.hasCallbackQuery() -> update.callbackQuery.from.userName
            else -> null
        }

        if (update.hasMessage() && update.message.hasText() &&
            update.message.text.lowercase() == "/start"
        ) {
            bot.execute(
                SendMessage(
                    chatId,
                    "Добро пожаловать в бота для обратной связи.\n" +
                        "Напишите свой вопрос, и мы на него ответим!",
                ),
            )
            return invalidQuery(exp)
        }

        var tui = chatId
        if (isValidByAdmin(update) && chatId == ControlData.ADMINS_CHAT_ID) {
            tui = update.callbackQuery.from.id.toString()
        }

        val preUser = userRepository.findByTui(tui)
        val user = (preUser ?: User(tui = tui))
            .copy(lastUserNick = userNick)

        userNick ?: run { userNick = user.lastUserNick }
        if (user.isBanned) return invalidQuery(exp)

        if (preUser == null && chatId == ControlData.ADMINS_CHAT_ID) {
            bot.execute(
                SendMessage(
                    ControlData.ADMINS_CHAT_ID,
                    "\uD83E\uDEF8 @{user.lastUserNick}, напишите мне что-нибудь в лс, пожалуйста, перед тем как что-то делать.",
                ),
            )
            return invalidQuery(exp)
        }

        // сохраняем с обновленным ником (или новой записью)
        userRepository.save(user)

        return InputQuery(
            author = user,
            action = chooseAction(update, chatId, user),
            chatId = chatId,
            messageId = if (update.hasMessage()) update.message.messageId else null,
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
        user: User,
    ): Action {
        if (chatId != ControlData.ADMINS_CHAT_ID) {
            if (user.currentQueryId != null) return Action.SEND_ANSWER
            return Action.SEND_QUESTION
        }
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
