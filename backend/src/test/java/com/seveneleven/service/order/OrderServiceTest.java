package com.seveneleven.service.order;

import com.seveneleven.dto.order.OrderItemResponse;
import com.seveneleven.dto.order.OrderRequest;
import com.seveneleven.dto.order.OrderResponse;
import com.seveneleven.entity.Order;
import com.seveneleven.entity.OrderItem;
import com.seveneleven.entity.Product;
import com.seveneleven.entity.User;
import com.seveneleven.exception.BadRequestException;
import com.seveneleven.exception.ResourceNotFoundException;
import com.seveneleven.repository.OrderItemRepository;
import com.seveneleven.repository.OrderRepository;
import com.seveneleven.repository.ProductRepository;
import com.seveneleven.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderService orderService;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("user@7eleven.com")
                .password("password")
                .role(User.Role.ROLE_USER)
                .isDeleted(false)
                .build();

        testProduct = Product.builder()
                .id(1L)
                .name("Coca Cola")
                .description("330ml soft drink")
                .price(BigDecimal.valueOf(15000))
                .stock(100)
                .imageUrl("https://example.com/coca-cola.jpg")
                .isDeleted(false)
                .createdAt(Instant.now())
                .build();

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("user@7eleven.com");
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void createOrder_Success() {
        OrderRequest.OrderItemRequest itemRequest = new OrderRequest.OrderItemRequest(1L, 2);
        OrderRequest request = new OrderRequest(Arrays.asList(itemRequest));

        when(userRepository.findByEmailAndIsDeletedFalse("user@7eleven.com"))
                .thenReturn(java.util.Optional.of(testUser));
        when(productRepository.findAllById(any())).thenReturn(Arrays.asList(testProduct));

        Order savedOrder = Order.builder()
                .id(1L)
                .user(testUser)
                .totalPrice(BigDecimal.valueOf(30000))
                .status(Order.OrderStatus.PENDING)
                .isDeleted(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderItem savedItem = OrderItem.builder()
                .id(1L)
                .order(savedOrder)
                .product(testProduct)
                .quantity(2)
                .price(BigDecimal.valueOf(15000))
                .isDeleted(false)
                .createdAt(Instant.now())
                .build();

        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(savedItem);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        when(orderItemRepository.findByOrderIdAndIsDeletedFalse(1L))
                .thenReturn(Arrays.asList(savedItem));

        OrderResponse response = orderService.createOrder(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(BigDecimal.valueOf(30000), response.getTotalPrice());
        assertEquals("PENDING", response.getStatus());
        assertEquals(1, response.getItems().size());
        
        verify(orderRepository, times(2)).save(any(Order.class));
        verify(orderItemRepository).save(any(OrderItem.class));
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createOrder_InsufficientStock() {
        testProduct.setStock(1);
        
        OrderRequest.OrderItemRequest itemRequest = new OrderRequest.OrderItemRequest(1L, 5);
        OrderRequest request = new OrderRequest(Arrays.asList(itemRequest));

        when(userRepository.findByEmailAndIsDeletedFalse("user@7eleven.com"))
                .thenReturn(java.util.Optional.of(testUser));
        when(productRepository.findAllById(any())).thenReturn(Arrays.asList(testProduct));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> orderService.createOrder(request));
        assertTrue(exception.getMessage().contains("Insufficient stock"));
    }

    @Test
    void createOrder_ProductNotFound() {
        OrderRequest.OrderItemRequest itemRequest = new OrderRequest.OrderItemRequest(999L, 1);
        OrderRequest request = new OrderRequest(Arrays.asList(itemRequest));

        when(userRepository.findByEmailAndIsDeletedFalse("user@7eleven.com"))
                .thenReturn(java.util.Optional.of(testUser));
        when(productRepository.findAllById(any())).thenReturn(Arrays.asList());

        assertThrows(ResourceNotFoundException.class, () -> orderService.createOrder(request));
    }
}
