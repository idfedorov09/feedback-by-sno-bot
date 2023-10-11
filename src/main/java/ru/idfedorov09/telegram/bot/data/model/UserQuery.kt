package ru.idfedorov09.telegram.bot.data.model

import jakarta.persistence.*

/**
 * Таблица с вопросами пользователей
 */
@Entity
@Table(name = "query_table")
data class UserQuery(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,

    @Column(name = "author_id")
    val authorId: String? = null,

    @Column(name = "message_id")
    val messageId: String? = null,
)