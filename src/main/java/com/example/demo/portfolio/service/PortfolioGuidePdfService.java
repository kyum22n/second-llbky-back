package com.example.demo.portfolio.service;

import java.io.ByteArrayOutputStream; // PDF를 메모리에서 바이트 배열로 만들기 위한 클래스
import java.time.format.DateTimeFormatter; // 날짜를 원하는 형식의 문자열로 변환하기 위한 클래스
import java.util.List; // 여러 개의 PortfolioGuide 객체를 담기 위한 자료구조

import org.springframework.beans.factory.annotation.Autowired; // 스프링 의존성 주입을 위한 어노테이션
import org.springframework.core.io.ClassPathResource; // 클래스패스(src/main/resources)에서 파일을 읽어오기 위한 클래스
import org.springframework.stereotype.Service; // 이 클래스가 비즈니스 로직을 처리하는 서비스 계층임을 표시

import com.example.demo.portfolio.dto.GuideContentData; // 가이드 콘텐츠 데이터를 담는 DTO 클래스
import com.example.demo.portfolio.dto.GuideItemData; // 가이드 항목 데이터를 담는 DTO 클래스
import com.example.demo.portfolio.dto.GuideStepData; // 가이드 단계 데이터를 담는 DTO 클래스
import com.example.demo.portfolio.entity.PortfolioGuide; // 포트폴리오 가이드 엔티티 클래스
import com.fasterxml.jackson.databind.ObjectMapper; // JSON 문자열을 객체로 변환하거나 그 반대를 위한 라이브러리
import com.itextpdf.io.exceptions.IOException; // PDF 처리 중 발생할 수 있는 예외 클래스
import com.itextpdf.io.font.PdfEncodings; // PDF에서 한글 등 다국어 지원을 위한 인코딩 설정
import com.itextpdf.kernel.colors.Color; // PDF에서 색상을 표현하기 위한 기본 클래스
import com.itextpdf.kernel.colors.ColorConstants; // 자주 사용되는 색상들을 상수로 정의한 클래스
import com.itextpdf.kernel.colors.DeviceRgb; // RGB 색상 모델로 색상을 정의하기 위한 클래스
import com.itextpdf.kernel.events.Event; // PDF 문서에서 발생하는 이벤트를 처리하기 위한 인터페이스
import com.itextpdf.kernel.events.IEventHandler; // PDF 이벤트를 처리하는 핸들러 인터페이스
import com.itextpdf.kernel.events.PdfDocumentEvent; // PDF 문서에서 발생하는 이벤트 타입 정의
import com.itextpdf.kernel.font.PdfFont; // PDF에서 사용할 폰트를 나타내는 클래스
import com.itextpdf.kernel.font.PdfFontFactory; // PDF 폰트를 생성하기 위한 팩토리 클래스
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle; // PDF 페이지의 사각형 영역을 정의하는 클래스
import com.itextpdf.kernel.pdf.PdfDocument; // PDF 문서의 핵심 객체
import com.itextpdf.kernel.pdf.PdfPage; // PDF의 개별 페이지를 나타내는 클래스
import com.itextpdf.kernel.pdf.PdfWriter; // PDF 파일을 생성하고 쓰기 위한 클래스
import com.itextpdf.kernel.pdf.canvas.PdfCanvas; // PDF 페이지에 직접 그리기 위한 캔버스 클래스
import com.itextpdf.layout.Canvas; // 레이아웃 요소들을 PDF에 배치하기 위한 캔버스
import com.itextpdf.layout.Document; // PDF 문서의 레이아웃을 관리하는 고수준 API
import com.itextpdf.layout.borders.SolidBorder; // 테두리를 그리기 위한 클래스
import com.itextpdf.layout.element.AreaBreak; // 페이지 나누기나 컬럼 나누기를 위한 요소
import com.itextpdf.layout.element.Cell; // 테이블의 셀을 나타내는 요소
import com.itextpdf.layout.element.Paragraph; // 문단(텍스트 블록)을 나타내는 요소
import com.itextpdf.layout.element.Table; // 테이블 레이아웃을 위한 요소
import com.itextpdf.layout.properties.AreaBreakType; // 페이지 나누기 유형을 정의하는 열거형
import com.itextpdf.layout.properties.TextAlignment; // 텍스트 정렬 방식을 정의하는 열거형
import com.itextpdf.layout.properties.UnitValue; // 크기 단위(퍼센트, 포인트 등)를 정의하는 클래스
import com.itextpdf.layout.properties.VerticalAlignment; // 수직 정렬 방식을 정의하는 열거형

