package com.mcpratapp.service

import com.mcpratapp.anyNonNull
import com.mcpratapp.dto.request.ProductVendorRequest
import com.mcpratapp.dto.request.ProductVendorUpdateRequest
import com.mcpratapp.exception.ConflictException
import com.mcpratapp.exception.ForbidenException
import com.mcpratapp.exception.ResourceNotFoundException
import com.mcpratapp.model.Product
import com.mcpratapp.model.ProductVendor
import com.mcpratapp.model.Role
import com.mcpratapp.model.User
import com.mcpratapp.model.UserStatus
import com.mcpratapp.repository.ProductRepository
import com.mcpratapp.repository.ProductVendorRepository
import com.mcpratapp.repository.UserRepository
import com.mcpratapp.security.AuthenticatedUser
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ProductVendorServiceTest {

    @Mock
    private lateinit var productVendorRepository: ProductVendorRepository

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @InjectMocks
    private lateinit var productVendorService: ProductVendorService

    private val vendorId = UUID.randomUUID()
    private val otherVendorId = UUID.randomUUID()
    private val adminId = UUID.randomUUID()
    private val productId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.clearContext()
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    private fun authenticate(id: UUID, role: Role) {
        val user = AuthenticatedUser(id, "user@test.com", role)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(user, null, emptyList())
    }

    private fun product(isActive: Boolean = true) = Product(
        id = productId,
        sku = "SKU-1",
        name = "Pulseira",
        totalQuantity = 10,
        reservedQuantity = 0,
        isActive = isActive
    )

    private fun vendor(id: UUID = vendorId, status: UserStatus = UserStatus.ACTIVE) = User(
        id = id,
        username = "vendedor",
        email = "v@test.com",
        password = "hash",
        role = Role.VENDOR,
        status = status
    )

    @Test
    fun `createProductVendor bloqueia vendedor tentando vincular produto a outro vendedor`() {
        authenticate(vendorId, Role.VENDOR)
        val request = ProductVendorRequest(productId, otherVendorId, BigDecimal("99.90"))

        assertThrows(ForbidenException::class.java) {
            productVendorService.createProductVendor(request)
        }
    }

    @Test
    fun `createProductVendor cria novo vinculo quando admin vincula para vendedor`() {
        authenticate(adminId, Role.ADMIN)
        val savedId = UUID.randomUUID()
        val request = ProductVendorRequest(productId, vendorId, BigDecimal("120.00"))

        `when`(productRepository.findById(productId)).thenReturn(Optional.of(product()))
        `when`(userRepository.findById(vendorId)).thenReturn(Optional.of(vendor()))
        `when`(productVendorRepository.findByVendorIdAndProductId(vendorId, productId)).thenReturn(null)
        `when`(productVendorRepository.save(anyNonNull<ProductVendor>())).thenAnswer { invocation ->
            val arg = invocation.getArgument<ProductVendor>(0)
            ProductVendor(id = savedId, product = arg.product, vendor = arg.vendor, price = arg.price, isActive = arg.isActive)
        }

        val response = productVendorService.createProductVendor(request)

        assertEquals(savedId, response.id)
        assertEquals(BigDecimal("120.00"), response.price)
        assertTrue(response.isActive)
    }

    @Test
    fun `createProductVendor reativa vinculo inativo existente atualizando preco`() {
        authenticate(vendorId, Role.VENDOR)
        val existingId = UUID.randomUUID()
        val existing = ProductVendor(
            id = existingId,
            product = product(),
            vendor = vendor(),
            price = BigDecimal("50.00"),
            isActive = false
        )
        val request = ProductVendorRequest(productId, vendorId, BigDecimal("75.00"))

        `when`(productRepository.findById(productId)).thenReturn(Optional.of(product()))
        `when`(userRepository.findById(vendorId)).thenReturn(Optional.of(vendor()))
        `when`(productVendorRepository.findByVendorIdAndProductId(vendorId, productId)).thenReturn(existing)
        `when`(productVendorRepository.save(anyNonNull<ProductVendor>())).thenAnswer { it.getArgument<ProductVendor>(0) }

        val response = productVendorService.createProductVendor(request)

        assertEquals(existingId, response.id)
        assertEquals(BigDecimal("75.00"), response.price)
        assertTrue(response.isActive)
    }

    @Test
    fun `createProductVendor rejeita produto inativo, vendedor inativo e vinculo ja ativo`() {
        authenticate(adminId, Role.ADMIN)
        val request = ProductVendorRequest(productId, vendorId, BigDecimal("50.00"))

        `when`(productRepository.findById(productId)).thenReturn(Optional.of(product(isActive = false)))
        assertThrows(ConflictException::class.java) { productVendorService.createProductVendor(request) }

        `when`(productRepository.findById(productId)).thenReturn(Optional.of(product(isActive = true)))
        `when`(userRepository.findById(vendorId)).thenReturn(Optional.of(vendor(status = UserStatus.INACTIVE)))
        assertThrows(ConflictException::class.java) { productVendorService.createProductVendor(request) }

        `when`(userRepository.findById(vendorId)).thenReturn(Optional.of(vendor(status = UserStatus.ACTIVE)))
        val activeLink = ProductVendor(id = UUID.randomUUID(), product = product(), vendor = vendor(), price = BigDecimal.TEN, isActive = true)
        `when`(productVendorRepository.findByVendorIdAndProductId(vendorId, productId)).thenReturn(activeLink)
        assertThrows(ConflictException::class.java) { productVendorService.createProductVendor(request) }
    }

    @Test
    fun `updateProductVendor permite dono ou admin e bloqueia outro vendedor`() {
        val linkId = UUID.randomUUID()
        val link = ProductVendor(id = linkId, product = product(), vendor = vendor(vendorId), price = BigDecimal("40.00"))
        `when`(productVendorRepository.findById(linkId)).thenReturn(Optional.of(link))

        authenticate(otherVendorId, Role.VENDOR)
        assertThrows(ForbidenException::class.java) {
            productVendorService.updateProductVendor(linkId, ProductVendorUpdateRequest(BigDecimal("60.00")))
        }

        authenticate(vendorId, Role.VENDOR)
        `when`(productVendorRepository.save(anyNonNull<ProductVendor>())).thenAnswer { it.getArgument<ProductVendor>(0) }
        val updated = productVendorService.updateProductVendor(linkId, ProductVendorUpdateRequest(BigDecimal("60.00")))
        assertEquals(BigDecimal("60.00"), updated.price)
    }

    @Test
    fun `deactivate e restore alternam estado e verificam permissao`() {
        val linkId = UUID.randomUUID()
        val link = ProductVendor(id = linkId, product = product(), vendor = vendor(vendorId), price = BigDecimal("40.00"), isActive = true)
        `when`(productVendorRepository.findById(linkId)).thenReturn(Optional.of(link))
        `when`(productVendorRepository.save(anyNonNull<ProductVendor>())).thenAnswer { it.getArgument<ProductVendor>(0) }

        authenticate(otherVendorId, Role.VENDOR)
        assertThrows(ForbidenException::class.java) { productVendorService.deactivateProductVendor(linkId) }

        authenticate(adminId, Role.ADMIN)
        assertThrows(ConflictException::class.java) { productVendorService.restoreProductVendor(linkId) }

        val deactivated = productVendorService.deactivateProductVendor(linkId)
        assertFalse(deactivated.isActive)

        assertThrows(ConflictException::class.java) { productVendorService.deactivateProductVendor(linkId) }

        val restored = productVendorService.restoreProductVendor(linkId)
        assertTrue(restored.isActive)
    }

    @Test
    fun `consultas lancam ResourceNotFoundException quando recurso nao existe`() {
        val missingId = UUID.randomUUID()
        `when`(userRepository.findById(missingId)).thenReturn(Optional.empty())
        `when`(productRepository.findById(missingId)).thenReturn(Optional.empty())
        `when`(productVendorRepository.findById(missingId)).thenReturn(Optional.empty())

        assertThrows(ResourceNotFoundException::class.java) { productVendorService.getProductsByVendorId(missingId) }
        assertThrows(ResourceNotFoundException::class.java) { productVendorService.getProductsByProductId(missingId) }
        assertThrows(ResourceNotFoundException::class.java) { productVendorService.getProductVendorById(missingId) }
    }
}
