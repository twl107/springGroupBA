package com.example.springGroupBA.service;

import com.example.springGroupBA.constant.DbProductCategory;
import com.example.springGroupBA.dto.PageRequestDTO;
import com.example.springGroupBA.dto.PageResultDTO;
import com.example.springGroupBA.dto.dbShop.DbShopDTO;
import com.example.springGroupBA.entity.dbShop.DbProduct;
import com.example.springGroupBA.repository.dbShop.DbProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DbShopService {

    private final DbProductRepository dbProductRepository;

    @Value("${org.zerock.upload.path}")
    private String uploadPath;
    private String contextPath = "/springGroupBA";

    @Transactional(readOnly = true)
    public PageResultDTO<DbShopDTO, DbProduct> getDbShopList(PageRequestDTO requestDTO,
                                                             String statusMode,
                                                             List<DbProductCategory> categories) {

        Pageable pageable = requestDTO.getPageable(Sort.by("id").descending());
        Page<DbProduct> result = dbProductRepository.searchDbProduct(statusMode, categories, requestDTO.getKeyword(), pageable);

        Function<DbProduct, DbShopDTO> fn = (DbShopDTO::entityToDto);

        return new PageResultDTO<>(result, fn);
    }

    @Transactional(readOnly = true)
    public DbShopDTO getDbProductContent(Long id) {
        DbProduct entity = dbProductRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품이 존재하지 않습니다. id=" + id));

        DbShopDTO dto = DbShopDTO.entityToDto(entity);

        if (dto.getDescription() != null && !contextPath.isEmpty()) {
            String fixedDescription = dto.getDescription().replace("src=\"/upload/", "src=\"" + contextPath + "/upload/");
            dto.setDescription(fixedDescription);
        }

        return dto;
    }
}