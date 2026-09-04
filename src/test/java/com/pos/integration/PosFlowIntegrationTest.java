package com.pos.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

import com.pos.auth.dto.CreateUserRequest;
import com.pos.auth.dto.LoginRequest;
import com.pos.auth.dto.RegisterRequest;
import com.pos.auth.entity.User;
import com.pos.auth.enums.Role;
import com.pos.auth.repository.UserRepository;
import com.pos.category.dto.CategoryRequest;
import com.pos.order.dto.CreateOrderRequest;
import com.pos.order.dto.OrderItemRequest;
import com.pos.payment.dto.PaymentRequest;
import com.pos.payment.enums.PaymentMethod;
import com.pos.product.dto.ProductRequest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class PosFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String createAdminAndLogin(String email, String rawPassword) throws Exception {

        userRepository.save(
                User.builder()
                        .name("Admin")
                        .email(email)
                        .password(passwordEncoder.encode(rawPassword))
                        .role(Role.ROLE_ADMIN)
                        .build()
        );

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(rawPassword);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(loginResult.getResponse().getContentAsString(), "$.data.token");
    }

    @Test
    void fullPurchaseFlow_shouldRegisterLoginCreateCatalogOrderAndPay() throws Exception {

        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("Jane Customer");
        registerRequest.setEmail("jane-" + uniqueSuffix + "@example.com");
        registerRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty());

        String adminToken = createAdminAndLogin("admin-" + uniqueSuffix + "@example.com", "adminPass123");

        CategoryRequest categoryRequest = CategoryRequest.builder()
                .name("Beverages-" + uniqueSuffix)
                .description("Drinks")
                .build();

        MvcResult categoryResult = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(categoryRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Long categoryId = ((Number) JsonPath.read(
                categoryResult.getResponse().getContentAsString(), "$.data.id"
        )).longValue();

        ProductRequest productRequest = ProductRequest.builder()
                .name("Cola")
                .sku("SKU-" + uniqueSuffix)
                .mrp(BigDecimal.valueOf(15))
                .sellingPrice(BigDecimal.valueOf(10))
                .stock(20)
                .categoryId(categoryId)
                .build();

        MvcResult productResult = mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stock").value(20))
                .andReturn();

        Long productId = ((Number) JsonPath.read(
                productResult.getResponse().getContentAsString(), "$.data.id"
        )).longValue();

        OrderItemRequest orderItem = new OrderItemRequest();
        orderItem.setProductId(productId);
        orderItem.setQuantity(3);

        CreateOrderRequest orderRequest = new CreateOrderRequest();
        orderRequest.setItems(List.of(orderItem));

        MvcResult orderResult = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(30))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andReturn();

        Long orderId = ((Number) JsonPath.read(
                orderResult.getResponse().getContentAsString(), "$.data.id"
        )).longValue();

        mockMvc.perform(get("/api/products/{id}", productId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stock").value(17));

        PaymentRequest paymentRequest = PaymentRequest.builder()
                .orderId(orderId)
                .method(PaymentMethod.CARD)
                .build();

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.amount").value(30));

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/analytics/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalOrders").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void protectedEndpoint_shouldReturn403_withoutToken() throws Exception {

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpoint_shouldReturn403_forAuthenticatedUserWithoutRequiredRole() throws Exception {

        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "plainuser-" + uniqueSuffix + "@example.com";

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("Plain User");
        registerRequest.setEmail(email);
        registerRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String token = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.data.token");

        mockMvc.perform(get("/api/products").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void register_shouldReturn409EndToEnd_whenEmailAlreadyRegistered() throws Exception {

        String email = "duplicate-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("First");
        registerRequest.setEmail(email);
        registerRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists"));

        assertThat(userRepository.existsByEmail(email)).isTrue();
    }

    @Test
    void createOrder_shouldReturn409EndToEnd_whenStockInsufficient() throws Exception {

        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String adminToken = createAdminAndLogin("admin2-" + uniqueSuffix + "@example.com", "adminPass123");

        CategoryRequest categoryRequest = CategoryRequest.builder()
                .name("Snacks-" + uniqueSuffix)
                .build();

        MvcResult categoryResult = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(categoryRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Long categoryId = ((Number) JsonPath.read(
                categoryResult.getResponse().getContentAsString(), "$.data.id"
        )).longValue();

        ProductRequest productRequest = ProductRequest.builder()
                .name("Chips")
                .sku("SKU-LOW-" + uniqueSuffix)
                .mrp(BigDecimal.valueOf(5))
                .sellingPrice(BigDecimal.valueOf(4))
                .stock(2)
                .categoryId(categoryId)
                .build();

        MvcResult productResult = mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Long productId = ((Number) JsonPath.read(
                productResult.getResponse().getContentAsString(), "$.data.id"
        )).longValue();

        OrderItemRequest orderItem = new OrderItemRequest();
        orderItem.setProductId(productId);
        orderItem.setQuantity(10);

        CreateOrderRequest orderRequest = new CreateOrderRequest();
        orderRequest.setItems(List.of(orderItem));

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/products/{id}", productId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stock").value(2));
    }

    @Test
    void adminCreatesCashier_shouldLoginAndAccessCashierEndpoints_butNotAdminOnlyEndpoints() throws Exception {

        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String adminToken = createAdminAndLogin("admin3-" + uniqueSuffix + "@example.com", "adminPass123");

        String cashierEmail = "cashier-" + uniqueSuffix + "@example.com";

        CreateUserRequest createUserRequest = new CreateUserRequest();
        createUserRequest.setName("New Cashier");
        createUserRequest.setEmail(cashierEmail);
        createUserRequest.setPassword("cashierPass123");
        createUserRequest.setRole(Role.ROLE_CASHIER);

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(cashierEmail))
                .andExpect(jsonPath("$.data.role").value("ROLE_CASHIER"));

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(cashierEmail);
        loginRequest.setPassword("cashierPass123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String cashierToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.data.token");

        mockMvc.perform(get("/api/products").header("Authorization", "Bearer " + cashierToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/analytics/dashboard").header("Authorization", "Bearer " + cashierToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + cashierToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createUser_shouldReturn409EndToEnd_whenEmailAlreadyRegistered() throws Exception {

        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String adminToken = createAdminAndLogin("admin4-" + uniqueSuffix + "@example.com", "adminPass123");

        String existingEmail = "existing-" + uniqueSuffix + "@example.com";

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("Existing User");
        registerRequest.setEmail(existingEmail);
        registerRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        CreateUserRequest createUserRequest = new CreateUserRequest();
        createUserRequest.setName("Duplicate");
        createUserRequest.setEmail(existingEmail);
        createUserRequest.setPassword("password123");
        createUserRequest.setRole(Role.ROLE_CASHIER);

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }
}
