package com.seveneleven.service.product;

import com.seveneleven.dto.PageResponse;
import com.seveneleven.dto.product.ProductPublicResponse;
import com.seveneleven.dto.product.ProductRequest;
import com.seveneleven.dto.product.ProductResponse;
import com.seveneleven.entity.Product;
import com.seveneleven.exception.ResourceNotFoundException;
import com.seveneleven.repository.ProductRepository;
import com.seveneleven.service.tempfile.TempFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final TempFileService tempFileService;

    @Cacheable(value = "products", key = "#page + '-' + #size + '-' + #keyword")
    @Transactional(readOnly = true)
    public PageResponse<ProductPublicResponse> getProductsPublic(int page, int size, String keyword) {
        log.debug("Fetching public products - page: {}, size: {}, keyword: {}", page, size, keyword);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Product> productPage;

        if (keyword != null && !keyword.isBlank()) {
            productPage = productRepository.searchByKeyword(keyword.trim(), pageable);
        } else {
            productPage = productRepository.findByIsDeletedFalse(pageable);
        }

        List<ProductPublicResponse> content = productPage.getContent().stream()
                .map(ProductPublicResponse::fromEntity)
                .collect(Collectors.toList());

        return PageResponse.<ProductPublicResponse>builder()
                .content(content)
                .page(productPage.getNumber())
                .size(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .first(productPage.isFirst())
                .last(productPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public ProductPublicResponse getProductByIdPublic(Long id) {
        Product product = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        return ProductPublicResponse.fromEntity(product);
    }

    @Cacheable(value = "products", key = "#page + '-' + #size + '-' + #keyword")
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProducts(int page, int size, String keyword) {
        log.debug("Fetching products - page: {}, size: {}, keyword: {}", page, size, keyword);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Product> productPage;

        if (keyword != null && !keyword.isBlank()) {
            productPage = productRepository.searchByKeyword(keyword.trim(), pageable);
        } else {
            productPage = productRepository.findByIsDeletedFalse(pageable);
        }

        List<ProductResponse> content = productPage.getContent().stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());

        return PageResponse.<ProductResponse>builder()
                .content(content)
                .page(productPage.getNumber())
                .size(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .first(productPage.isFirst())
                .last(productPage.isLast())
                .build();
    }

    @Cacheable(value = "product", key = "#id")
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        return ProductResponse.fromEntity(product);
    }

    @CacheEvict(value = {"products", "product"}, allEntries = true)
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        String imageUrl = resolveImageUrl(request);

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .imageUrl(imageUrl)
                .build();

        Product saved = productRepository.save(product);
        log.info("Product created: {}", saved.getId());
        return ProductResponse.fromEntity(saved);
    }

    @CacheEvict(value = {"products", "product"}, allEntries = true)
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        String imageUrl = resolveImageUrl(request);

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setImageUrl(imageUrl);

        Product saved = productRepository.save(product);
        log.info("Product updated: {}", saved.getId());
        return ProductResponse.fromEntity(saved);
    }

    /**
     * Resolve the final imageUrl for a product. If the request contains a temp file key,
     * the file is promoted from temp storage to permanent storage. Otherwise, the existing
     * imageUrl from the request is used as-is (e.g., for URLs pointing to existing files).
     */
    private String resolveImageUrl(ProductRequest request) {
        String imageUrl = request.getImageUrl();
        String fileKey = request.getImageFileKey();

        if (fileKey != null && !fileKey.isBlank()) {
            String extension = getExtensionFromFilename(imageUrl);
            String finalFilename = UUID.randomUUID().toString() + extension;
            return tempFileService.promoteTempFile(fileKey, "uploads/products", finalFilename);
        }

        return imageUrl;
    }

    private String getExtensionFromFilename(String filename) {
        if (filename == null || filename.isBlank()) return "";
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1) return "";
        return filename.substring(dotIndex);
    }

    @CacheEvict(value = {"products", "product"}, allEntries = true)
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        product.setIsDeleted(true);
        productRepository.save(product);
        log.info("Product soft deleted: {}", id);
    }
}
