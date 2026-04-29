package com.yzh.yingshi.service.impl;

import com.yzh.yingshi.common.api.BusinessCode;
import com.yzh.yingshi.common.exception.BusinessException;
import com.yzh.yingshi.dto.PetCreateRequest;
import com.yzh.yingshi.entity.Pet;
import com.yzh.yingshi.mapper.PetMapper;
import com.yzh.yingshi.service.PetService;
import com.yzh.yingshi.vo.PetVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PetServiceImpl implements PetService {

    private final PetMapper petMapper;
    private final HttpServletRequest request;

    @Override
    public PetVO create(PetCreateRequest dto) {
        Pet pet = new Pet();
        pet.setUserId(getCurrentUserId());
        pet.setPetName(dto.getPetName());
        pet.setPetType(dto.getPetType());
        pet.setAge(dto.getAge());
        pet.setGender(dto.getGender());
        pet.setRemark(dto.getRemark());
        pet.setAvatarUrl(dto.getAvatarUrl());
        pet.setCreatedAt(LocalDateTime.now());
        petMapper.insert(pet);
        return toVO(pet);
    }

    @Override
    public PetVO update(Long id, PetCreateRequest dto) {
        Pet pet = petMapper.selectById(id);
        if (pet == null) {
            throw new BusinessException(BusinessCode.RESOURCE_NOT_FOUND, "宠物不存在");
        }
        if (!pet.getUserId().equals(getCurrentUserId())) {
            throw new BusinessException(BusinessCode.FORBIDDEN, "无权操作");
        }
        pet.setPetName(dto.getPetName());
        pet.setPetType(dto.getPetType());
        pet.setAge(dto.getAge());
        pet.setGender(dto.getGender());
        pet.setRemark(dto.getRemark());
        pet.setAvatarUrl(dto.getAvatarUrl());
        petMapper.updateById(pet);
        return toVO(pet);
    }

    @Override
    public void delete(Long id) {
        Pet pet = petMapper.selectById(id);
        if (pet == null) {
            throw new BusinessException(BusinessCode.RESOURCE_NOT_FOUND, "宠物不存在");
        }
        if (!pet.getUserId().equals(getCurrentUserId())) {
            throw new BusinessException(BusinessCode.FORBIDDEN, "无权操作");
        }
        petMapper.deleteById(id);
    }

    @Override
    public PetVO getById(Long id) {
        Pet pet = petMapper.selectById(id);
        if (pet == null) {
            throw new BusinessException(BusinessCode.RESOURCE_NOT_FOUND, "宠物不存在");
        }
        return toVO(pet);
    }

    @Override
    public List<PetVO> listAll() {
        Long userId = getCurrentUserId();
        List<Pet> pets = petMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Pet>()
                        .eq("user_id", userId)
                        .orderByDesc("created_at")
        );
        return pets.stream().map(this::toVO).collect(Collectors.toList());
    }

    private PetVO toVO(Pet pet) {
        PetVO vo = new PetVO();
        vo.setId(pet.getId());
        vo.setPetName(pet.getPetName());
        vo.setPetType(pet.getPetType());
        vo.setAge(pet.getAge());
        vo.setGender(pet.getGender());
        vo.setRemark(pet.getRemark());
        vo.setAvatarUrl(pet.getAvatarUrl());
        vo.setCreatedAt(pet.getCreatedAt());
        return vo;
    }

    private Long getCurrentUserId() {
        return (Long) request.getAttribute("userId");
    }
}
