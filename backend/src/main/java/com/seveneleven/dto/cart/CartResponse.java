package com.seveneleven.dto.cart;

import com.seveneleven.entity.Cart;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {
    private List<CartItemResponse> items;
    private int totalItems;
    private BigDecimal totalPrice;

    public static CartResponse fromEntities(List<Cart> carts) {
        List<CartItemResponse> items = carts.stream()
                .map(CartItemResponse::fromEntity)
                .collect(Collectors.toList());

        BigDecimal totalPrice = items.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = items.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();

        return CartResponse.builder()
                .items(items)
                .totalItems(totalItems)
                .totalPrice(totalPrice)
                .build();
    }
}
