package com.seveneleven.controller.order;

import com.seveneleven.dto.BaseResponse;
import com.seveneleven.dto.PageResponse;
import com.seveneleven.dto.order.OrderRequest;
import com.seveneleven.dto.order.OrderResponse;
import com.seveneleven.entity.Order;
import com.seveneleven.service.order.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order Management APIs")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Create order", description = "Create a new order (User only)")
    public ResponseEntity<BaseResponse<OrderResponse>> createOrder(@Valid @RequestBody OrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success("Order created successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all orders", description = "Get paginated list of orders (Admin only)")
    public ResponseEntity<BaseResponse<PageResponse<OrderResponse>>> getOrders(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        PageResponse<OrderResponse> response = orderService.getOrders(page, size);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get order by ID", description = "Get a single order by its ID (Admin only)")
    public ResponseEntity<BaseResponse<OrderResponse>> getOrderById(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        OrderResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update order status", description = "Update order status - CONFIRMED, SHIPPING, DELIVERED, CANCELLED (Admin only)")
    public ResponseEntity<BaseResponse<OrderResponse>> updateOrderStatus(
            @Parameter(description = "Order ID") @PathVariable Long id,
            @Parameter(description = "New status: CONFIRMED, SHIPPING, DELIVERED, CANCELLED") @RequestParam Order.OrderStatus status) {
        OrderResponse response = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(BaseResponse.success("Order status updated to " + status, response));
    }

    @GetMapping("/my-orders")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get my orders", description = "Get current user's order history")
    public ResponseEntity<BaseResponse<PageResponse<OrderResponse>>> getMyOrders(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        PageResponse<OrderResponse> response = orderService.getMyOrders(email, page, size);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @GetMapping("/my-orders/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get my order by ID", description = "Get current user's order detail by ID")
    public ResponseEntity<BaseResponse<OrderResponse>> getMyOrderById(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        OrderResponse response = orderService.getMyOrderById(email, id);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @PostMapping("/from-cart")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Place order from cart", description = "Create order from user's cart (with pessimistic lock on products)")
    public ResponseEntity<BaseResponse<OrderResponse>> placeOrderFromCart() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        OrderResponse response = orderService.placeOrderFromCart(email);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success("Order placed successfully", response));
    }
}
