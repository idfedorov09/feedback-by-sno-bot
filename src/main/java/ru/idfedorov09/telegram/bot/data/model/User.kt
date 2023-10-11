package ru.idfedorov09.telegram.bot.data.model

import jakarta.persistence.*

/**
 * Содержит информацию о пользователе
 */
@Entity
@Table(name = "users_table")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,

    @Column(name = "true_user_id", columnDefinition = "TEXT")
    val tui: String? = null,

    @Column(name = "is_banned")
    val isBanned: Boolean = false,

    // id запроса на который сейчас отвечает пользователь
    @Column(name = "current_query_id")
    val currentQueryId: Long? = null,

    // сохраненный с прошлого запроса никнейм пользователя
    @Column(name = "last_user_nick", columnDefinition = "TEXT")
    val lastUserNick: String? = null,
)
