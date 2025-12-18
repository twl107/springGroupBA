package com.example.springGroupBA.service;

import com.example.springGroupBA.constant.DbProductCategory;
import com.example.springGroupBA.constant.InquiryStatus;
import com.example.springGroupBA.constant.OrderStatus;
import com.example.springGroupBA.dto.PageRequestDTO;
import com.example.springGroupBA.dto.PageResultDTO;
import com.example.springGroupBA.dto.admin.AdminMainDTO;
import com.example.springGroupBA.dto.admin.ReportHistoryDTO;
import com.example.springGroupBA.entity.CustomerReview;
import com.example.springGroupBA.entity.board.Board;
import com.example.springGroupBA.entity.board.BoardReply;
import com.example.springGroupBA.entity.board.BoardReplyReport;
import com.example.springGroupBA.entity.board.BoardReport;
import com.example.springGroupBA.entity.dbShop.DbProduct;
import com.example.springGroupBA.dto.dbShop.DbShopDTO;
import com.example.springGroupBA.entity.dbShop.Order;
import com.example.springGroupBA.entity.pds.Pds;
import com.example.springGroupBA.entity.pds.PdsReply;
import com.example.springGroupBA.repository.board.BoardReplyReportRepository;
import com.example.springGroupBA.repository.board.BoardReplyRepository;
import com.example.springGroupBA.repository.board.BoardReportRepository;
import com.example.springGroupBA.repository.board.BoardRepository;
import com.example.springGroupBA.repository.dbShop.CartItemRepository;
import com.example.springGroupBA.repository.dbShop.DbProductRepository;
import com.example.springGroupBA.repository.CustomerReviewRepository;
import com.example.springGroupBA.repository.dbShop.OrderItemRepository;
import com.example.springGroupBA.repository.dbShop.OrderRepository;
import com.example.springGroupBA.repository.inquiry.InquiryRepository;
import com.example.springGroupBA.repository.member.MemberRepository;
import com.example.springGroupBA.repository.pds.PdsReplyRepository;
import com.example.springGroupBA.repository.pds.PdsRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

  private final CustomerReviewRepository reviewRepository;
  private final DbProductRepository dbProductRepository;
  private final CartItemRepository cartItemRepository;
  private final OrderItemRepository orderItemRepository;
  private final BoardRepository boardRepository;
  private final BoardReplyRepository boardReplyRepository;
  private final BoardReportRepository boardReportRepository;
  private final BoardReplyReportRepository boardReplyReportRepository;
  private final MemberRepository memberRepository;
  private final OrderRepository orderRepository;
  private final InquiryRepository inquiryRepository;

  @Value("${org.zerock.upload.path}")
  private String uploadPath;
  private String contextPath = "/springGroupBA";

    /* ===========================
       고객후기 관리
    ============================ */

  public List<CustomerReview> findAllReviews() {
    return reviewRepository.findAll();
  }

  public CustomerReview findReviewById(Long id) {
    return reviewRepository.findById(id).orElse(null);
  }

  public void saveReview(CustomerReview review, MultipartFile file) {

    // 파일 있을 경우
    if (file != null && !file.isEmpty()) {
      String savedName = saveReviewFile(file);
      review.setImagePath(savedName);
    }

    reviewRepository.save(review);
  }

  public void deleteReview(Long id) {
    reviewRepository.deleteById(id);
  }

  public void toggleReviewVisible(Long id) {
    CustomerReview review = findReviewById(id);
    if (review != null) {
      review.setVisible(review.getVisible().equals("Y") ? "N" : "Y");
      reviewRepository.save(review);
    }
  }

  public String saveReviewFile(MultipartFile file) {
    if (file == null || file.isEmpty()) return null;

    String reviewPath = uploadPath + "review/";
    File dir = new File(reviewPath);
    if (!dir.exists()) dir.mkdirs();

    String original = file.getOriginalFilename();
    String ext = original.substring(original.lastIndexOf("."));
    String uuid = UUID.randomUUID().toString();
    String newFileName = uuid + ext;

    File dest = new File(reviewPath + newFileName);

    try {
      file.transferTo(dest);
    } catch (IOException e) {
      throw new RuntimeException("파일 저장 실패: " + e.getMessage());
    }

    return newFileName;
  }

  public List<CustomerReview> findVisibleReviews() {
    return reviewRepository.findByVisible("Y");
  }

  public Page<CustomerReview> findVisibleReviews(Pageable pageable) {
    return reviewRepository.findByVisibleOrderByCreatedAtDesc("Y", pageable);
  }

  public Page<CustomerReview> getReviewPage(int page) {
    Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "id"));
    return reviewRepository.findAll(pageable);
  }

  /* ===========================
       쇼핑몰 관리
    ============================ */
  @Transactional(readOnly = true)
  public PageResultDTO<DbShopDTO, DbProduct> getDbShopPage(PageRequestDTO requestDTO,
                                                           String statusMode,
                                                           List<DbProductCategory> categories) {

    Pageable pageable = requestDTO.getPageable(Sort.by("id").descending());

    // 검색 실행
    Page<DbProduct> result = dbProductRepository.searchDbProduct(
            statusMode,
            categories,
            requestDTO.getKeyword(),
            pageable
    );

    Function<DbProduct, DbShopDTO> fn = (DbShopDTO::entityToDto);

    return new PageResultDTO<>(result, fn);
  }

  @Transactional
  public void deleteDbProduct(Long id) {
    DbProduct product = dbProductRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("해당 상품이 존재하지 않습니다. id=" + id));

    if (orderItemRepository.existsByProduct(product)) {
      throw new IllegalArgumentException("이미 주문된 이력이 있는 상품은 삭제할 수 없습니다.\n(판매 중지 기능을 이용해주세요)");
    }

    cartItemRepository.deleteByProduct(product);

    String mainImageName = product.getMainImage();
    List<String> contentImages = extractContentImageFileNames(product.getDescription());

    dbProductRepository.delete(product);
    dbProductRepository.flush();

    if (mainImageName != null && !mainImageName.isEmpty()) deleteFile(mainImageName);
    for (String imageFile : contentImages) deleteFile(imageFile);
  }

  @Transactional
  public void stopSelling(Long id) {
    DbProduct product = dbProductRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("상품이 없습니다."));
    product.markAsDeleted();
  }

  @Transactional
  public void resell(Long id) {
    DbProduct product = dbProductRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("상품이 없습니다."));
    product.markAsActive();
  }

  @Transactional
  public Long setDbProductInput(DbShopDTO dto) {
    String fileName = null;
    if (dto.getFile() != null && !dto.getFile().isEmpty()) {
      fileName = saveFile(dto.getFile());
    }
    String processedDescription = processContentImages(dto.getDescription());
    dto.setDescription(processedDescription);
    DbProduct product = dtoToEntity(dto, fileName);
    dbProductRepository.save(product);
    return product.getId();
  }

  @Transactional(readOnly = true)
  public DbShopDTO getDbProductContent(Long id) {
    DbProduct entity = dbProductRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("해당 상품이 없습니다. id=" + id));
    DbShopDTO dto = DbShopDTO.entityToDto(entity);
    if (dto.getDescription() != null && !contextPath.isEmpty()) {
      String fixedDescription = dto.getDescription().replace("src=\"/upload/", "src=\"" + contextPath + "/upload/");
      dto.setDescription(fixedDescription);
    }
    return dto;
  }

  @Transactional
  public void setDbProductUpdate(DbShopDTO dto) {
    DbProduct entity = dbProductRepository.findById(dto.getId())
            .orElseThrow(() -> new IllegalArgumentException("해당 상품이 없습니다. id=" + dto.getId()));

    List<String> oldContentImages = extractContentImageFileNames(entity.getDescription());
    String processedDescription = processContentImages(dto.getDescription());
    List<String> newContentImages = extractContentImageFileNames(processedDescription);

    oldContentImages.removeAll(newContentImages);
    for (String fileName : oldContentImages) {
      deleteFile(fileName);
    }

    entity.setCategory(DbProductCategory.valueOf(dto.getCategory()));
    entity.setName(dto.getName());
    entity.setModelName(dto.getModelName());
    entity.setPrice(dto.getPrice());
    entity.setStock(dto.getStock());
    entity.setDescription(processedDescription);

    if (dto.getFile() != null && !dto.getFile().isEmpty()) {
      String oldFileName = entity.getMainImage();
      if (oldFileName != null && !oldFileName.isEmpty()) {
        deleteFile(oldFileName);
      }
      String newFileName = saveFile(dto.getFile());
      entity.setMainImage(newFileName);
    }
    else if (dto.isImageDeleted()) {
      String oldFileName = entity.getMainImage();
      if (oldFileName != null && !oldFileName.isEmpty()) {
        deleteFile(oldFileName);
      }
      entity.setMainImage(null);
    }
    dbProductRepository.save(entity);
  }

  public Map<String, Object> uploadImage(MultipartFile upload) {
    Map<String, Object> response = new HashMap<>();
    if (upload == null || upload.isEmpty()) {
      response.put("uploaded", 0);
      response.put("error", Map.of("message", "파일이 비어있습니다."));
      return response;
    }
    String uuid = UUID.randomUUID().toString().substring(0, 8);
    String originalName = upload.getOriginalFilename();
    String safeName = originalName != null ? originalName.replaceAll("[^a-zA-Z0-9.\\-]", "_") : "image.jpg";
    String fileName = uuid + "_" + safeName;
    File folder = new File(uploadPath, "cktemp");
    if (!folder.exists()) folder.mkdirs();
    File saveFile = new File(folder, fileName);
    try {
      upload.transferTo(saveFile);
      response.put("uploaded", 1);
      response.put("fileName", fileName);
      response.put("url", contextPath + "/upload/cktemp/" + fileName);
    } catch (IOException e) {
      log.error("이미지 업로드 실패", e);
      response.put("uploaded", 0);
      response.put("error", Map.of("message", "업로드 중 오류가 발생했습니다."));
    }
    return response;
  }

  private String processContentImages(String content) {
    if (content == null || content.isEmpty()) return "";
    Pattern pattern = Pattern.compile("/upload/cktemp/([^\"\\s?]+)");
    Matcher matcher = pattern.matcher(content);
    StringBuffer sb = new StringBuffer();
    File tempDir = new File(uploadPath, "cktemp");
    File targetDir = new File(uploadPath, "shop");
    if (!targetDir.exists()) targetDir.mkdirs();
    while (matcher.find()) {
      String encodedFileName = matcher.group(1);
      String fileName;
      try { fileName = URLDecoder.decode(encodedFileName, StandardCharsets.UTF_8.toString()); }
      catch (Exception e) { fileName = encodedFileName; }
      File tempFile = new File(tempDir, fileName);
      File targetFile = new File(targetDir, fileName);
      if (tempFile.exists()) {
        try { Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING); }
        catch (IOException e) {
          try { Files.copy(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING); Files.delete(tempFile.toPath()); }
          catch (Exception ex) { log.error("이미지 파일 이동 실패: " + fileName, ex); }
        }
      }
      matcher.appendReplacement(sb, Matcher.quoteReplacement("/upload/shop/" + encodedFileName));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  private List<String> extractContentImageFileNames(String content) {
    List<String> fileNames = new ArrayList<>();
    if (content == null || content.isEmpty()) return fileNames;
    Pattern pattern = Pattern.compile("/upload/shop/([^\"\\s?]+)");
    Matcher matcher = pattern.matcher(content);
    while (matcher.find()) {
      try { String fileName = URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8.toString()); fileNames.add(fileName); }
      catch (Exception e) { fileNames.add(matcher.group(1)); }
    }
    return fileNames;
  }

  private String saveFile(MultipartFile file) {
    if (file.isEmpty()) return null;
    String originalFileName = file.getOriginalFilename();
    String uuid = UUID.randomUUID().toString().substring(0, 8);
    String saveFileName = uuid + "_" + originalFileName;
    String savePath = uploadPath + "shop";
    File folder = new File(savePath);
    if (!folder.exists()) folder.mkdirs();
    try { file.transferTo(new File(folder, saveFileName)); } catch (IOException e) { e.printStackTrace(); return null; }
    return saveFileName;
  }

  private void deleteFile(String fileName) {
    try {
      String decodedName = URLDecoder.decode(fileName, StandardCharsets.UTF_8);
      File file = new File(uploadPath + "shop/" + decodedName);
      if (file.exists()) file.delete();
    } catch (Exception e) { log.error("파일 삭제 중 오류 발생: " + e.getMessage()); }
  }

  private DbProduct dtoToEntity(DbShopDTO dto, String fileName) {
    return DbProduct.builder()
            .name(dto.getName())
            .modelName(dto.getModelName())
            .category(DbProductCategory.valueOf(dto.getCategory()))
            .price(dto.getPrice())
            .stock(dto.getStock())
            .description(dto.getDescription())
            .mainImage(fileName)
            .isDeleted(dto.isDeleted())
            .build();
  }

  /* ===========================
       커뮤니티(게시판) 관리
    ============================ */
  @Transactional(readOnly = true)
  public List<Board> getBlindedBoards() {
    return boardRepository.findByIsBlindTrueOrderByRegDateDesc();
  }

  @Transactional(readOnly = true)
  public List<BoardReply> getBlindedBoardReplies() {
    return boardReplyRepository.findByIsBlindTrueOrderByRegDateDesc();
  }

  public void unblindBoard(Long id) {
    Board board = boardRepository.findById(id).orElseThrow();
    board.changeBlind(false);
    board.setReportCount(0);
    boardRepository.save(board);
  }

  public void unblindBoardReply(Long id) {
    BoardReply reply = boardReplyRepository.findById(id).orElseThrow();
    reply.setBlind(false);
    reply.setReport(0);
    boardReplyRepository.save(reply);
  }

  public void deleteBoard(Long id) {
    Board board = boardRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

    List<BoardReport> reports = boardReportRepository.findByBoard(board);
    if (!reports.isEmpty()) {
      boardReportRepository.deleteAll(reports);
      boardReportRepository.flush();
    }

    List<BoardReply> replies = board.getReplies();
    if (replies != null && !replies.isEmpty()) {
      for (BoardReply reply : replies) {
        List<BoardReplyReport> replyReports = boardReplyReportRepository.findByBoardReply(reply);
        if (!replyReports.isEmpty()) {
          boardReplyReportRepository.deleteAll(replyReports);
        }
      }
      boardReplyReportRepository.flush();
    }

    boardRepository.delete(board);
  }

  public void deleteBoardReply(Long id) {
    BoardReply reply = boardReplyRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));

    int childCount = boardReplyRepository.countByParentId(id);

    if (childCount > 0) {
      reply.markAsRemoved();
      reply.setBlind(false);
      reply.setReport(0);

      List<BoardReplyReport> reports = boardReplyReportRepository.findByBoardReply(reply);
      if (!reports.isEmpty()) {
        boardReplyReportRepository.deleteAll(reports);
      }

      boardReplyRepository.save(reply);

    } else {

      List<BoardReplyReport> reports = boardReplyReportRepository.findByBoardReply(reply);
      if (!reports.isEmpty()) {
        boardReplyReportRepository.deleteAll(reports);
        boardReplyReportRepository.flush();
      }

      BoardReply parent = reply.getParent();

      boardReplyRepository.delete(reply);
      boardReplyRepository.flush();

      while (parent != null && parent.isRemoved()) {
        if (boardReplyRepository.countByParentId(parent.getId()) == 0) {

          List<BoardReplyReport> parentReports = boardReplyReportRepository.findByBoardReply(parent);
          if (!parentReports.isEmpty()) {
            boardReplyReportRepository.deleteAll(parentReports);
            boardReplyReportRepository.flush();
          }

          BoardReply nextParent = parent.getParent();
          boardReplyRepository.delete(parent);
          boardReplyRepository.flush();

          parent = nextParent;
        } else {
          break;
        }
      }
    }
  }

  public List<ReportHistoryDTO> getBoardReportHistory(Long boardId) {
    Board board = boardRepository.findById(boardId)
            .orElseThrow(() -> new EntityNotFoundException("게시글이 존재하지 않습니다."));

    List<BoardReport> reports = boardReportRepository.findByBoard(board);

    return reports.stream().map(report -> ReportHistoryDTO.builder()
                    .id(report.getId())
                    .reporterMid(report.getMember().getMid())
                    .reporterName(report.getMember().getName())
                    .reason(report.getReason().getDescription())
                    .regDate(report.getRegDate())
                    .build())
            .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<ReportHistoryDTO> getBoardReplyReportHistory(Long replyId) {
    BoardReply reply = boardReplyRepository.findById(replyId)
            .orElseThrow(() -> new EntityNotFoundException("댓글이 존재하지 않습니다."));

    List<BoardReplyReport> reports = boardReplyReportRepository.findByBoardReply(reply);

    return reports.stream().map(report -> ReportHistoryDTO.builder()
                    .id(report.getId())
                    .reporterMid(report.getMember().getMid())
                    .reporterName(report.getMember().getName())
                    .reason(report.getReason().getDescription())
                    .regDate(report.getRegDate())
                    .build())
            .collect(Collectors.toList());
  }

  /* ===========================
   관리자 메인페이지 관리
============================ */
  @Transactional(readOnly = true)
  public AdminMainDTO getDashboardStatistics() {
    LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
    LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
    LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();

    long newOrders = orderRepository.countByStatus(OrderStatus.PAYMENT_COMPLETED);

    Long todaySalesVal = orderRepository.sumTotalPriceBetween(startOfDay, endOfDay);
    long todaySales = (todaySalesVal != null) ? todaySalesVal : 0L;

    Long monthSalesVal = orderRepository.sumTotalPriceBetween(startOfMonth, endOfDay);
    long monthSales = (monthSalesVal != null) ? monthSalesVal : 0L;

    long claims = orderRepository.countByStatusList(List.of(OrderStatus.CANCEL_REQUESTED, OrderStatus.RETURN_REQUESTED));

    long preparing = orderRepository.countByStatus(OrderStatus.PREPARING);

    List<Order> recentOrders = orderRepository.findTop5ByOrderByOrderDateDesc();

    long newMembers = memberRepository.countByJoinDateBetween(startOfDay, endOfDay);

    long blindBoards = getBlindedBoards().size();
    long blindReplies = getBlindedBoardReplies().size();
    long communityBlindCount = blindBoards + blindReplies;

    long unansweredQna = inquiryRepository.countByStatus(InquiryStatus.WAITING);

    return AdminMainDTO.builder()
            .newOrderCount(newOrders)
            .todaySales(todaySales)
            .monthSales(monthSales)
            .claimCount(claims)
            .preparingCount(preparing)
            .recentOrders(recentOrders)
            .newMemberCount(newMembers)
            .communityBlindCount(communityBlindCount)
            .unansweredQnaCount(unansweredQna)
            .build();
  }

}