import jakarta.servlet.http.HttpServletResponse; // HTTP 응답을 처리하기 위한 서블릿 API
import lombok.extern.slf4j.Slf4j; // 로깅을 위한 Lombok 어노테이션

@Service // 스프링 컨테이너에 이 클래스를 서비스 빈으로 등록
@Slf4j // Lombok을 통해 log 필드를 자동으로 생성 (System.out.println 대신 로깅 사용)
public class PortfolioGuidePdfService {
    
    @Autowired // 스프링이 ObjectMapper 인스턴스를 자동으로 주입해줌
    private ObjectMapper objectMapper; // JSON 파싱을 위한 Jackson 라이브러리 객체
    
    // 색상 팔레트 - PDF 전체에서 일관된 색상 사용을 위해 상수로 정의
    private static final Color PRIMARY_COLOR = new DeviceRgb(41, 128, 185);      // 메인 색상: 진한 파랑 (RGB 값으로 정의)
    private static final Color SECONDARY_COLOR = new DeviceRgb(52, 73, 94);      // 보조 색상: 진한 회색
    private static final Color ACCENT_COLOR = new DeviceRgb(46, 204, 113);       // 강조 색상: 초록 (완료 상태 표시용)
    private static final Color LIGHT_BG = new DeviceRgb(236, 240, 241);          // 배경색: 연한 회색
    private static final Color SECTION_BG = new DeviceRgb(189, 195, 199);        // 섹션 배경색
    

    /**
     * 한글 폰트 생성 - PDF 생성 시마다 새로운 폰트 인스턴스 생성
     */
    private PdfFont createKoreanFont() throws Exception {
        ClassPathResource fontResource = new ClassPathResource("fonts/NanumGothic.ttf");
        return PdfFontFactory.createFont(
            fontResource.getInputStream().readAllBytes(),
            PdfEncodings.IDENTITY_H,
            PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
        );
    }

    /**
     * 포트폴리오 PDF 생성 메인 메서드 - 컨트롤러에서 호출하는 핵심 메서드
     */
    public void generateGuidePdf(PortfolioGuide guide, HttpServletResponse response) throws IOException {
        log.info("포트폴리오 PDF 생성 시작 - guideId: {}", guide.getGuideId()); // 어떤 가이드의 PDF를 생성하는지 로그로 추적
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream(); // PDF 데이터를 메모리에 저장할 스트림 객체
        
        try {
            PdfFont koreanFont = createKoreanFont();
            PdfFont koreanBoldFont = koreanFont;
            
            PdfWriter writer = new PdfWriter(baos); // PDF를 바이트 스트림에 쓸 수 있는 writer 객체 생성

            PageSize widescreen = new PageSize(960, 540);
            PdfDocument pdfDocument = new PdfDocument(writer); // PDF 문서의 핵심 객체 생성
            pdfDocument.setDefaultPageSize(widescreen); // 페이지 크기 설정
           
           
            Document document = new Document(pdfDocument); // 고수준 레이아웃 API를 사용하기 위한 Document 객체
            
            document.setMargins(40, 40, 60, 40); // 페이지 여백 설정 (상, 우, 하, 좌 순서)
            
            // 모든 페이지에 헤더와 푸터를 자동으로 추가하는 이벤트 핸들러 등록
            pdfDocument.addEventHandler(PdfDocumentEvent.END_PAGE, // 페이지가 끝날 때마다 실행
                new PortfolioHeaderFooterHandler(guide.getTitle(), koreanFont)); // 프로젝트 제목과 폰트를 전달
            
            // PDF 내용 구성 - 순서대로 추가
            addPortfolioCover(document, guide, koreanFont, koreanBoldFont); // 1. 표지 페이지 추가
            addDynamicContentsPage(document, guide, koreanFont, koreanBoldFont); 
            // 2. 프로젝트 개요 페이지 추가
            addPortfolioContent(document, guide, koreanFont, koreanBoldFont); // 3. 실제 가이드 내용 추가
            
            document.close(); // PDF 문서 생성 완료 및 리소스 정리
            
            // 브라우저에 PDF 파일을 다운로드 형태로 전송하기 위한 HTTP 응답 설정
            String fileName = String.format("Portfolio_%s.pdf", 
                guide.getTitle().replaceAll("[^a-zA-Z0-9가-힣]", "_")); // 특수문자를 언더스코어로 치환하여 안전한 파일명 생성
            
            response.setContentType("application/pdf"); // 브라우저에게 이것이 PDF 파일임을 알려줌
            response.setHeader("Content-Disposition", // 파일 다운로드 방식과 파일명 지정
                "attachment; filename=\"" + java.net.URLEncoder.encode(fileName, "UTF-8") + "\""); // 한글 파일명도 안전하게 인코딩
            response.setContentLength(baos.size()); // 파일 크기 정보 제공 (다운로드 진행률 표시에 사용)
            
            baos.writeTo(response.getOutputStream()); // 메모리에 저장된 PDF 데이터를 HTTP 응답으로 전송
            response.getOutputStream().flush(); // 버퍼에 남은 데이터까지 모두 전송
            
            log.info("포트폴리오 PDF 생성 완료"); // 성공 로그 출력
            
        } catch (Exception e) { // PDF 생성 중 발생할 수 있는 모든 예외 처리
            log.error("PDF 생성 실패", e); // 오류 로그와 스택 트레이스 출력
            throw new IOException("PDF 생성 실패: " + e.getMessage()); // 컨트롤러에서 처리할 수 있도록 예외 다시 던지기
        }
    }
    
