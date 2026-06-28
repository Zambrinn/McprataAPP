package com.mcpratapp.repository

import com.mcpratapp.model.Role
import com.mcpratapp.model.User
import com.mcpratapp.model.UserStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserRepository : JpaRepository<User, UUID> {
    fun findByEmail(email: String): User?
    @Query(
        """
      SELECT u FROM User u
      WHERE (:role IS NULL OR u.role = :role)
      AND (:status IS NULL OR u.status = :status)
      """
    )
    fun findUsers(
        @Param("role") role: Role?,
        @Param("status") status: UserStatus?,
        pageable: Pageable
    ): Page<User>

    @Query(
        """
      SELECT u FROM User u
      WHERE (
          LOWER(u.username) LIKE CONCAT('%', :search, '%')
          OR LOWER(u.email) LIKE CONCAT('%', :search, '%')
      )
      AND (:role IS NULL OR u.role = :role)
      AND (:status IS NULL OR u.status = :status)
      """
    )
    fun searchUsers(
        @Param("search") search: String,
        @Param("role") role: Role?,
        @Param("status") status: UserStatus?,
        pageable: Pageable
    ): Page<User>
}