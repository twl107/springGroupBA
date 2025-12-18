package com.example.springGroupBA.common;

import com.example.springGroupBA.entity.member.Member;
import com.example.springGroupBA.entity.member.Message;
import lombok.Data;

import java.util.List;

@Data
public class PageVO {
  private int pag;
  private int pageSize;
  private int totRecCnt;
  private int totPage;
  private int startIndexNo;
  private int curScrStartNo;
  private int blockSize;
  private int curBlock;
  private int lastBlock;

  private String section;	// 'guest/board/pds/member'....
  private String part;		// '학습/여행/음식/기타'...
  private String search;  // '글제목/글쓴이/글내용'
  private String searchString;  // '검색어...'
  private String searchStr; // '글제목/글쓴이/글내용'
  private String boardFlag;	// 검색기에서 글내용보기 호출시 사용하는 변수
  private String currentPage;
  private int msgSw;    // 웹메세지에서 사용하는 변수(앞으로 가야하는 위치 - 0:메세지작성, 1:받은메세지, 2:새메세지, 3:보낸메세지, 4:수신확인, 5:휴지통, 6:메세지내용보기, 9:휴지통비우기)
  private int preSw;    // 웹메세지에서 사용하는 변수(현재 위치했넌곳의 번호)
  private int startPageNo;    // 웹메세지에서 사용하는 변수(현재 위치했넌곳의 번호)
  private int endPageNo;    // 웹메세지에서 사용하는 변수(현재 위치했넌곳의 번호)

  private int level;	// 회원 등급(초기값:99 - 비회원)
  private String keyword;
  private String role;


//  private List<Board> boardList;  // 게시판의 글 리스트를 저장하기위한 변수

  private List<Member> memberList;

  private List webMessageList;

  private boolean isOwner;    // 본인 인증여부를 확인하기위한 변수

}
