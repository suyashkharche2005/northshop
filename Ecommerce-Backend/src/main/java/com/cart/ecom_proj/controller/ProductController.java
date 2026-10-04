package com.cart.ecom_proj.controller;
import com.cart.ecom_proj.dto.*;
import com.cart.ecom_proj.model.Product;
import com.cart.ecom_proj.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
@RestController @RequestMapping("/api")
public class ProductController {
 private final ProductService service;
 public ProductController(ProductService service){this.service=service;}
 @GetMapping("/products") public List<ProductResponse> all(){return service.all();}
 @GetMapping("/products/search") public List<ProductResponse> search(@RequestParam String keyword){return service.search(keyword);}
 @GetMapping("/product/{id}") public ProductResponse one(@PathVariable int id){return service.one(id);}
 @GetMapping("/product/{id}/image") public ResponseEntity<byte[]> image(@PathVariable int id){Product p=service.image(id);return p.getImageDate()==null?ResponseEntity.notFound().build():ResponseEntity.ok().contentType(MediaType.parseMediaType(p.getImageType())).body(p.getImageDate());}
 @PostMapping(value="/product",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public ResponseEntity<ProductResponse> create(@Valid @RequestPart("product") ProductRequest request,@RequestPart(value="imageFile",required=false) MultipartFile image){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request,image));}
 @PutMapping(value="/product/{id}",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public ProductResponse update(@PathVariable int id,@Valid @RequestPart("product") ProductRequest request,@RequestPart(value="imageFile",required=false) MultipartFile image){return service.update(id,request,image);}
 @DeleteMapping("/product/{id}") public ResponseEntity<Void> delete(@PathVariable int id){service.delete(id);return ResponseEntity.noContent().build();}
}
