package com.seveneleven.dto.cart;

import com.seveneleven.entity.Cart;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private BigDecimal productPrice;
    private Integer quantity;
    private BigDecimal subtotal;

    public static CartItemResponse fromEntity(Cart cart) {
        return CartItemResponse.builder()
                .id(cart.getId())
                .productId(cart.getProduct().getId())
                .productName(cart.getProduct().getName())
                .productImageUrl(cart.getProduct().getImageUrl())
                .productPrice(cart.getProduct().getPrice())
                .quantity(cart.getQuantity())
                .subtotal(cart.getProduct().getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())))
                .build();
    }
}