    /**
     * 포트폴리오 표지 페이지 생성 - PPT 스타일의 깔끔한 표지
     */
    private void addPortfolioCover(Document document, PortfolioGuide guide,
                                    PdfFont koreanFont, PdfFont koreanBoldFont) {
        // 상단 장식 바 - 디자인적 요소로 전문성 부여
        Table topBar = new Table(UnitValue.createPercentArray(1)) // 1개 컬럼의 테이블 생성 (장식용)
            .setWidth(UnitValue.createPercentValue(100)) // 페이지 전체 너비 사용
            .setMarginTop(100); // 위쪽 여백으로 중앙 배치 효과
        
        Cell topCell = new Cell() // 테이블 셀 생성
            .add(new Paragraph("")) // 빈 내용 (색상만 표시)
            .setHeight(10) // 바의 높이 설정
            .setBackgroundColor(PRIMARY_COLOR) // 메인 색상으로 칠하기
            .setBorder(null); // 테두리 제거
        topBar.addCell(topCell); // 테이블에 셀 추가
        document.add(topBar); // 문서에 테이블 추가
        
        // 메인 제목 "PORTFOLIO" - 임팩트 있는 대문자 제목
        Paragraph title = new Paragraph("PORTFOLIO")
            .setFont(koreanBoldFont) // 굵은 폰트 사용
            .setFontSize(36) // 큰 글자 크기로 임팩트 부여
            .setBold() // 볼드체 적용
            .setTextAlignment(TextAlignment.CENTER) // 중앙 정렬
            .setMarginTop(40) // 위쪽 여백
            .setMarginBottom(20) // 아래쪽 여백
            .setFontColor(PRIMARY_COLOR); // 메인 색상 적용
        document.add(title);
        
        // 프로젝트명 - 실제 프로젝트 이름 표시
        Paragraph projectTitle = new Paragraph(guide.getTitle())
            .setFont(koreanFont) // 일반 한글 폰트 사용
            .setFontSize(24) // 제목보다 작지만 충분히 눈에 띄는 크기
            .setTextAlignment(TextAlignment.CENTER) // 중앙 정렬
            .setMarginBottom(60) // 다음 요소와의 간격
            .setFontColor(SECONDARY_COLOR); // 보조 색상 사용
        document.add(projectTitle);
        
        // 하단 장식 바 - 상단과 대칭으로 디자인 통일성 부여
        Table bottomBar = new Table(UnitValue.createPercentArray(1))
            .setWidth(UnitValue.createPercentValue(100))
            .setMarginTop(100);
        
        Cell bottomCell = new Cell()
            .add(new Paragraph(""))
            .setHeight(10)
            .setBackgroundColor(PRIMARY_COLOR)
            .setBorder(null);
        bottomBar.addCell(bottomCell);
        document.add(bottomBar);
        
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE)); // 새 페이지로 이동 (표지와 내용 분리)
    }
    
    /**
     * 포트폴리오의 핵심 콘텐츠 처리 - JSON 형태로 저장된 가이드 내용을 파싱하여 표시
     */
    private void addPortfolioContent(Document document, PortfolioGuide guide,
                                      PdfFont koreanFont, PdfFont koreanBoldFont) {
        if (guide.getGuideContent() == null || guide.getGuideContent().trim().isEmpty()) { // 내용이 없는 경우 처리
            Paragraph emptyMsg = new Paragraph("작성된 내용이 없습니다.")
                .setFont(koreanFont)
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(100);
            document.add(emptyMsg);
            return; // 더 이상 처리할 것이 없으므로 메서드 종료
        }
        
        try {
            // JSON 문자열을 Java 객체로 변환 - Jackson 라이브러리 사용
            GuideContentData content = objectMapper.readValue(
                guide.getGuideContent(), GuideContentData.class); // DB에 저장된 JSON을 DTO로 파싱
            
            // 각 단계별로 순차 처리
            for (int i = 0; i < content.getSteps().size(); i++) {
                GuideStepData step = content.getSteps().get(i); // 개별 단계 데이터 추출
                
                if (i > 0) { // 첫 번째 단계가 아닌 경우
                    document.add(new AreaBreak(AreaBreakType.NEXT_PAGE)); // 새 페이지에서 시작
                }
                
                addStepSection(document, step, koreanFont, koreanBoldFont); // 단계별 섹션 추가
            }
            
        } catch (Exception e) { // JSON 파싱 실패 시 예외 처리
            log.error("콘텐츠 파싱 실패", e); // 오류 로그 출력
            Paragraph errorMsg = new Paragraph("콘텐츠를 불러올 수 없습니다.")
                .setFont(koreanFont)
                .setFontColor(ColorConstants.RED); // 빨간색으로 오류 표시
            document.add(errorMsg);
        }
    }
    
    /**
     * 개별 단계의 섹션 생성 - PPT처럼 단계별로 구성
     */
    private void addStepSection(Document document, GuideStepData step,
                                 PdfFont koreanFont, PdfFont koreanBoldFont) {
        addStepDividerPage(document, step, koreanFont, koreanBoldFont); // 단계 구분 페이지 먼저 추가
        
        // 해당 단계의 모든 항목들을 각각 별도 페이지로 처리
        if (step.getItems() != null && !step.getItems().isEmpty()) {
            for (int i = 0; i < step.getItems().size(); i++) {
                document.add(new AreaBreak(AreaBreakType.NEXT_PAGE)); // 항목마다 새 페이지
                addPPTStyleItemPage(document, step, step.getItems().get(i), i + 1, koreanFont, koreanBoldFont); // PPT 스타일로 항목 표시
            }
        }
    }
    
    /**
     * 단계 구분 페이지 생성 - 새로운 단계가 시작됨을 시각적으로 표현
     */
    private void addStepDividerPage(Document document, GuideStepData step,
                                     PdfFont koreanFont, PdfFont koreanBoldFont) {
        // 왼쪽에 큰 단계 번호 표시
        Paragraph bigNumber = new Paragraph(String.format("%02d", step.getStepNumber())) // 01, 02 형태로 0 패딩
            .setFont(koreanBoldFont)
            .setFontSize(120) // 매우 큰 글씨로 임팩트 부여
            .setBold()
            .setFontColor(new DeviceRgb(200, 200, 200))  // 연한 회색으로 배경 효과
            .setMarginTop(150)
            .setMarginLeft(60);
        document.add(bigNumber);
        
        // 단계 제목
        Paragraph stepTitle = new Paragraph(step.getStepTitle())
            .setFont(koreanBoldFont)
            .setFontSize(36)
            .setBold()
            .setMarginTop(-80) // 음수 마진으로 큰 숫자와 겹치도록
            .setMarginLeft(60)
            .setMarginBottom(30);
        document.add(stepTitle);
        
        // 제목 아래 구분선
        Table line = new Table(UnitValue.createPercentArray(1))
            .setWidth(UnitValue.createPercentValue(80))
            .setMarginLeft(60);
        
        Cell lineCell = new Cell()
            .add(new Paragraph(""))
            .setHeight(3)
            .setBackgroundColor(PRIMARY_COLOR)
            .setBorder(null);
        line.addCell(lineCell);
        document.add(line);
        
        // 우측 상단에 페이지 정보 표시 (선택사항)
        Paragraph stepInfo = new Paragraph(String.format("%02d. %s", 
                step.getStepNumber(), step.getStepTitle()))
            .setFont(koreanFont)
            .setFontSize(11)
            .setTextAlignment(TextAlignment.RIGHT)
            .setFontColor(ColorConstants.GRAY)
            .setMarginTop(-40)
            .setMarginRight(40);
        document.add(stepInfo);
    }
    
    /**
     * PPT 스타일의 개별 항목 페이지 생성 - 각 항목을 슬라이드처럼 표시
     */
    private void addPPTStyleItemPage(Document document, GuideStepData step, 
                                      GuideItemData item, int itemNumber,
                                      PdfFont koreanFont, PdfFont koreanBoldFont) {
        
        // 우측 상단 네비게이션 - 현재 위치 표시
        Paragraph navigation = new Paragraph(String.format("%02d %s", 
                step.getStepNumber(), step.getStepTitle()))
            .setFont(koreanFont)
            .setFontSize(10)
            .setTextAlignment(TextAlignment.RIGHT)
            .setFontColor(ColorConstants.GRAY) // 회색으로 보조 정보임을 표시
            .setMarginTop(30)
            .setMarginRight(40)
            .setMarginBottom(20);
        document.add(navigation);
        
        // 헤더 영역 - 단계 번호와 항목 제목
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{15, 85})) // 번호:제목 = 15:85 비율
            .setWidth(UnitValue.createPercentValue(100))
            .setMarginBottom(30);
        
        // 왼쪽: 단계 번호 (배경 장식용)
        Cell numberCell = new Cell()
            .add(new Paragraph(String.format("%02d", step.getStepNumber()))
                .setFont(koreanBoldFont)
                .setFontSize(48)
                .setBold()
                .setFontColor(new DeviceRgb(200, 200, 200))) // 연한 회색으로 배경화
            .setBorder(null)
            .setVerticalAlignment(VerticalAlignment.TOP);
        headerTable.addCell(numberCell);
        
        // 우측: 항목 제목과 구분선
        Table titleBlock = new Table(UnitValue.createPercentArray(1))
            .setWidth(UnitValue.createPercentValue(100));
        
        // 항목 제목
        Cell titleCell = new Cell()
            .add(new Paragraph(item.getTitle())
                .setFont(koreanBoldFont)
                .setFontSize(20)
                .setBold())
            .setBorder(null)
            .setPaddingBottom(10);
        titleBlock.addCell(titleCell);
        
        // 제목 아래 구분선
        Cell lineCell = new Cell()
            .add(new Paragraph(""))
            .setHeight(2)
            .setBackgroundColor(PRIMARY_COLOR)
            .setBorder(null);
        titleBlock.addCell(lineCell);
        
        headerTable.addCell(new Cell().add(titleBlock).setBorder(null));
        document.add(headerTable);
        
        // 실제 항목 내용 표시
        if (item.getContent() != null && !item.getContent().trim().isEmpty()) {
            // 내용이 있는 경우 박스 형태로 표시
            Table contentBox = new Table(UnitValue.createPercentArray(1))
                .setWidth(UnitValue.createPercentValue(85))
                .setMarginLeft(80)
                .setMarginTop(20);
            
            Cell contentCell = new Cell()
                .add(new Paragraph(item.getContent())
                    .setFont(koreanFont)
                    .setFontSize(11)
                    .setFixedLeading(18))  // 줄간격 설정으로 가독성 향상
                .setBackgroundColor(LIGHT_BG) // 연한 배경으로 내용 영역 구분
                .setPadding(20)
                .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 1)); // 테두리로 박스 효과
            
            contentBox.addCell(contentCell);
            document.add(contentBox);
        } else {
            // 내용이 없는 경우 안내 메시지
            Paragraph emptyContent = new Paragraph("(작성된 내용이 없습니다)")
                .setFont(koreanFont)
                .setFontSize(11)
                .setItalic() // 기울임체로 안내 메시지임을 표시
                .setFontColor(ColorConstants.LIGHT_GRAY)
                .setMarginLeft(80)
                .setMarginTop(40)
                .setTextAlignment(TextAlignment.CENTER);
            document.add(emptyContent);
        }
    }
    
    /**
     * 헤더/푸터 이벤트 핸들러 - 모든 페이지에 공통으로 표시될 요소들
     */
    private class PortfolioHeaderFooterHandler implements IEventHandler {
        private String projectTitle; // 헤더에 표시할 프로젝트명
        private PdfFont font; // 헤더/푸터에 사용할 폰트
        
        public PortfolioHeaderFooterHandler(String projectTitle, PdfFont font) {
            this.projectTitle = projectTitle;
            this.font = font;
        }
        
        @Override
        public void handleEvent(Event event) { // 페이지 종료 이벤트 발생 시 실행
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event; // 이벤트를 PDF 문서 이벤트로 캐스팅
            PdfDocument pdfDoc = docEvent.getDocument(); // PDF 문서 객체 가져오기
            PdfPage page = docEvent.getPage(); // 현재 페이지 객체 가져오기
            int pageNumber = pdfDoc.getPageNumber(page); // 현재 페이지 번호 계산
            
            Rectangle pageSize = page.getPageSize(); // 페이지 크기 정보 가져오기
            PdfCanvas pdfCanvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdfDoc); // 페이지에 그리기 위한 캔버스
            
            try (Canvas canvas = new Canvas(pdfCanvas, pageSize)) { // 자동 리소스 정리를 위한 try-with-resources
                // 헤더 (표지, 목차 페이지 제외)
                if (pageNumber > 2) { // 1페이지(표지), 2페이지(목차)는 헤더 표시하지 않음
                    canvas.showTextAligned(
                        new Paragraph(projectTitle)
                            .setFont(font)
                            .setFontSize(9) // 작은 크기로 방해하지 않도록
                            .setFontColor(ColorConstants.GRAY), // 회색으로 보조 정보임을 표시
                        40, pageSize.getTop() - 30, // 페이지 상단 좌측에 배치
                        TextAlignment.LEFT
                    );
                }
                
                // 푸터 (모든 페이지에 페이지 번호 표시)
                canvas.showTextAligned(
                    new Paragraph(String.valueOf(pageNumber))
                        .setFont(font)
                        .setFontSize(9)
                        .setFontColor(ColorConstants.GRAY),
                    pageSize.getWidth() / 2, 30, // 페이지 하단 중앙에 배치
                    TextAlignment.CENTER
                );
            } catch (Exception e) {
                log.error("헤더/푸터 렌더링 실패", e); // 헤더/푸터 렌더링 실패 시 로그 출력
            }
        }
    }
    
    /**
     * 여러 프로젝트를 하나의 PDF로 통합 - 회원의 모든 포트폴리오를 한 번에 볼 수 있도록
     */
    public void generateMemberGuidesPdf(List<PortfolioGuide> guides, HttpServletResponse response) throws IOException {
        log.info("회원 포트폴리오 PDF 생성 - 프로젝트 수: {}", guides.size()); // 몇 개 프로젝트를 처리하는지 로그
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            PdfFont koreanFont = createKoreanFont();
            PdfFont koreanBoldFont = koreanFont;
            
            PdfWriter writer = new PdfWriter(baos);
            // ⭐ 16:9 와이드스크린 크기 설정
            PageSize widescreen = new PageSize(960, 540);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);
            pdfDocument.setDefaultPageSize(widescreen);
            document.setMargins(30,30,40,30);
            
            addMemberCoverPage(document, guides.size(), koreanFont, koreanBoldFont); // 통합 표지 (총 프로젝트 수 표시)
            
            // 각 프로젝트를 순차적으로 PDF에 추가
            for (int i = 0; i < guides.size(); i++) {
                if (i > 0) { // 첫 번째 프로젝트가 아닌 경우
                    document.add(new AreaBreak(AreaBreakType.NEXT_PAGE)); // 새 페이지에서 시작
                }
                
                PortfolioGuide guide = guides.get(i);
                
                addProjectDividerPage(document, i + 1, guide, koreanFont, koreanBoldFont); // 프로젝트 구분 페이지 (PROJECT 1, PROJECT 2...)
                 // 각 프로젝트의 개요
                addPortfolioContent(document, guide, koreanFont, koreanBoldFont); // 각 프로젝트의 상세 내용
            }
            
            document.close();
            
            // 다운로드 파일명에 날짜 포함
            String fileName = String.format("Portfolio_Collection_%s.pdf",
                DateTimeFormatter.ofPattern("yyyyMMdd").format(java.time.LocalDateTime.now())); // 현재 날짜를 파일명에 포함
            
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", 
                "attachment; filename=\"" + java.net.URLEncoder.encode(fileName, "UTF-8") + "\"");
            response.setContentLength(baos.size());
            
            baos.writeTo(response.getOutputStream());
            response.getOutputStream().flush();
            
            log.info("회원 포트폴리오 PDF 생성 완료");
            
        } catch (Exception e) {
            log.error("PDF 생성 실패", e);
            throw new IOException("PDF 생성 실패: " + e.getMessage());
        }
    }
    
    /**
     * 여러 프로젝트 통합 표지 페이지
     */
    private void addMemberCoverPage(Document document, int projectCount,
                                     PdfFont koreanFont, PdfFont koreanBoldFont) {
        Paragraph title = new Paragraph("PROJECT PORTFOLIO") // 복수 프로젝트임을 명시
            .setFont(koreanBoldFont)
            .setFontSize(36)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(200)
            .setMarginBottom(40)
            .setFontColor(PRIMARY_COLOR);
        document.add(title);
        
        Paragraph subtitle = new Paragraph(String.format("총 %d개의 프로젝트", projectCount)) // 포함된 프로젝트 수 표시
            .setFont(koreanFont)
            .setFontSize(18)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20);
        document.add(subtitle);
        
        Paragraph date = new Paragraph(java.time.LocalDateTime.now() // 문서 생성 날짜 표시
                .format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")))
            .setFont(koreanFont)
            .setFontSize(14)
            .setTextAlignment(TextAlignment.CENTER)
            .setFontColor(ColorConstants.GRAY);
        document.add(date);
        
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
    }
    
    /**
     * 개별 프로젝트 구분 페이지 - PROJECT 1, PROJECT 2 형태로 구분
     */
    private void addProjectDividerPage(Document document, int projectNumber, PortfolioGuide guide,
                                        PdfFont koreanFont, PdfFont koreanBoldFont) {
        Paragraph projectLabel = new Paragraph(String.format("PROJECT %d", projectNumber)) // 프로젝트 순번 표시
            .setFont(koreanBoldFont)
            .setFontSize(48)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(250)
            .setMarginBottom(40)
            .setFontColor(PRIMARY_COLOR);
        document.add(projectLabel);
        
        Paragraph projectName = new Paragraph(guide.getTitle()) // 실제 프로젝트명
            .setFont(koreanFont)
            .setFontSize(24)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20);
        document.add(projectName);
        
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
    }

