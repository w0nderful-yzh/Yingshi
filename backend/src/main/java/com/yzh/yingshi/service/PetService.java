package com.yzh.yingshi.service;

import com.yzh.yingshi.dto.PetCreateRequest;
import com.yzh.yingshi.vo.PetVO;

import java.util.List;

public interface PetService {
    PetVO create(PetCreateRequest request);

    PetVO update(Long id, PetCreateRequest request);

    void delete(Long id);

    PetVO getById(Long id);

    List<PetVO> listAll();
}
