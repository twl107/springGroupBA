package com.example.springGroupBA.service;

import com.example.springGroupBA.constant.InquiryStatus;
import com.example.springGroupBA.dto.Inquiry.AdminAnswerDTO;
import com.example.springGroupBA.dto.Inquiry.InquiryDTO;
import com.example.springGroupBA.dto.PageRequestDTO;
import com.example.springGroupBA.dto.PageResultDTO;
import com.example.springGroupBA.entity.inquiry.Inquiry;
import com.example.springGroupBA.entity.member.Member;
import com.example.springGroupBA.repository.inquiry.InquiryRepository;
import com.example.springGroupBA.repository.member.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final MemberRepository memberRepository;

    @Value("${org.zerock.upload.path}")
    private String uploadFolder;

    public PageResultDTO<InquiryDTO, Inquiry> getInquiryList(String email, PageRequestDTO requestDTO, InquiryStatus status) {
        Pageable pageable = requestDTO.getPageable(Sort.by("regDate").descending());
        Page<Inquiry> result = inquiryRepository.searchInquiry(email, status, requestDTO.getType(), requestDTO.getKeyword(), pageable);

        Function<Inquiry, InquiryDTO> fn = (InquiryDTO::entityToDto);
        return new PageResultDTO<>(result, fn);
    }

    public PageResultDTO<InquiryDTO, Inquiry> getAllInquiryList(PageRequestDTO requestDTO, InquiryStatus status) {
        Pageable pageable = requestDTO.getPageable(Sort.by("regDate").descending());

        Page<Inquiry> result = inquiryRepository.searchInquiry(null, status, requestDTO.getType(), requestDTO.getKeyword(), pageable);

        Function<Inquiry, InquiryDTO> fn = (InquiryDTO::entityToDto);
        return new PageResultDTO<>(result, fn);
    }

    @Transactional
    public Long setInquiryInput(InquiryDTO dto, String name) {
        Member member = memberRepository.findByEmail(name)
                .orElseThrow(() -> new EntityNotFoundException("회원 정보를 찾을 수 없습니다. ID: " + name));

        Inquiry inquiry = dto.dtoToEntity(member);

        try {
            if (dto.getFile() != null && !dto.getFile().isEmpty()) {
                String[] fileNames = uploadFile(dto.getFile());
                inquiry.updateInquiry(inquiry.getTitle(), inquiry.getContent(), fileNames[1], fileNames[0]);
            }
        }
        catch (IOException e) {
            throw new RuntimeException("파일 저장 중 오류가 발생했습니다.", e);
        }

        inquiryRepository.save(inquiry);
        return inquiry.getId();
    }

    public InquiryDTO getInquiry(Long id) {
        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 문의가 존재하지 않습니다."));

        return InquiryDTO.entityToDto(inquiry);
    }

    @Transactional
    public void setInquiryUpdate(InquiryDTO dto, String name) {
        Inquiry inquiry = inquiryRepository.findById(dto.getId())
                .orElseThrow(() -> new EntityNotFoundException("해당 문의가 존재하지 않습니다."));

        if (!inquiry.getMember().getEmail().equals(name)) {
            throw new IllegalStateException("수정 권한이 없습니다.");
        }

        String originalName = inquiry.getOriginalFileName();
        String saveName = inquiry.getSaveFileName();

        try {
            if (dto.getFile() != null && !dto.getFile().isEmpty()) {
                if (saveName != null) {
                    deleteFile(saveName);
                }

                String[] fileName = uploadFile(dto.getFile());
                originalName = fileName[1];
                saveName = fileName[0];
            }
        }
        catch (IOException e) {
            throw new RuntimeException("파일 수정 중 오류가 발생했습니다.", e);
        }

        if (inquiry.getStatus() == InquiryStatus.COMPLETE) {
            throw new IllegalStateException("답변이 완료된 문의는 수정할 수 없습니다.");
        }
        inquiry.updateInquiry(dto.getTitle(), dto.getContent(), originalName, saveName);
    }

    @Transactional
    public void answerInquiry(AdminAnswerDTO dto, String name) {
        Inquiry inquiry = inquiryRepository.findById(dto.getId())
                .orElseThrow(() -> new EntityNotFoundException("답변할 문의가 존재하지 않습니다."));

        if (inquiry.getStatus() == InquiryStatus.COMPLETE) {
            throw new IllegalStateException("이미 답변이 완료된 문의입니다.");
        }
        inquiry.answerInquiry(dto.getAnswerContent());
    }

    @Transactional
    public void modifyAdminAnswer(AdminAnswerDTO dto, String name) {
        Inquiry inquiry = inquiryRepository.findById(dto.getId())
                .orElseThrow(() -> new EntityNotFoundException("수정할 문의가 존재하지 않습니다."));

        if (inquiry.getStatus() != InquiryStatus.COMPLETE) {
            throw new IllegalStateException("답변이 등록되지 않았거나, 수정할 수 없는 상태입니다.");
        }

        inquiry.updateAnswer(dto.getAnswerContent());
    }

    @Transactional
    public void deleteAdminAnswer(Long id, String name) {
        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("삭제할 문의가 존재하지 않습니다."));

        if (inquiry.getStatus() != InquiryStatus.COMPLETE || inquiry.getAnswer() == null) {
            throw new IllegalStateException("삭제할 답변이 존재하지 않습니다.");
        }
        inquiry.deleteAnswer();
    }

    @Transactional
    public void setInquiryDelete(Long id, String name) {
        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 문의가 존재하지 않습니다."));

        if (!inquiry.getMember().getEmail().equals(name)) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }

        if (inquiry.getSaveFileName() != null) {
            deleteFile(inquiry.getSaveFileName());
        }
        inquiryRepository.delete(inquiry);
    }

    private String[] uploadFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String uuid = UUID.randomUUID().toString().substring(0, 8);

        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String saveName = uuid + "_" + extension;

        File folder = new File(uploadFolder, "inquiry");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File saveFile = new File(folder, saveName);
        file.transferTo(saveFile);

        return new String[]{saveName, originalFilename};
    }

    private void deleteFile(String saveFileName) {
        File file = new File(uploadFolder + File.separator + "inquiry", saveFileName);
        if (file.exists()) {
            file.delete();
        }
    }

}
