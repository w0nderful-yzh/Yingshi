package com.yzh.yingshi.controller;

import com.yzh.yingshi.common.api.ApiResponse;
import com.yzh.yingshi.dto.PetCreateRequest;
import com.yzh.yingshi.service.PetService;
import com.yzh.yingshi.vo.PetVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pets")
public class PetController {

    private final PetService petService;

    @PostMapping
    public ApiResponse<PetVO> create(@Valid @RequestBody PetCreateRequest request) {
        return ApiResponse.success(petService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<PetVO> update(@PathVariable Long id, @Valid @RequestBody PetCreateRequest request) {
        return ApiResponse.success(petService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        petService.delete(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}")
    public ApiResponse<PetVO> getById(@PathVariable Long id) {
        return ApiResponse.success(petService.getById(id));
    }

    @GetMapping
    public ApiResponse<List<PetVO>> listAll() {
        return ApiResponse.success(petService.listAll());
    }
}
