package com.seveneleven.dto.product;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;

    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stock;

    /**
     * Final image URL for products with permanent images.
     * If this starts with "/uploads/temp/" or matches a temp file key,
     * the file will be promoted from temp storage to permanent storage.
     */
    private String imageUrl;

    /**
     * Optional: temp file key for newly uploaded images.
     * If provided, the corresponding temp file will be promoted to permanent storage
     * and its permanent URL will be used as imageUrl.
     */
    private String imageFileKey;
}
