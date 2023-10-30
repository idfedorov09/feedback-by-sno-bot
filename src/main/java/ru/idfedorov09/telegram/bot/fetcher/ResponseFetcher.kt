
package ru.idfedorov09.telegram.bot.fetcher

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.ForwardMessage
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.idfedorov09.telegram.bot.data.enums.Action
import ru.idfedorov09.telegram.bot.data.enums.ControlData
import ru.idfedorov09.telegram.bot.data.model.InputQuery
import ru.idfedorov09.telegram.bot.data.model.UserQuery
import ru.idfedorov09.telegram.bot.data.repo.UserQueryRepository
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
    private val userQueryRepository: UserQueryRepository,
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
        inputQuery: InputQuery,
        bot: TelegramPollingBot,
        exp: ExpContainer,
    ) {
        when (inputQuery.action) {
            Action.SEND_QUESTION -> sendQuestion(inputQuery, bot, exp, update)
            Action.GIVE_ME_ATTEMPT -> giveMeAttempt(inputQuery, bot, exp, update)
            Action.SEND_ANSWER -> sendAnswer(inputQuery, bot, exp, update)

            Action.IGNORE -> ignoreQuestion(inputQuery, bot, exp, update)
            Action.BLOCK_USER -> blockUser(inputQuery, bot, exp, update)
            else -> run {
                log.error("Detected unknown type of action.")
                return
            }
        }
    }

    private fun extractQuery(
        callbackQueryData: String,
    ) = userQueryRepository.findById(
        callbackQueryData.split(" ")[1].toLongOrNull(),
    ).get()

    // TODO: удалять все сообщения от пользователя?
    private fun blockUser(
        inputQuery: InputQuery,
        bot: TelegramPollingBot,
        exp: ExpContainer,
        update: Update,
    ) {
        val query = extractQuery(update.callbackQuery.data)
        query.authorTui ?: return

        userRepository.findByTui(query.authorTui)?.copy(
            isBanned = true,
        )?.let {
            userRepository.save(
                it,
            )
        }

        bot.execute(
            EditMessageReplyMarkup().also {
                it.chatId = ControlData.ADMINS_CHAT_ID
                it.messageId = query.consoleMessageId?.toInt()
                it.replyMarkup = InlineKeyboardMarkup(listOf())
            },
        )

        Thread.sleep(100)
        val newText = "\uD83D\uDD34 Пользователь заблокирован по воле @${inputQuery.author.lastUserNick}."
        bot.execute(
            EditMessageText().also {
                it.chatId = ControlData.ADMINS_CHAT_ID
                it.messageId = query.consoleMessageId?.toInt()
                it.text = newText
            },
        )
    }

    private fun ignoreQuestion(
        inputQuery: InputQuery,
        bot: TelegramPollingBot,
        exp: ExpContainer,
        update: Update,
    ) {
        val query = extractQuery(update.callbackQuery.data)

        bot.execute(
            EditMessageReplyMarkup().also {
                it.chatId = ControlData.ADMINS_CHAT_ID
                it.messageId = query.consoleMessageId?.toInt()
                it.replyMarkup = InlineKeyboardMarkup(listOf())
            },
        )

        Thread.sleep(100)
        // TODO: кем проигорен?
        val newText = "\uD83D\uDFE1 Ответ проигнорирован пользователем @${inputQuery.author.lastUserNick}."
        bot.execute(
            EditMessageText().also {
                it.chatId = ControlData.ADMINS_CHAT_ID
                it.messageId = query.consoleMessageId?.toInt()
                it.text = newText
            },
        )
    }

    private fun sendAnswer(
        inputQuery: InputQuery,
        bot: TelegramPollingBot,
        exp: ExpContainer,
        update: Update,
    ) {
        inputQuery.author.tui ?: return
        if (!(update.hasMessage() && update.message.hasText())) {
            bot.execute(
                SendMessage(
                    inputQuery.author.tui,
                    "❗\uFE0F Принимаются только текстовые сообщения.",
                ),
            )
            return
        }

        inputQuery.author.currentQueryId ?: return
        val query = userQueryRepository.findById(inputQuery.author.currentQueryId).get()
        query.authorTui ?: return

        userRepository.save(
            inputQuery.author.copy(currentQueryId = null),
        )

        bot.execute(
            SendMessage(
                query.authorTui,
                "Получен ответ на Ваш вопрос #${query.id}:",
            ),
        )

        Thread.sleep(100)
        bot.execute(
            SendMessage(
                query.authorTui,
                update.message.text,
            ),
        )

        Thread.sleep(100)
        bot.execute(
            SendMessage(
                inputQuery.author.tui,
                "✅ Ваш ответ передан.",
            ),
        )

        bot.execute(
            EditMessageReplyMarkup().also {
                it.chatId = ControlData.ADMINS_CHAT_ID
                it.messageId = query.consoleMessageId?.toInt()
                it.replyMarkup = InlineKeyboardMarkup(listOf())
            },
        )

        Thread.sleep(100)
        // TODO: сделать маркдаун разметку
        val newText = "\uD83D\uDFE2 Пользователем @${inputQuery.author.lastUserNick} выдан следующий ответ:\n${update.message.text}"
        bot.execute(
            EditMessageText().also {
                it.chatId = ControlData.ADMINS_CHAT_ID
                it.messageId = query.consoleMessageId?.toInt()
                it.text = newText
            },
        )
    }

    private fun giveMeAttempt(
        inputQuery: InputQuery,
        bot: TelegramPollingBot,
        exp: ExpContainer,
        update: Update,
    ) {
        inputQuery.author.tui ?: return
        inputQuery.chatId ?: return
        val query = extractQuery(update.callbackQuery.data)
        query.authorTui ?: return
        query.messageId ?: return

        userRepository.save(
            inputQuery.author.copy(
                currentQueryId = query.id,
            ),
        )

        bot.execute(
            SendMessage(
                inputQuery.author.tui,
                "Напишите мне одним сообщением ответ на нижеприведенное сообщение (#${query.id}).\n" +
                    "\uD83D\uDD38 Обратите внимание, пока принимаются только текстовые сообщения",
            ),
        )

        val msg = ForwardMessage().also {
            it.fromChatId = query.authorTui
            it.messageId = query.messageId.toInt()

            it.chatId = inputQuery.author.tui
        }

        Thread.sleep(250)
        bot.execute(msg)
    }

    private fun sendQuestion(
        inputQuery: InputQuery,
        bot: TelegramPollingBot,
        exp: ExpContainer,
        update: Update,
    ) {
        inputQuery.chatId ?: return
        inputQuery.messageId ?: return
        inputQuery.author.tui ?: return

        val query = registerNewQuery(inputQuery, "0")

        bot.execute(
            SendMessage(
                inputQuery.author.tui,
                "Ваш вопрос #${query.id} отправлен, ожидайте ответа.",
            ),
        )

        bot.execute(
            SendMessage(
                ControlData.ADMINS_CHAT_ID,
                "\uD83D\uDCE4 Получено сообщение #${query.id} от пользователя @${inputQuery.author.lastUserNick}",
            ),
        )

        val msg = ForwardMessage().also {
            it.fromChatId = inputQuery.chatId
            it.messageId = inputQuery.messageId

            it.chatId = ControlData.ADMINS_CHAT_ID
        }

        Thread.sleep(250)
        bot.execute(msg)

        Thread.sleep(250)

        val sent = bot.execute(
            SendMessage().also {
                it.chatId = ControlData.ADMINS_CHAT_ID
                it.text = "\uD83D\uDC40 Выберите действие."
                it.replyMarkup = createKeyboard(
                    listOf(
                        listOf(InlineKeyboardButton("\uD83D\uDCAC Ответ").also { it.callbackData = "ans ${query.id}" }),
                        listOf(
                            InlineKeyboardButton("\uD83D\uDD07 Игнор").also { it.callbackData = "ignore ${query.id}" },
                            InlineKeyboardButton("\uD83D\uDEAF Бан").also { it.callbackData = "ban ${query.id}" },
                        ),
                    ),
                )
            },
        )

        userQueryRepository.save(
            query.copy(
                consoleMessageId = sent.messageId.toString(),
            ),
        )
    }

    private fun registerNewQuery(
        inputQuery: InputQuery,
        consoleMessageId: String,
    ) = userQueryRepository.save(
        UserQuery(
            authorTui = inputQuery.author.tui,
            messageId = inputQuery.messageId.toString(),
            consoleMessageId = consoleMessageId,
        ),
    )

    private fun createKeyboard(keyboard: List<List<InlineKeyboardButton>>) =
        InlineKeyboardMarkup().also { it.keyboard = keyboard }
}
