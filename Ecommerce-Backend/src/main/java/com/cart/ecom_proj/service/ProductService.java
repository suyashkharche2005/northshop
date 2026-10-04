package com.cart.ecom_proj.service;
import com.cart.ecom_proj.dto.*;
import com.cart.ecom_proj.exception.ApiException;
import com.cart.ecom_proj.model.Product;
import com.cart.ecom_proj.repo.ProductRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.Set;
@Service
public class ProductService {
 private final ProductRepo repo;
 public ProductService(ProductRepo repo){this.repo=repo;}
 @Transactional(readOnly=true) public List<ProductResponse> all(){return repo.findAll().stream().map(this::view).toList();}
 @Transactional(readOnly=true) public ProductResponse one(int id){return view(find(id));}
 @Transactional(readOnly=true) public List<ProductResponse> search(String keyword){return repo.searchProducts(keyword.trim()).stream().map(this::view).toList();}
 @Transactional(readOnly=true) public Product image(int id){return find(id);}
 @Transactional public ProductResponse create(ProductRequest request,MultipartFile file){Product p=new Product();apply(p,request);setImage(p,file);return view(repo.save(p));}
 @Transactional public ProductResponse update(int id,ProductRequest request,MultipartFile file){Product p=find(id);apply(p,request);if(file!=null&&!file.isEmpty())setImage(p,file);return view(p);}
 @Transactional public void delete(int id){repo.delete(find(id));}
 private Product find(int id){return repo.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Product not found"));}
 private void apply(Product p,ProductRequest r){p.setName(r.name().trim());p.setDescription(r.description().trim());p.setBrand(r.brand().trim());p.setPrice(r.price());p.setCategory(r.category().trim());p.setReleaseDate(r.releaseDate()==null?null:java.sql.Date.valueOf(r.releaseDate()));p.setStockQuantity(r.stockQuantity());p.setProductAvailable(r.productAvailable()&&r.stockQuantity()>0);}
 private void setImage(Product p,MultipartFile file){if(file==null||file.isEmpty())return;String type=file.getContentType();if(!Set.of("image/jpeg","image/png","image/webp").contains(type))throw new ApiException(HttpStatus.BAD_REQUEST,"Use a JPEG, PNG or WebP image");try{p.setImageDate(file.getBytes());p.setImageType(type);p.setImageName(file.getOriginalFilename());}catch(IOException e){throw new ApiException(HttpStatus.BAD_REQUEST,"Could not read image");}}
 private ProductResponse view(Product p){return new ProductResponse(p.getId(),p.getName(),p.getDescription(),p.getBrand(),p.getPrice(),p.getCategory(),p.getReleaseDate()==null?null:new java.sql.Date(p.getReleaseDate().getTime()).toLocalDate(),p.isProductAvailable(),p.getStockQuantity(),p.getImageName(),p.getImageDate()!=null);}
}
