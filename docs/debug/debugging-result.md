# DB 재연동/배포 후 발생한 문제 디버깅 결과

> [debugging-plan.md](debugging-plan.md)에서 계획한 수정 사항의 실제 적용 내용과 검증 결과입니다. 모든 검증은 로컬 환경(`.env.local`의 실제 Neon DB / OpenAI / Naver 키 사용, 백엔드 `localhost:8080`, 프론트엔드 `localhost:80`)에서 실제 API 호출·브라우저 조작으로 수행했습니다.

## 1. 수정 내용 (변경 전 → 후)

### 백엔드 (second-llbky-back)

| 항목 | 파일 | 변경 내용 |
|---|---|---|
| P0-1 | `src/main/java/com/example/demo/member/service/MemberService.java` | 로그인 실패 시 `RuntimeException` → `NoSuchElementException`(아이디 없음, 404) / `IllegalArgumentException`(비밀번호 불일치, 400)로 교체 |
| P0-2 | `src/main/resources/mapper/interviewQuestion.xml` | `insertCustomQuestion`에서 컬럼 목록의 `created_at` 제거(값 개수와 일치시킴), 형제 구문과 동일하게 `useGeneratedKeys`/`keyProperty` 추가 |
| P0-3 | `src/main/resources/mapper/portfolioguide.xml` | `deleteAllGuides`의 삭제 대상 테이블을 `news_summary` → `portfolio_guide`로 정정 |
| P1-2 | `src/main/java/com/example/demo/ai/learning/RewriteMemoAgent.java` | `learningDayDao.update(day)` 호출을 valid(검증 통과) 분기 안으로 이동 — 검증 실패 시 DB에 반영되지 않도록 수정 |
| P1-4 | `src/main/java/com/example/demo/interview/service/InterviewService.java`, `src/main/java/com/example/demo/ai/interview/CreateQuestionAgent.java` | `createAiQuestion`에서 `targetCompany`가 있으면 `companyIdealTalentAgent`로 인재상/핵심가치를 사전 조회해 `CreateQuestionAgent.createQuestion(request, companyIdealTalent)`로 전달, 질문 생성 프롬프트에 `[기업 인재상/핵심가치]` 섹션 추가. 조회 실패는 로그만 남기고 질문 생성은 계속 진행 |
| P2 | `src/main/java/com/example/demo/ai/newstrend/SentimetalAnalysisAgent.java` | 시스템 프롬프트에 누락되어 있던 `%s` 자리표시자(및 "출력 형식:" 안내문) 추가 — BeanOutputConverter의 JSON 강제 지시문이 실제로 프롬프트에 삽입되도록 수정 |
| P4(신규) | `src/main/java/com/example/demo/newstrend/service/NewsCollectorService.java`, `src/main/java/com/example/demo/ai/interview/CompanySearchAgent.java`, `src/main/java/com/example/demo/ai/newstrend/TrendDataAgent.java`, `src/main/resources/application.properties` | **네이버 API Hub 마이그레이션**: 네이버가 검색/데이터랩 API를 구(舊) Developers Center에서 신(新) NAVER API HUB(네이버클라우드플랫폼)로 이관하면서 도메인·경로·인증 헤더가 모두 변경됨. `https://openapi.naver.com` → `https://naverapihub.apigw.ntruss.com`, 헤더 `X-Naver-Client-Id/Secret` → `X-NCP-APIGW-API-KEY-ID/KEY`로 전체 교체. `CompanySearchAgent`는 경로를 `/v1/search/webkr`(404)에서 뉴스 검색과 동일한 `/search/v1/webkr` 패턴으로 재수정. 사용자가 Naver API Hub 콘솔에서 웹문서 검색 API 상품을 추가 신청한 뒤 최종 200 확인 |

### 프론트엔드 (second-llbky-front)

| 항목 | 파일 | 변경 내용 |
|---|---|---|
| P2 | `src/utils/interviewMock.js` | `generateQuestions()`에서 PDF 미첨부 시 `alert` 후 즉시 종료하던 필수 검증 제거. PDF는 첨부된 경우에만 `FormData`에 포함하도록 변경(백엔드는 이미 `documentFile`을 선택 파라미터로 받음) |
| P3 | `src/utils/news.js` | 실제와 맞지 않는 낡은 FIXME 주석 한 줄 삭제(로직 변경 없음, 기존 코드는 이미 Vuex 스토어의 실제 memberId를 사용 중이었음) |

전체 diff는 각 저장소에서 `git diff`로 확인 가능합니다.

## 2. P2 재현·원인·검증 상세

