package com.seveneleven.service.order;

import com.seveneleven.dto.PageResponse;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        List<Long> productIds = request.getItems().stream()
                .map(OrderRequest.OrderItemRequest::getProductId)
                .collect(Collectors.toList());

        Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        for (OrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Product product = productMap.get(itemRequest.getProductId());
            if (product == null) {
                throw new ResourceNotFoundException("Product", itemRequest.getProductId());
            }
            if (product.getStock() < itemRequest.getQuantity()) {
                throw new BadRequestException(
                        String.format("Insufficient stock for product '%s'. Available: %d, Requested: %d",
                                product.getName(), product.getStock(), itemRequest.getQuantity()));
            }
        }

        Order order = Order.builder()
                .user(user)
                .totalPrice(BigDecimal.ZERO)
                .status(Order.OrderStatus.PENDING)
                .build();

        Order savedOrder = orderRepository.save(order);

        BigDecimal totalPrice = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Product product = productMap.get(itemRequest.getProductId());

            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .price(product.getPrice())
                    .build();

            orderItems.add(orderItemRepository.save(orderItem));

            product.setStock(product.getStock() - itemRequest.getQuantity());
            productRepository.save(product);

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
}
