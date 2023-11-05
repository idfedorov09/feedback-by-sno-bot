package ru.idfedorov09.telegram.bot.data.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.idfedorov09.telegram.bot.data.model.UserQuery

interface UserQueryRepository : JpaRepository<UserQuery, Long> {
    @Query("SELECT COUNT(u) > 0 FROM UserQuery u WHERE u.authorTui = :tui")
    fun existsByAuthorTui(@Param("tui") tui: String): Boolean
}