1. **최초 가설(CORS 포트 불일치) 기각**: 로컬 프론트 dev 서버가 정확히 `http://localhost`(포트 80)로 기동되어 `CorsConfig.allowedOrigins("http://localhost")`와 정확히 일치함을 확인. 자기소개서 실시간 코칭(`POST /coverletter/realtime-coach`)을 브라우저에서 직접 호출해 OPTIONS 200 → POST 200, 실제 AI 피드백(JSON)이 정상 반환됨을 확인 — CORS/OpenAI 연동 자체는 정상.
2. **모의 면접 질문 생성 0건 재현**: "AI 예상 면접 질문 생성하기" 버튼 클릭 시 Network 탭에 요청이 전혀 기록되지 않음을 확인. 콘솔에서 `면접 질문 생성을 위한 PDF 파일을 업로드해주세요.` alert 확인 → `src/utils/interviewMock.js:101-105`의 필수 검증이 원인으로 특정.
3. **수정 후 재검증**: PDF 없이 "AI 예상 면접 질문 생성하기" 실행 → `POST /interview/ai-questions` 200, 5개 질문 정상 생성 확인.
4. **트렌드 분석 500 재현**: `GET /trend/today` 호출 시 500, 응답 바디에서 `JsonParseException: Unrecognized token '주어진'` 확인 → 스택트레이스로 `SentimetalAnalysisAgent.excute()` 특정.
5. **원인 특정**: `SentimetalAnalysisAgent.java`만 유일하게 `.formatted(format)` 호출 시 문자열에 `%s`가 없어 BeanOutputConverter의 JSON 강제 지시문이 프롬프트에서 누락됨(다른 모든 Agent는 `%s`를 정상 포함).
6. **연쇄 원인(별도 확인, 코드 수정 범위 아님)**: 네이버 뉴스/데이터랩 API가 `401 Unauthorized` 반환 → 뉴스가 하나도 수집되지 않아 감정 분석 입력이 비어있는 상태에서 LLM이 자연어로 응답("주어진 뉴스가 없어서...")하며 파싱 실패를 유발한 것으로 추정. `.env.local`/Render 양쪽에 동일하게 등록된 `NAVER_CLIENT_SECRET`의 길이(40자)가 `NAVER_CLIENT_ID`(10자)와 형태가 달라 정상적인 네이버 자격증명이 아닐 가능성이 높음 — Naver Developers 콘솔에서의 직접 재확인이 필요하다고 사용자에게 안내함(진행 중, 코드 수정 범위 밖).
7. **`%s` 수정 후 재검증**: `GET /trend/today` 재호출 → 200. 백엔드 로그에서 `SentimentResponse(...)`로 정상 파싱된 것을 확인(더 이상 JsonParseException 없음). 네이버 데이터가 비어있어 분석 결과 자체는 부정적/0에 가깝게 나오지만(데이터 품질 이슈, 별건), 크래시 없이 200으로 끝까지 완료됨을 확인.
8. **Render(배포 환경) 추가 확인**: `https://second-llbky-back.onrender.com/health`(200 UP), `/portfolio-standard`(200, DB 읽기)는 정상 — 배포 백엔드 자체와 DB 연결은 문제없음. 다만 `/interview/search`(네이버 검색 의존)는 당시 배포 환경에서도 500이었음 — 아래 9번 항목의 진짜 원인이 배포 환경에도 동일하게 존재했음을 의미.
9. **네이버 401의 진짜 원인 특정 및 해결(2026-09-18)**: 사용자가 Naver Developers 콘솔·API Hub 콘솔·자격증명 값을 직접 재확인한 결과, `.env.local`/Render의 `NAVER_CLIENT_ID`/`NAVER_CLIENT_SECRET` 값 자체는 문제가 없었고, **네이버가 검색/데이터랩 API를 구 Developers Center에서 신규 NAVER API HUB(네이버클라우드플랫폼)로 이관**하면서 발급 키의 인증 방식이 완전히 바뀐 것이 원인으로 확인됨(위 6-7번의 "자격증명 형태 이상" 추정은 오판이었고, 실제로는 코드가 구(舊) 방식 도메인/헤더로 신규 키를 호출한 것이 원인). 공식 이관 가이드(https://guide.ncloud-docs.com/docs/apihub-migration) 기준으로 3개 파일의 baseUrl/path/헤더를 신규 방식(`https://naverapihub.apigw.ntruss.com`, `X-NCP-APIGW-API-KEY-ID`/`X-NCP-APIGW-API-KEY`)으로 전면 수정.
   - 뉴스 검색(`NewsCollectorService`): `GET /trend/news/search` → **200**, 실제 뉴스 10건 수집·LLM 분석·DB 저장까지 정상 완료 확인.
   - 데이터랩 트렌드(`TrendDataAgent`, `GET /trend/today`): **200**, 키워드별 실제 검색량(`ratio`)·감정분석·마켓 인사이트까지 완전한 데이터로 정상 반환 확인.
   - 웹문서 검색(`CompanySearchAgent`, `GET /interview/search`): 경로를 뉴스 검색과 동일 패턴(`/search/v1/webkr`)으로 재수정 후에도 처음엔 401 — 원인은 네이버 API Hub 콘솔에 "웹문서 검색" API 상품이 별도 신청/구독되어 있지 않았기 때문으로 확인. 사용자가 콘솔에서 해당 API를 추가 신청한 뒤 재검증 → **200**, `["네이버"]` 정상 반환 확인.
   - **결론: 네이버 API 관련 3개 엔드포인트 모두 정상화 완료. `%s` 수정 이후에도 남아있던 "뉴스 0건이라 감정분석이 빈약하게 나오는" 데이터 품질 이슈도 이번 수정으로 함께 해소됨(이제 실제 뉴스가 정상 수집되므로).**

## 3. 전체 기능 회귀 테스트 결과 (O/X)

| 구분 | 테스트 항목 | 결과 | 비고 |
|---|---|---|---|
| 회원 | 회원가입 | O | `POST /member/register` 200 |
| 회원 | 로그인 성공 | O | `POST /member/login` 200 |
| 회원 | 로그인 실패(아이디 없음) | O | 200(500) → **404**로 정상 변경 확인 (P0-1) |
| 회원 | 로그인 실패(비밀번호 틀림) | O | **400**으로 정상 변경 확인 (P0-1) |
| 회원 | 회원정보 수정 | O | `PUT /member/update` 200 |
| 자기소개서 | 실시간 코칭 | O | `POST /coverletter/realtime-coach` 200, 실제 AI 피드백 JSON 확인 |
| 자기소개서 | 저장(최종 피드백 자동 생성 포함) | O | `POST /coverletter/save` 200, FinalFeedbackAgent 자동 호출 확인 |
| 자기소개서 | 목록/상세/삭제 | O | `/coverletter/list`, `/detail`, `/delete` 모두 200 |
| 자기소개서 | 문체 버전 생성 | O | `GET /coverletter/detail/styles`(section=supportMotive) 200, simple/case/vision 3가지 버전 정상 생성 확인 (2026-09-18 재검증) |
| 자기소개서 | 문체 버전 적용 | O | `PUT /coverletter/detail/styles/apply` 200, 재조회로 해당 section 내용이 실제로 교체됨을 확인 (2026-09-18) |
| 면접 | 예상 질문 생성(기업명 없이) | O | `POST /interview/ai-questions` 200, 5개 질문 생성 |
| 면접 | 예상 질문 생성(PDF 없이, P2 핵심) | O | 수정 전 요청 0건 → 수정 후 200 정상 생성 |
| 면접 | AI+커스텀 질문 병합 저장 (P0-2 관련) | O | `POST /interview/session-save` 200, 두 질문 모두 정상 저장 |
| 면접 | 답변 제출→AI 피드백 파이프라인 | O(부분) | `POST /interview/submit-answer`→`/create-feedback` 200, 크래시 없이 "무응답" 케이스를 논리적으로 처리(점수 0, 적절한 안내 메시지). 다만 로컬에 테스트용 음성/영상 샘플 파일이 없어 **실제 음성 STT 인식·비언어 분석 자체의 정확도까지는 미검증** — 브라우저에서 실제 마이크/카메라로 한 번 더 확인 권장 |
| 학습 | 로드맵 생성/저장 | O | `POST /learning/roadmap-create`, `/roadmap-save` 200, 4주 로드맵 정상 생성·저장 |
| 학습 | 정상 메모 제출(검증 통과) | O | 제출 후 상태 "완료", 정리된 요약으로 DB 반영 확인 |
| 학습 | 부적절 메모 제출(검증 실패, P1-2 핵심) | O | 응답에는 거부 안내 표시, **DB는 제출 전 상태로 완전히 그대로 유지**됨을 재조회로 확인 |
| 포트폴리오 | 목록 조회 | O | `GET /portfolio/list/5` 200 (의도적으로 `portfolio_id`/`title`/`updated_at`만 반환하는 경량 조회 — 다른 필드 null은 버그 아님) |
| 포트폴리오 | 등록(PDF 업로드) | O | `POST /portfolio/create` 200 (2026-09-18 재검증, 타임아웃을 넉넉히 잡아 재시도). 실제 인증서 PDF 1페이지를 업로드해 페이지별 비전 분석(`pageSummary`/`pageComment`)과 최종 요약(`finalScore`, 강점/약점 등)까지 전부 정상 반환 확인. 페이지당 이미지 분석 LLM 호출 때문에 처리에 3~4분 소요됨(지연이지 실패 아님) |
| 포트폴리오 가이드 | 생성/코칭/PDF | X(미검증) | 로컬 DB에 `portfolio_standard` 시드 데이터가 없어 생성 자체가 불가. 해당 테이블은 별도 관리자 등록 API가 없어(현재 GET 전용) 코드로 직접 채울 수 없음 — 로컬 psql 클라이언트도 없어 수동 삽입도 불가. 기능 자체의 회귀는 아니며 순수 로컬 데이터 부재가 원인 (2026-09-18 재확인, 이전과 동일한 결론) |
| 포트폴리오 가이드 | 전체 삭제(테이블 정정, P0-3 핵심) | O | 코드 리뷰로 `portfolio_guide` 대상 확인 + 삭제 API 정상 호출(200, 크래시 없음) 확인. 로컬에 시드 데이터가 없어 실제 행 삭제 전/후 비교까지는 못했으나 SQL 수정은 명확함 |
| 트렌드 | 트렌드 분석(P2 핵심) | O | 수정 전 500 → 수정 후 200, JSON 파싱 정상 완료 |
| 뉴스 | 오늘 뉴스/검색(네이버 API Hub 이관, P4 핵심) | O | 2026-09-18 네이버 API Hub 마이그레이션 수정 후 `GET /trend/today`, `GET /trend/news/search` 모두 200 + 실제 뉴스·검색량 데이터 정상 반환 확인 (기존 "0건" 데이터 이슈 해소) |
| 뉴스 | 키워드 저장/조회 | O | `POST /keyword/create`, `GET /keyword/list` 200 |
| 면접 | 회사(웹문서) 검색(네이버 API Hub 이관, P4 핵심) | O | `GET /interview/search` — API Hub 콘솔에 웹문서 검색 API 추가 신청 후 200, `["네이버"]` 정상 반환 확인 |
| 이력서 | 생성/코칭/리포트 | O | `/resume/create`, `/coach`, `/report/{id}` 200 |
| 프론트 | `/trend/news` 화면 정상 진입(P3) | O | 주석 삭제 후 기존과 동일하게 정상 렌더링, 콘솔 에러 없음 |
| 연동 | 브라우저에서 LLM 호출 정상 완료 | O | 자기소개서 코칭·면접 질문 생성·트렌드 분석 모두 수정 후 정상 완료 확인 |

## 4. 남은 이슈 (이번 범위 밖, 사용자 확인/조치 필요)

- ~~네이버 API 자격증명 문제~~ → **해결 완료(2026-09-18)**. 자격증명 값 자체는 문제가 없었고, 네이버 API Hub 마이그레이션에 따른 코드 측 도메인/헤더 불일치가 원인이었음. 3개 파일 수정 + 사용자의 API Hub 웹문서 검색 상품 추가 신청으로 완전히 해소, 로컬에서 재검증 완료. **Render(배포 환경)에도 동일한 환경변수(`NAVER_CLIENT_ID`/`NAVER_CLIENT_SECRET`)가 이미 등록되어 있으므로, 이번 코드 수정을 배포하면 배포 환경에서도 동일하게 정상화될 것으로 예상됨 — 배포 후 실제 재확인 권장.**
- ~~자기소개서 문체 생성/포트폴리오 등록(PDF 업로드)~~ → **2026-09-18 재검증 완료, 정상 동작 확인.** 이전 회귀에서는 curl 테스트 방식의 문제(파라미터 값, 타임아웃)로 미검증 처리됐던 것으로, 실제 코드 결함은 없었음.
- **포트폴리오 가이드 생성/코칭/PDF**: 로컬 DB에 `portfolio_standard`(평가 기준 템플릿) 시드 데이터가 없어 여전히 검증 불가. 이 테이블은 조회용 API만 있고 등록/삽입 API가 없어, Render(운영) DB에는 별도로 이미 데이터가 들어있는 것으로 추정됩니다 — 배포 환경에서는 정상 동작할 가능성이 높으나, 로컬 개발 검증을 위해서는 관리자용 등록 기능을 추가하거나 시드 SQL 스크립트를 마련하는 것을 권장합니다(이번 범위 밖 제안).
- **면접 답변의 실제 음성 STT/비언어 분석 정확도**: 파이프라인 자체(제출→피드백 생성)는 크래시 없이 정상 동작함을 확인했으나, 로컬에 테스트용 음성/영상 샘플이 없어 실제 인식 정확도는 검증하지 못했습니다. 필요 시 브라우저에서 실제 마이크/카메라로 한 번 더 확인하는 것을 권장합니다.
