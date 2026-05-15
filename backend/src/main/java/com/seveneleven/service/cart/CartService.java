package com.seveneleven.service.cart;

import com.seveneleven.dto.cart.CartRequest;
import com.seveneleven.dto.cart.CartResponse;
import com.seveneleven.entity.Cart;
import com.seveneleven.entity.Product;
import com.seveneleven.entity.User;
import com.seveneleven.exception.BadRequestException;
import com.seveneleven.exception.ResourceNotFoundException;
import com.seveneleven.repository.CartRepository;
import com.seveneleven.repository.ProductRepository;
import com.seveneleven.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public CartResponse getMyCart(String email) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        List<Cart> carts = cartRepository.findByUserIdWithProduct(user.getId());
        return CartResponse.fromEntities(carts);
    }

    @Transactional
    public CartResponse addToCart(String email, CartRequest request) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        // No lock needed here - just add to cart
        Product product = productRepository.findByIdAndIsDeletedFalse(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.getProductId()));

        // Check if item already in cart
        var existingCart = cartRepository.findByUserIdAndProductIdWithProduct(user.getId(), product.getId());

        if (existingCart.isPresent()) {
            Cart cart = existingCart.get();
            int newQuantity = cart.getQuantity() + request.getQuantity();

            // Note: we don't check stock here, only at place order time
            cart.setQuantity(newQuantity);
            cartRepository.save(cart);
            log.info("User {} updated cart: product {} quantity to {}", email, product.getId(), newQuantity);
        } else {
            Cart newCart = Cart.builder()
                    .user(user)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cartRepository.save(newCart);
            log.info("User {} added to cart: product {} x{}", email, product.getId(), request.getQuantity());
        }

        return getMyCart(email);
    }

    @Transactional
    public CartResponse updateCartItem(String email, Long cartItemId, CartRequest request) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        Cart cart = cartRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", cartItemId));

        if (!cart.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Cart item", cartItemId);
        }

        if (request.getQuantity() <= 0) {
            cart.setIsDeleted(true);
            cartRepository.save(cart);
            log.info("User {} removed cart item {} (quantity set to 0)", email, cartItemId);
        } else {
            cart.setQuantity(request.getQuantity());
            cartRepository.save(cart);
            log.info("User {} updated cart item {}: quantity to {}", email, cartItemId, request.getQuantity());
        }

        return getMyCart(email);
    }

    @Transactional
    public CartResponse removeFromCart(String email, Long cartItemId) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        Cart cart = cartRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", cartItemId));

        if (!cart.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Cart item", cartItemId);
        }

        cart.setIsDeleted(true);
        cartRepository.save(cart);
        log.info("User {} removed cart item {}", email, cartItemId);

        return getMyCart(email);
    }

    @Transactional
    public CartResponse clearCart(String email) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        List<Cart> carts = cartRepository.findByUserIdWithProduct(user.getId());

        for (Cart cart : carts) {
            cart.setIsDeleted(true);
            cartRepository.save(cart);
        }

        log.info("User {} cleared cart ({} items)", email, carts.size());
        return CartResponse.builder().items(List.of()).totalItems(0).totalPrice(java.math.BigDecimal.ZERO).build();
    }

    @Transactional(readOnly = true)
    public List<Cart> getCartsByUserEmail(String email) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new BadRequestException("User not found"));
        return cartRepository.findByUserIdWithProduct(user.getId());
    }
}
