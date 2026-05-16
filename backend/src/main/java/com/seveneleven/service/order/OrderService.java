package com.seveneleven.service.order;

import com.seveneleven.dto.PageResponse;
import com.seveneleven.dto.order.OrderItemResponse;
import com.seveneleven.dto.order.OrderRequest;
import com.seveneleven.dto.order.OrderResponse;
import com.seveneleven.entity.Cart;
import com.seveneleven.entity.Order;
import com.seveneleven.entity.OrderItem;
import com.seveneleven.entity.Product;
import com.seveneleven.entity.User;
import com.seveneleven.exception.BadRequestException;
import com.seveneleven.exception.ResourceNotFoundException;
import com.seveneleven.repository.CartRepository;
import com.seveneleven.repository.OrderItemRepository;
import com.seveneleven.repository.OrderRepository;
import com.seveneleven.repository.ProductRepository;
import com.seveneleven.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CacheManager cacheManager;

    /**
     * Places an order directly (without using cart).
     * Uses pessimistic lock on products to prevent overselling when multiple users
     * try to order the same product simultaneously.
     */
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Order must have at least one item");
        }

        // Sort product IDs to prevent deadlock
        List<Long> sortedProductIds = request.getItems().stream()
                .map(OrderRequest.OrderItemRequest::getProductId)
                .distinct()
                .sorted()
                .toList();

        // Lock all products in sorted order
        Map<Long, Product> lockedProducts = sortedProductIds.stream()
                .collect(Collectors.toMap(
                        pid -> pid,
                        pid -> productRepository.findByIdWithLock(pid)
                                .orElseThrow(() -> new ResourceNotFoundException("Product", pid))
                ));

        // Verify stock for all items
        for (OrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Product product = lockedProducts.get(itemRequest.getProductId());
            if (product.getStock() < itemRequest.getQuantity()) {
                throw new BadRequestException(
                        String.format("Insufficient stock for product '%s'. Available: %d, Requested: %d",
                                product.getName(), product.getStock(), itemRequest.getQuantity()));
            }
        }

        // Create order
        Order order = Order.builder()
                .user(user)
                .totalPrice(BigDecimal.ZERO)
                .status(Order.OrderStatus.PENDING)
                .build();
        Order savedOrder = orderRepository.save(order);

        BigDecimal totalPrice = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Product product = lockedProducts.get(itemRequest.getProductId());

            // Deduct stock
            product.setStock(product.getStock() - itemRequest.getQuantity());
            productRepository.save(product);
            evictProductCache(itemRequest.getProductId());

            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .price(product.getPrice())
                    .build();

            orderItems.add(orderItemRepository.save(orderItem));
            totalPrice = totalPrice.add(product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
        }

        savedOrder.setTotalPrice(totalPrice);
        Order finalOrder = orderRepository.save(savedOrder);

        log.info("Order created: {} with {} items, total: {}", finalOrder.getId(), orderItems.size(), totalPrice);

        List<OrderItemResponse> itemResponses = orderItems.stream()
                .map(OrderItemResponse::fromEntity)
                .collect(Collectors.toList());

        return OrderResponse.fromEntity(finalOrder, itemResponses);
    }

    /**
     * Places an order from the user's cart.
     * Uses pessimistic lock on products to prevent overselling when multiple users
     * try to order the same product simultaneously.
     */
    @Transactional
    public OrderResponse placeOrderFromCart(String email) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        List<Cart> cartItems = cartRepository.findByUserIdWithProduct(user.getId());
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        // Sort product IDs to prevent deadlock
        List<Long> sortedProductIds = cartItems.stream()
                .map(cart -> cart.getProduct().getId())
                .distinct()
                .sorted()
                .toList();

        // Lock all products in sorted order
        Map<Long, Product> lockedProducts = sortedProductIds.stream()
                .collect(Collectors.toMap(
                        pid -> pid,
                        pid -> productRepository.findByIdWithLock(pid)
                                .orElseThrow(() -> new ResourceNotFoundException("Product", pid))
                ));

        // Verify stock for all cart items
        for (Cart cartItem : cartItems) {
            Product product = lockedProducts.get(cartItem.getProduct().getId());
            if (product.getStock() < cartItem.getQuantity()) {
                throw new BadRequestException(
                        String.format("Insufficient stock for product '%s'. Available: %d, In cart: %d",
                                product.getName(), product.getStock(), cartItem.getQuantity()));
            }
        }

        // Create order
        Order order = Order.builder()
                .user(user)
                .totalPrice(BigDecimal.ZERO)
                .status(Order.OrderStatus.PENDING)
                .build();
        Order savedOrder = orderRepository.save(order);

        BigDecimal totalPrice = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (Cart cartItem : cartItems) {
            Product product = lockedProducts.get(cartItem.getProduct().getId());

            // Deduct stock
            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);
            evictProductCache(cartItem.getProduct().getId());

            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .price(product.getPrice())
                    .build();

            orderItems.add(orderItemRepository.save(orderItem));
            totalPrice = totalPrice.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));

            // Hard delete cart item
            cartRepository.delete(cartItem);
        }

        savedOrder.setTotalPrice(totalPrice);
        Order finalOrder = orderRepository.save(savedOrder);

        log.info("Order created from cart: {} with {} items, total: {}", finalOrder.getId(), orderItems.size(), totalPrice);

        List<OrderItemResponse> itemResponses = orderItems.stream()
                .map(OrderItemResponse::fromEntity)
                .collect(Collectors.toList());

        return OrderResponse.fromEntity(finalOrder, itemResponses);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orderPage = orderRepository.findByIsDeletedFalseOrderByCreatedAtDesc(pageable);

        List<OrderResponse> content = orderPage.getContent().stream()
                .map(order -> {
                    List<OrderItem> items = orderItemRepository.findByOrderIdAndIsDeletedFalse(order.getId());
                    List<OrderItemResponse> itemResponses = items.stream()
                            .map(OrderItemResponse::fromEntity)
                            .collect(Collectors.toList());
                    return OrderResponse.fromEntity(order, itemResponses);
                })
                .collect(Collectors.toList());

        return PageResponse.<OrderResponse>builder()
                .content(content)
                .page(orderPage.getNumber())
                .size(orderPage.getSize())
                .totalElements(orderPage.getTotalElements())
                .totalPages(orderPage.getTotalPages())
                .first(orderPage.isFirst())
                .last(orderPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));

        List<OrderItemResponse> itemResponses = orderItemRepository.findByOrderIdAndIsDeletedFalse(id).stream()
                .map(OrderItemResponse::fromEntity)
                .collect(Collectors.toList());

        return OrderResponse.fromEntity(order, itemResponses);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, Order.OrderStatus newStatus) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));

        Order.OrderStatus currentStatus = order.getStatus();

        if (currentStatus == Order.OrderStatus.DELIVERED || currentStatus == Order.OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot change status of a " + currentStatus + " order");
        }

        if (newStatus == Order.OrderStatus.CANCELLED) {
            // Sort product IDs before locking to prevent deadlock
            List<Long> sortedProductIds = order.getOrderItems().stream()
                    .map(item -> item.getProduct().getId())
                    .distinct()
                    .sorted()
                    .toList();

            Map<Long, Product> lockedProducts = sortedProductIds.stream()
                    .collect(Collectors.toMap(
                            pid -> pid,
                            pid -> productRepository.findByIdWithLock(pid)
                                    .orElseThrow(() -> new ResourceNotFoundException("Product", pid))
                    ));

            for (OrderItem item : order.getOrderItems()) {
                Product product = lockedProducts.get(item.getProduct().getId());
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.save(product);
                evictProductCache(item.getProduct().getId());
            }
            log.info("Order {} cancelled, stock restored for {} items", id, order.getOrderItems().size());
        }

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(OrderItemResponse::fromEntity)
                .collect(Collectors.toList());

        log.info("Order {} status changed from {} to {}", id, currentStatus, newStatus);

        return OrderResponse.fromEntity(updatedOrder, itemResponses);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getMyOrders(String email, int page, int size) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orderPage = orderRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(user.getId(), pageable);

        List<OrderResponse> content = orderPage.getContent().stream()
                .map(order -> {
                    List<OrderItem> items = orderItemRepository.findByOrderIdAndIsDeletedFalse(order.getId());
                    List<OrderItemResponse> itemResponses = items.stream()
                            .map(OrderItemResponse::fromEntity)
                            .collect(Collectors.toList());
                    return OrderResponse.fromEntity(order, itemResponses);
                })
                .collect(Collectors.toList());

        return PageResponse.<OrderResponse>builder()
                .content(content)
                .page(orderPage.getNumber())
                .size(orderPage.getSize())
                .totalElements(orderPage.getTotalElements())
                .totalPages(orderPage.getTotalPages())
                .first(orderPage.isFirst())
                .last(orderPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public OrderResponse getMyOrderById(String email, Long id) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        Order order = orderRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Order", id);
        }

        List<OrderItemResponse> itemResponses = orderItemRepository.findByOrderIdAndIsDeletedFalse(id).stream()
                .map(OrderItemResponse::fromEntity)
                .collect(Collectors.toList());

        return OrderResponse.fromEntity(order, itemResponses);
    }

    private void evictProductCache(Long productId) {
        var productsCache = cacheManager.getCache("products");
        if (productsCache != null) {
            productsCache.clear();
        }
        var productCache = cacheManager.getCache("product");
        if (productCache != null) {
            productCache.evict(productId);
        }
    }
}
