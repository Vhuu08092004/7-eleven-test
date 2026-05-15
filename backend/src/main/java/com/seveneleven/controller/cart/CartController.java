package com.seveneleven.controller.cart;

import com.seveneleven.dto.BaseResponse;
import com.seveneleven.dto.cart.CartRequest;
import com.seveneleven.dto.cart.CartResponse;
import com.seveneleven.service.cart.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping Cart APIs")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get my cart", description = "Get current user's shopping cart")
    public ResponseEntity<BaseResponse<CartResponse>> getMyCart() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        CartResponse response = cartService.getMyCart(email);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Add to cart", description = "Add a product to cart (with pessimistic lock on product)")
    public ResponseEntity<BaseResponse<CartResponse>> addToCart(@Valid @RequestBody CartRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        CartResponse response = cartService.addToCart(email, request);
        return ResponseEntity.ok(BaseResponse.success("Item added to cart", response));
    }

    @PutMapping("/{cartItemId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Update cart item", description = "Update quantity of a cart item")
    public ResponseEntity<BaseResponse<CartResponse>> updateCartItem(
            @PathVariable Long cartItemId,
            @Valid @RequestBody CartRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        CartResponse response = cartService.updateCartItem(email, cartItemId, request);
        return ResponseEntity.ok(BaseResponse.success("Cart item updated", response));
    }

    @DeleteMapping("/{cartItemId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Remove from cart", description = "Remove an item from cart")
    public ResponseEntity<BaseResponse<CartResponse>> removeFromCart(@PathVariable Long cartItemId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        CartResponse response = cartService.removeFromCart(email, cartItemId);
        return ResponseEntity.ok(BaseResponse.success("Item removed from cart", response));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Clear cart", description = "Remove all items from cart")
    public ResponseEntity<BaseResponse<CartResponse>> clearCart() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        CartResponse response = cartService.clearCart(email);
        return ResponseEntity.ok(BaseResponse.success("Cart cleared", response));
    }
}
