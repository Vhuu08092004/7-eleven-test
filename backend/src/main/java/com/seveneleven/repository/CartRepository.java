package com.seveneleven.repository;

import com.seveneleven.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    @Query("SELECT c FROM Cart c JOIN FETCH c.product p WHERE c.user.id = :userId AND c.isDeleted = false AND p.isDeleted = false")
    List<Cart> findByUserIdWithProduct(@Param("userId") Long userId);

    Optional<Cart> findByUserIdAndProductIdAndIsDeletedFalse(Long userId, Long productId);

    @Query("SELECT c FROM Cart c JOIN FETCH c.product p WHERE c.user.id = :userId AND c.product.id = :productId AND c.isDeleted = false")
    Optional<Cart> findByUserIdAndProductIdWithProduct(@Param("userId") Long userId, @Param("productId") Long productId);
}
