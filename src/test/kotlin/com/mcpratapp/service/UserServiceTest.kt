package com.mcpratapp.service

import com.mcpratapp.anyNonNull
import com.mcpratapp.dto.request.UserRequest
import com.mcpratapp.dto.request.UserUpdateRequest
import com.mcpratapp.exception.ConflictException
import com.mcpratapp.exception.ResourceNotFoundException
import com.mcpratapp.model.Role
import com.mcpratapp.model.User
import com.mcpratapp.model.UserStatus
import com.mcpratapp.repository.RefreshTokenRepository
import com.mcpratapp.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class UserServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @InjectMocks
    private lateinit var userService: UserService

    private fun sampleUser(
        id: UUID = UUID.randomUUID(),
        email: String = "joao@test.com",
        status: UserStatus = UserStatus.ACTIVE
    ) = User(
        id = id,
        username = "João",
        email = email,
        password = "encoded-password",
        role = Role.VENDOR,
        status = status
    )

    @Test
    fun `createUser codifica senha e salva usuario`() {
        val savedId = UUID.randomUUID()
        val request = UserRequest("João", "joao@test.com", "123456", Role.VENDOR)
        `when`(userRepository.findByEmail("joao@test.com")).thenReturn(null)
        `when`(passwordEncoder.encode("123456")).thenReturn("hashed-123456")
        `when`(userRepository.save(anyNonNull<User>())).thenAnswer { invocation ->
            val arg = invocation.getArgument<User>(0)
            User(id = savedId, username = arg.username, email = arg.email, password = arg.password, role = arg.role, status = arg.status)
        }

        val response = userService.createUser(request)

        assertEquals(savedId, response.id)
        assertEquals("João", response.name)
        assertEquals(Role.VENDOR, response.role)
    }

    @Test
    fun `createUser lanca ConflictException para email duplicado`() {
        `when`(userRepository.findByEmail("joao@test.com")).thenReturn(sampleUser())

        assertThrows(ConflictException::class.java) {
            userService.createUser(UserRequest("João", "joao@test.com", "123456", Role.VENDOR))
        }
    }

    @Test
    fun `getAllUsers usa findUsers sem busca e searchUsers normalizado quando ha busca`() {
        val pageable = PageRequest.of(0, 10)
        val user = sampleUser()
        `when`(userRepository.findUsers(Role.VENDOR, UserStatus.ACTIVE, pageable)).thenReturn(PageImpl(listOf(user)))
        `when`(userRepository.searchUsers("joão", null, null, pageable)).thenReturn(PageImpl(listOf(user)))

        val withoutSearch = userService.getAllUsers("   ", Role.VENDOR, UserStatus.ACTIVE, pageable)
        val withSearch = userService.getAllUsers("  JOÃO  ", null, null, pageable)

        assertEquals(1, withoutSearch.totalElements)
        assertEquals(1, withSearch.totalElements)
        verify(userRepository).findUsers(Role.VENDOR, UserStatus.ACTIVE, pageable)
        verify(userRepository).searchUsers("joão", null, null, pageable)
    }

    @Test
    fun `updateUser atualiza dados com ou sem troca de senha e valida regras`() {
        val id = UUID.randomUUID()
        val user = sampleUser(id = id)
        `when`(userRepository.findById(id)).thenReturn(Optional.of(user))
        `when`(userRepository.findByEmail("joao@test.com")).thenReturn(user)
        `when`(userRepository.save(anyNonNull<User>())).thenAnswer { it.getArgument<User>(0) }

        val updated = userService.updateUser(id, UserUpdateRequest("João Silva", "joao@test.com", null, Role.ADMIN))
        assertEquals("João Silva", updated.name)
        assertEquals(Role.ADMIN, updated.role)
        verify(passwordEncoder, never()).encode(anyNonNull())

        `when`(passwordEncoder.encode("novaSenha")).thenReturn("encoded-nova")
        userService.updateUser(id, UserUpdateRequest("João Silva", "joao@test.com", "novaSenha", Role.ADMIN))
        assertEquals("encoded-nova", user.password)
    }

    @Test
    fun `updateUser rejeita usuario inativo ou email pertencente a outro usuario`() {
        val id = UUID.randomUUID()
        val inactive = sampleUser(id = id, status = UserStatus.INACTIVE)
        `when`(userRepository.findById(id)).thenReturn(Optional.of(inactive))

        assertThrows(ConflictException::class.java) {
            userService.updateUser(id, UserUpdateRequest("João", "joao@test.com", null, Role.VENDOR))
        }

        val active = sampleUser(id = id, status = UserStatus.ACTIVE)
        val other = sampleUser(id = UUID.randomUUID(), email = "outro@test.com")
        `when`(userRepository.findById(id)).thenReturn(Optional.of(active))
        `when`(userRepository.findByEmail("outro@test.com")).thenReturn(other)

        assertThrows(ConflictException::class.java) {
            userService.updateUser(id, UserUpdateRequest("João", "outro@test.com", null, Role.VENDOR))
        }
    }

    @Test
    fun `deactivateUser revoga refresh tokens e marca INACTIVE`() {
        val id = UUID.randomUUID()
        val user = sampleUser(id = id, status = UserStatus.ACTIVE)
        `when`(userRepository.findById(id)).thenReturn(Optional.of(user))
        `when`(userRepository.save(anyNonNull<User>())).thenAnswer { it.getArgument<User>(0) }

        val result = userService.deactivateUser(id)

        assertEquals(UserStatus.INACTIVE, result.status)
        verify(refreshTokenRepository).deleteByUser(user)

        assertThrows(ConflictException::class.java) { userService.deactivateUser(id) }
    }

    @Test
    fun `restoreUser reativa usuario inativo e bloqueia se ja ativo ou email em uso`() {
        val id = UUID.randomUUID()
        val inactive = sampleUser(id = id, status = UserStatus.INACTIVE)
        `when`(userRepository.findById(id)).thenReturn(Optional.of(inactive))
        `when`(userRepository.findByEmail(inactive.email)).thenReturn(sampleUser(id = UUID.randomUUID(), email = inactive.email, status = UserStatus.ACTIVE))

        assertThrows(ConflictException::class.java) { userService.restoreUser(id) }

        `when`(userRepository.findByEmail(inactive.email)).thenReturn(inactive)
        `when`(userRepository.save(anyNonNull<User>())).thenAnswer { it.getArgument<User>(0) }

        val restored = userService.restoreUser(id)
        assertEquals(UserStatus.ACTIVE, restored.status)

        assertThrows(ConflictException::class.java) { userService.restoreUser(id) }
    }

    @Test
    fun `deleteUser marca como DELETED, remove refresh tokens e bloqueia exclusao repetida`() {
        val id = UUID.randomUUID()
        val user = sampleUser(id = id, status = UserStatus.ACTIVE)
        `when`(userRepository.findById(id)).thenReturn(Optional.of(user))
        `when`(userRepository.save(anyNonNull<User>())).thenAnswer { it.getArgument<User>(0) }

        userService.deleteUser(id)

        assertEquals(UserStatus.DELETED, user.status)
        verify(refreshTokenRepository).deleteByUser(user)

        assertThrows(ConflictException::class.java) { userService.deleteUser(id) }
    }

    @Test
    fun `getUserById lanca ResourceNotFoundException quando ausente`() {
        val id = UUID.randomUUID()
        `when`(userRepository.findById(id)).thenReturn(Optional.empty())

        assertThrows(ResourceNotFoundException::class.java) { userService.getUserById(id) }
    }
}
