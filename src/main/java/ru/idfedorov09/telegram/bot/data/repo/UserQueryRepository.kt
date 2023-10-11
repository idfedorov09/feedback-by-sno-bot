package ru.idfedorov09.telegram.bot.data.repo

import org.springframework.data.jpa.repository.JpaRepository
import ru.idfedorov09.telegram.bot.data.model.UserQuery

interface UserQueryRepository : JpaRepository<UserQuery, Long>
