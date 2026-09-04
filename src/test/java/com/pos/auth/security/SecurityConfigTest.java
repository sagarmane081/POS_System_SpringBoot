package com.pos.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.analytics.service.AnalyticsService;
import com.pos.auth.dto.RegisterRequest;
import com.pos.auth.service.AuthService;
import com.pos.category.controller.CategoryController;
import com.pos.category.dto.CategoryRequest;
import com.pos.category.service.CategoryService;
import com.pos.order.controller.OrderController;
import com.pos.order.dto.CreateOrderRequest;
import com.pos.order.dto.OrderItemRequest;
import com.pos.order.service.OrderService;
import com.pos.payment.controller.PaymentController;
import com.pos.payment.dto.PaymentRequest;
import com.pos.payment.enums.PaymentMethod;
import com.pos.payment.service.PaymentService;
import com.pos.product.controller.ProductController;
import com.pos.product.controller.SeedController;
import com.pos.product.dto.ProductRequest;
import com.pos.product.service.ProductSeedService;
import com.pos.product.service.ProductService;
import com.pos.analytics.controller.AnalyticsController;
import com.pos.auth.controller.AuthController;
import com.pos.auth.controller.UserController;
import com.pos.auth.dto.CreateUserRequest;
import com.pos.auth.enums.Role;
import com.pos.auth.service.RefreshTokenService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AuthController.class,
        UserController.class,
        CategoryController.class,
        OrderController.class,
        PaymentController.class,
        AnalyticsController.class,
        ProductController.class,
        SeedController.class
})
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private AuthService authService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private ProductService productService;

    @MockBean
    private ProductSeedService productSeedService;

    @MockBean
    private OrderService orderService;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private AnalyticsService analyticsService;

    private String orderJson() throws Exception {

        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(1);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(List.of(item));

        return objectMapper.writeValueAsString(request);
    }

    private String paymentJson() throws Exception {

        return objectMapper.writeValueAsString(
                PaymentRequest.builder().orderId(1L).method(PaymentMethod.CARD).build()
        );
    }

    private String productJson() throws Exception {

        return objectMapper.writeValueAsString(
                ProductRequest.builder()
                        .name("Coke")
                        .sku("SKU-1")
                        .mrp(BigDecimal.TEN)
                        .sellingPrice(BigDecimal.valueOf(9))
                        .stock(10)
                        .categoryId(1L)
                        .build()
        );
    }

    private String categoryJson() throws Exception {

        return objectMapper.writeValueAsString(
                CategoryRequest.builder().name("Beverages").build()
        );
    }

    private String createUserJson() throws Exception {

        CreateUserRequest request = new CreateUserRequest();
        request.setName("Cashier One");
        request.setEmail("cashier@example.com");
        request.setPassword("password123");
        request.setRole(Role.ROLE_CASHIER);

        return objectMapper.writeValueAsString(request);
    }

    @Test
    void registerEndpoint_shouldBeAccessible_withoutAuthentication() throws Exception {

        RegisterRequest request = new RegisterRequest();
        request.setName("John");
        request.setEmail("john@example.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void getProducts_shouldBeForbidden_whenUnauthenticated() throws Exception {

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getProducts_shouldBeAllowed_forAdmin() throws Exception {

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CASHIER")
    void getProducts_shouldBeAllowed_forCashier() throws Exception {

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getProducts_shouldBeForbidden_forPlainUser() throws Exception {

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduct_shouldBeAllowed_forAdmin() throws Exception {

        mockMvc.perform(post("/api/products")
                        .contentType("application/json")
                        .content(productJson()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CASHIER")
    void createProduct_shouldBeForbidden_forCashier() throws Exception {

        mockMvc.perform(post("/api/products")
                        .contentType("application/json")
                        .content(productJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void seedProducts_shouldBeAllowed_forAdmin() throws Exception {

        mockMvc.perform(post("/api/seed/products"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CASHIER")
    void seedProducts_shouldBeForbidden_forCashier() throws Exception {

        mockMvc.perform(post("/api/seed/products"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createOrder_shouldBeAllowed_forAdmin() throws Exception {

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(orderJson()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CASHIER")
    void createOrder_shouldBeAllowed_forCashier() throws Exception {

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(orderJson()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createOrder_shouldBeForbidden_forPlainUser() throws Exception {

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(orderJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createPayment_shouldBeAllowed_forAdmin() throws Exception {

        mockMvc.perform(post("/api/payments")
                        .contentType("application/json")
                        .content(paymentJson()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CASHIER")
    void createPayment_shouldBeAllowed_forCashier() throws Exception {

        mockMvc.perform(post("/api/payments")
                        .contentType("application/json")
                        .content(paymentJson()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createPayment_shouldBeForbidden_forPlainUser() throws Exception {

        mockMvc.perform(post("/api/payments")
                        .contentType("application/json")
                        .content(paymentJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getCategories_shouldBeAllowed_forAdmin() throws Exception {

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CASHIER")
    void getCategories_shouldBeAllowed_forCashier() throws Exception {

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getCategories_shouldBeForbidden_forPlainUser() throws Exception {

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCategory_shouldBeAllowed_forAdmin() throws Exception {

        mockMvc.perform(post("/api/categories")
                        .contentType("application/json")
                        .content(categoryJson()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CASHIER")
    void createCategory_shouldBeForbidden_forCashier() throws Exception {

        mockMvc.perform(post("/api/categories")
                        .contentType("application/json")
                        .content(categoryJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getDashboard_shouldBeAllowed_forAdmin() throws Exception {

        mockMvc.perform(get("/api/analytics/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CASHIER")
    void getDashboard_shouldBeForbidden_forCashier() throws Exception {

        mockMvc.perform(get("/api/analytics/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createUser_shouldBeForbidden_whenUnauthenticated() throws Exception {

        mockMvc.perform(post("/api/users")
                        .contentType("application/json")
                        .content(createUserJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_shouldBeAllowed_forAdmin() throws Exception {

        mockMvc.perform(post("/api/users")
                        .contentType("application/json")
                        .content(createUserJson()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CASHIER")
    void createUser_shouldBeForbidden_forCashier() throws Exception {

        mockMvc.perform(post("/api/users")
                        .contentType("application/json")
                        .content(createUserJson()))
                .andExpect(status().isForbidden());
    }
}