/**
 * 가이드 내용 기반 동적 목차 생성
 */
private void addDynamicContentsPage(Document document, PortfolioGuide guide,
                                     PdfFont koreanFont, PdfFont koreanBoldFont) throws Exception {
    Paragraph title = new Paragraph("CONTENTS")
        .setFont(koreanBoldFont)
        .setFontSize(48)
        .setBold()
        .setItalic()
        .setTextAlignment(TextAlignment.CENTER)
        .setMarginTop(80)
        .setMarginBottom(60);
    document.add(title);
    
    if (guide.getGuideContent() != null && !guide.getGuideContent().trim().isEmpty()) {
        try {
            GuideContentData content = objectMapper.readValue(
                guide.getGuideContent(), GuideContentData.class);
            
            // 각 단계별로 목차 항목 생성
            for (int i = 0; i < content.getSteps().size(); i++) {
                GuideStepData step = content.getSteps().get(i);
                
                // 단계 제목
                Paragraph stepTitle = new Paragraph(String.format("%d. %s", 
                        step.getStepNumber(), step.getStepTitle()))
                    .setFont(koreanBoldFont)
                    .setFontSize(18)
                    .setBold()
                    .setMarginLeft(60)
                    .setMarginTop(20)
                    .setMarginBottom(10);
                document.add(stepTitle);
                
                // 하위 항목들
                if (step.getItems() != null) {
                    for (GuideItemData item : step.getItems()) {
                        Paragraph itemPara = new Paragraph("  • " + item.getTitle())
                            .setFont(koreanFont)
                            .setFontSize(13)
                            .setMarginLeft(80)
                            .setMarginBottom(5);
                        document.add(itemPara);
                    }
                }
            }
        } catch (Exception e) {
            log.error("목차 생성 실패", e);
        }
    }
    
    document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
}




}