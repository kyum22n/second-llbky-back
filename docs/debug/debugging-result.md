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
| 포트폴리오 가이드 | 생성/저장/PDF 다운로드 | O | `portfolio_standard` 시드 데이터 삽입(2026-09-18) 후 배포 백엔드에서 `POST /portfolio-guide/create`→200(guideId 생성), `PUT /portfolio-guide/save`→200, `GET /portfolio-guide/{id}/pdf`→200(유효 PDF 7페이지) 전 과정 정상 확인 |
| 포트폴리오 가이드 | 전체 삭제(테이블 정정, P0-3 핵심) | O | 코드 리뷰로 `portfolio_guide` 대상 확인 + 삭제 API 정상 호출(200, 크래시 없음) 확인. 로컬에 시드 데이터가 없어 실제 행 삭제 전/후 비교까지는 못했으나 SQL 수정은 명확함 |
| 트렌드 | 트렌드 분석(P2 핵심) | O | 수정 전 500 → 수정 후 200, JSON 파싱 정상 완료 |
| 뉴스 | 오늘 뉴스/검색(네이버 API Hub 이관, P4 핵심) | O | 2026-09-18 네이버 API Hub 마이그레이션 수정 후 `GET /trend/today`, `GET /trend/news/search` 모두 200 + 실제 뉴스·검색량 데이터 정상 반환 확인 (기존 "0건" 데이터 이슈 해소) |
| 뉴스 | 키워드 저장/조회 | O | `POST /keyword/create`, `GET /keyword/list` 200 |
| 면접 | 회사(웹문서) 검색(네이버 API Hub 이관, P4 핵심) | O | `GET /interview/search` — API Hub 콘솔에 웹문서 검색 API 추가 신청 후 200, `["네이버"]` 정상 반환 확인 |
| 이력서 | 생성/코칭/리포트 | O | `/resume/create`, `/coach`, `/report/{id}` 200 |
| 프론트 | `/trend/news` 화면 정상 진입(P3) | O | 주석 삭제 후 기존과 동일하게 정상 렌더링, 콘솔 에러 없음 |
| 연동 | 브라우저에서 LLM 호출 정상 완료 | O | 자기소개서 코칭·면접 질문 생성·트렌드 분석 모두 수정 후 정상 완료 확인 |

## 4. 남은 이슈 (이번 범위 밖, 사용자 확인/조치 필요)

- ~~네이버 API 자격증명 문제~~ / ~~뉴스 요약 로드 실패(응답 지연)~~ → **배포 환경에서 최종 확인 완료(2026-09-18)**. 자격증명 값 자체는 문제가 없었고, 네이버 API Hub 마이그레이션에 따른 코드 측 도메인/헤더 불일치가 근본 원인이었음. 마이그레이션 수정 배포 후 응답 지연 문제가 추가로 발견되어 자동 수집 건수 제한까지 배포 완료했고, 사용자가 실제 배포 프론트엔드에서 뉴스 요약이 정상 동작함을 확인함. 상세 경과는 아래 5번 참고.
- ~~자기소개서 문체 생성/포트폴리오 등록(PDF 업로드)~~ → **2026-09-18 재검증 완료, 정상 동작 확인.** 이전 회귀에서는 curl 테스트 방식의 문제(파라미터 값, 타임아웃)로 미검증 처리됐던 것으로, 실제 코드 결함은 없었음.
- ~~**포트폴리오 가이드 생성/코칭/PDF**~~ → **해결 완료(2026-09-18)**. 원인은 `portfolio_standard`(평가 기준 템플릿) 테이블이 로컬뿐 아니라 Render(운영) DB에서도 완전히 비어있었기 때문(등록 API가 없어 코드로는 채울 수 없었음). 사용자가 Neon 콘솔에서 직접 기본 평가 기준 1건을 삽입한 뒤, 배포 환경에서 가이드 생성→저장→PDF 다운로드 전 과정을 재검증해 정상 동작 확인함 — 상세 경과는 아래 6번 참고.
- **면접 답변의 실제 음성 STT/비언어 분석 정확도**: 파이프라인 자체(제출→피드백 생성)는 크래시 없이 정상 동작함을 확인했으나, 로컬에 테스트용 음성/영상 샘플이 없어 실제 인식 정확도는 검증하지 못했습니다. 필요 시 브라우저에서 실제 마이크/카메라로 한 번 더 확인하는 것을 권장합니다.

## 5. 배포 후 추가 발견 — 뉴스 요약 로드 실패 (2026-09-18)

네이버 API Hub 마이그레이션을 배포한 뒤 사용자가 실제 배포 프론트엔드에서 "뉴스 요약"을 시도했더니 로드에 실패하는 새로운 증상이 발견되어 조사·수정했습니다.

**원인**: `GET /trend/news/today`([TotalNewsService.getTodayNewsByMember](src/main/java/com/example/demo/newstrend/service/TotalNewsService.java:386))는 오늘 뉴스가 없으면 요청-응답 사이에서 **동기적으로** 최대 50건(부족 시 추가로 50건, 총 최대 100건)의 기사를 수집합니다. 기사 1건마다 네이버 검색 + 관련성 판정(LLM) + 요약/감정분석(LLM) + 키워드추출(LLM)이 순차적으로 발생해, 기사당 수 초~수십 초가 걸립니다.

- **마이그레이션 이전**: 네이버 API가 401로 즉시 실패 → 수집이 매번 빠르게 "0건"으로 끝나 응답이 빨랐음(단, 데이터는 항상 비어 있었음 — 이 때문에 이 성능 문제가 지금까지 가려져 있었음).
- **마이그레이션 이후**: 네이버 API가 실제로 동작하면서 최대 100건을 순차 처리하게 되어 응답 시간이 수 분으로 늘어남 → 배포 프론트에서 `fetch`가 무한정 응답을 기다리다 사실상 멈춘 것처럼 보임(45초 이상 응답 없음을 브라우저에서 직접 재현·확인). 배포 백엔드에 curl로 직접 요청해도 90초가 넘도록 응답이 오지 않아 동일하게 재현됨.
- **부수 확인**: 프론트엔드(`second-llbky-front`)에는 이미 SSE 스트리밍용 `streamTodayNews()`(`src/apis/newsApi.js`)가 구현돼 있으나, 실제 화면(`src/utils/news.js`)은 이를 사용하지 않고 blocking 방식인 `getTodayNews()`를 호출하고 있음 — 스트리밍으로 전환하려다 중간에 멈춘 것으로 보이나, 이번에는 최소 범위 수정만 진행하고 구조 변경은 하지 않음(사용자 확인).

**수정(최소 범위, 사용자 승인)**: `TotalNewsService.getTodayNewsByMember`에서 자동 수집 시 사용하는 건수 상한을 `min(요청 limit, 5)`로 제한(`src/main/java/com/example/demo/newstrend/service/TotalNewsService.java:410`). 구조는 그대로 두고 한 번에 처리하는 기사 수만 줄여 응답 시간을 단축.

**검증**: 완전히 새로운 회원(뉴스 데이터 0건, memberId=8)으로 로컬에서 `GET /trend/news/today` 재호출 → 이전에는 90초 이상 응답 없음 → 수정 후 **1분 5초 만에 200 정상 응답, 뉴스 5건 반환**. 배포 후 사용자가 실제 배포 프론트엔드에서 재확인 → **정상 동작 확인 완료(2026-09-18).**

**남은 근본적 제안(이번 범위 밖)**: 이 구조는 여전히 요청-응답을 막는(blocking) 동기 처리이므로, 건수를 줄인 것은 임시방편에 가깝습니다. 장기적으로는 이미 준비된 SSE 스트리밍(`streamTodayNews`)으로 프론트를 전환하거나, 수집을 비동기 백그라운드 작업으로 분리하는 것을 권장합니다.

## 6. 포트폴리오 분석/가이드 문제 조사 (2026-09-18)

배포 후 사용자가 실제로 "포트폴리오 분석"과 "포트폴리오 작성 도우미(가이드)"를 사용해보니 각각 다른 원인으로 실패하는 것을 발견하여 조사했습니다.

### 6-1. 포트폴리오 분석 실패 (원인 분석 — 해결은 7-1 참고)

**원인**: [PortfolioPageAnalysisAgent.java:217](src/main/java/com/example/demo/ai/portfolio/PortfolioPageAnalysisAgent.java:217)에서 PDF 페이지마다 비전 LLM 호출 후 `Thread.sleep(5000)`(5초 고정 대기)이 걸려 있습니다. 페이지 1개당 "비전 LLM 분석 시간(수 초~수십 초) + 5초 고정 대기"가 순차적으로 발생하며, 이후 `generateSummary()`(최종 요약, 추가 LLM 호출 1회)까지 이어집니다.

- 로컬 테스트에서는 1페이지짜리 PDF로도 처리에 3~4분이 걸렸음(이전 세션 기록).
- 실제 사용자의 포트폴리오는 보통 10~20페이지 이상이므로, 단순 계산으로도 5~15분 이상 걸릴 수 있어 브라우저/Render 게이트웨이 타임아웃으로 실패하는 것으로 판단됩니다. `/trend/news/today`와 동일한 유형의 "동기 처리 시간 초과" 문제입니다.

**중요한 정정**: 처음에는 `Thread.sleep(5000)`을 줄이거나 없애는 것을 해결방안으로 제안했으나, 이는 **틀린 진단이었습니다.** 사용자 확인 결과 이 sleep은 OpenAI 분당 토큰(TPM) 제한 회피를 위해 의도적으로 넣은 것이며, 페이지 1개당 5초는 전체 소요 시간(로컬 1페이지 테스트 기준 3~4분) 중 극히 일부에 불과해 sleep을 없애도 타임아웃 문제 자체는 해결되지 않고, 오히려 sleep이 막으려던 rate-limit 문제가 재발할 위험이 있습니다. **sleep은 건드리지 않고, 요청-응답을 막는 동기 구조 자체를 비동기로 전환하는 방식으로 해결했습니다 — 7-1 참고.**

### 6-2. 포트폴리오 작성 도우미 — 저장/가이드 생성/PDF 다운로드 전부 실패 (원인 특정 완료 — 해결책은 승인 필요)

**원인**: `portfolio_standard`(포트폴리오 평가 기준) 테이블이 **로컬뿐 아니라 배포(Render/Neon) DB에서도 완전히 비어있음**을 직접 확인했습니다(`curl https://second-llbky-back.onrender.com/portfolio-standard` → `[]`).

프론트엔드가 가이드를 생성할 때 `standardId: 1`을 하드코딩해서 보내는데([portfolioStepbystep.js:1028](src/utils/portfolioStepbystep.js:1028) — `second-llbky-front` 저장소), `portfolio_guide.standard_id`는 `portfolio_standard(standard_id)`를 참조하는 **NOT NULL 외래키**([schema.sql:207-209](docs/db/schema.sql:207)) 입니다. `portfolio_standard`에 `standard_id=1` 행이 존재하지 않으므로, 가이드 생성(`POST /portfolio-guide/create`)이 항상 외래키 위반으로 500 실패합니다. 배포 백엔드에 동일한 요청을 직접 보내 500을 재현·확인했습니다.

이 실패가 다음 세 가지 증상을 전부 설명합니다:
- **"가이드 생성 실패" 알림**: `createGuide()` 실패 → `guideId`가 끝내 null로 남음.
- **저장 버튼 클릭 시 실패**: `saveManually()`는 가이드가 없으면 먼저 `createGuide()`를 호출하는데, 이게 항상 실패하므로 저장도 항상 실패.
- **PDF 다운로드 안 됨**: `downloadPortfolioPdf()`도 `guideId`가 있어야 동작하는데, 애초에 가이드가 생성된 적이 없어 다운로드까지 가지도 못함(모든 단계를 작성해도 마찬가지로 실패).

**해결(완료, 2026-09-18)**: `portfolio_standard`에 최소 1건의 평가 기준 데이터가 필요했습니다. 이 테이블은 조회 전용 API만 있고 등록/삽입 API가 전혀 없어(관리자 기능 부재) 코드 경로로는 채울 수 없었습니다.

- 임시 JUnit 테스트로 운영과 공유하는 Neon DB에 직접 삽입을 시도했으나, **운영 공유 자원에 쓰기 작업이라 세션의 자동 승인 정책에 의해 하드 차단됨**(사용자의 채팅 내 승인과 무관하게 harness 레벨에서 막힘). 이 세션에서는 실행 불가로 판단, 임시 테스트 파일은 삭제함.
- 대신 사용자가 **Neon 콘솔의 SQL Editor에서 직접 INSERT 문을 실행**(`standard_id`를 지정하지 않아 SERIAL이 자동으로 `1`을 부여하도록 함 — 프론트엔드가 하드코딩한 `standardId: 1`과 정확히 일치): `standard_name="종합 포트폴리오 평가 기준"`, 직군/직무 무관(모든 회원 공통 적용), 구조/가독성/직무 연관성/성과 구체성을 평가 기준으로 하는 프롬프트 템플릿 1건.
- **검증**: `GET /portfolio-standard` → `standardId: 1` 데이터 정상 확인. 이어서 배포 백엔드에 직접 `POST /portfolio-guide/create` → 200(`guideId: 10` 생성), `PUT /portfolio-guide/save` → 200(저장 정상), `GET /portfolio-guide/10/pdf` → 200(유효한 PDF 7페이지, 71KB 다운로드 확인). **가이드 생성·저장·PDF 다운로드 전 과정이 배포 환경에서 정상 동작함을 확인했습니다.**
- 근본적으로는 이 테이블을 관리할 수 있는 등록/수정 API(또는 최소한 시드 SQL 스크립트를 리포지토리에 포함)를 추가하는 것을 권장합니다 — 이후 7-2에서 직군별 10건을 추가로 삽입함.

### 6-3. 포트폴리오 작성 도우미 UI 변경 (1차, 이후 7-3에서 되돌림)

사용자 요청에 따라 수동 저장 버튼을 제거하고 안내 문구로 대체했었으나("모든 단계를 작성해야 다운로드할 수 있습니다"), 저장이 실제로 됐는지 확인할 방법이 없다는 문제가 있어 이후 7-3에서 버튼을 복구함.

## 7. 비동기 전환, 직군별 기준 데이터, 가이드 저장 버튼 복구 (2026-09-18, 2차)

### 7-1. 포트폴리오 분석 비동기 전환 (적용 완료)

`Thread.sleep(5000)`은 그대로 두고, "요청-응답을 막는 동기 구조" 자체를 비동기로 전환했습니다. SSE는 이 리포지토리에 관련 인프라가 전혀 없어(뉴스 기능의 `streamTodayNews()`는 프론트 전용 미사용 스텁, 대응 백엔드 엔드포인트 없음) 새로 구축하는 대신, 이미 존재하는 조회 API를 프론트에서 폴링하는 방식을 채택했습니다.

- **신규**: `src/main/java/com/example/demo/config/AsyncConfig.java` — `@EnableAsync` + 전용 스레드풀(`portfolioAnalysisExecutor`, core 1/max 2, LLM rate limit 때문에 병렬로 늘려도 의미 없음).
- **`PortfolioService.java`**: `runAnalysisAsync(portfolioId)` 신규 — `@Async("portfolioAnalysisExecutor")`. 기존 `analyzePortfolio()` → `generateSummary()`를 순서·로직 변경 없이 그대로 호출, 예외는 내부에서 로깅만(비동기 메서드라 호출자에게 전파되지 않음).
- **`PortfolioController.createPortfolio()`**: PDF 저장(빠른 동기 처리)만 하고 `runAnalysisAsync()`를 호출한 뒤 `{portfolioId}`만 즉시 응답. 프론트가 이미 `res.data.portfolioId`만 사용하고 있어 응답 축소가 프론트에 영향 없음(코드 확인 완료).
- **`second-llbky-front/src/utils/portfolioCoach.js`**: `loadPortfolio()`를 "PDF 이미지 로딩"과 "분석 결과 폴링(5초 간격, 최대 10분)"으로 나누어 **병렬** 실행하도록 재작성. `GET /portfolio/{id}`의 `portfolioFeedback`이 채워지면 완료로 간주하고 폴링 중단.
  - 최초 구현 시 두 작업을 순차로 짜는 실수가 있었음(PDF 렌더링이 끝나야 폴링이 시작됨) → PDF 렌더링이 느리거나 멈추면 진행률 폴링 자체가 시작되지 않는 버그를 브라우저 테스트로 발견, 즉시 병렬 구조로 수정.
- **`second-llbky-front/src/views/resume/PortfolioCoach.vue`**: "N / 전체 페이지 분석 완료" 진행률 안내와 타임아웃 안내 UI 추가.
- **검증**: `POST /portfolio/create` 응답 시간이 **3~4분 → 1.6초**로 단축(로컬, 1페이지 PDF 기준). 브라우저로 실제 코칭 화면에 진입해 "AI가 포트폴리오를 분석하고 있습니다 (0/1 페이지 완료)" 진행 상태 표시 → 백엔드 분석 완료 후 폴링으로 페이지별 분석 결과와 전체 요약(종합 점수 60/100 등)이 정상적으로 채워지는 것까지 확인. 기존(비동기 전환 이전)에 이미 분석이 끝나있던 포트폴리오(id=2)도 회귀 없이 정상 표시됨을 확인.
- **부수 발견(별건, 미수정)**: 코칭 화면 왼쪽의 PDF 페이지 이미지 미리보기가 "PDF 렌더링 중..."에서 멈추는 현상을 발견함. 콘솔에 pdf.js의 "Setting up fake worker" 경고가 뜨는 것으로 보아 pdf.js 워커 로딩 문제로 추정되나, 이번 변경과는 무관한 기존 코드(`src/utils/pdfRenderer.js`)의 문제이고 실제 분석 결과(페이지별 피드백·종합 분석)는 정상 표시되므로 별도 이슈로 분리해 보고만 함 — 필요 시 별도로 조사 권장.

### 7-2. `portfolio_standard` 직군별 데이터 10건 추가 (적용 완료)

`docs/db/seed-portfolio-standard.sql`에 프론트엔드 회원가입 폼([SignupView.vue](../../../second-llbky-front/src/views/SignupView.vue)의 `jobRolesData`)에 하드코딩된 10개 직군(개발/디자인/기획/PM/마케팅/AI·데이터/영업/경영/교육/기타)에 대응하는 평가 기준을 작성, 사용자가 Neon 콘솔에서 직접 실행해 삽입함.

### 7-3. 하드코딩된 `standardId: 1` 문제 발견 및 근본 수정 (적용 완료)

7-2 삽입 후 `portfolio_standard`를 재조회한 결과, **6-2에서 삽입했던 공통 기준(`standard_id=1`)이 사라져 있었고, 직군별 10건은 `standard_id=2~11`로 채번됨을 발견했습니다.** `portfolio_guide.standard_id`는 `ON DELETE CASCADE` 외래키라, id=1이 삭제되면서 그걸 참조하던 6-2 검증용 가이드(`guideId=10`)도 함께 삭제된 것을 확인함(`GET /portfolio-guide/10` → 404).

프론트엔드가 여전히 `standardId: 1`을 하드코딩하고 있었다면 **가이드 생성이 다시 예전과 동일하게 실패했을 것**입니다. 근본 수정으로, `second-llbky-front/src/utils/portfolioStepbystep.js`의 `createGuide()`가 더 이상 고정 ID를 쓰지 않고, 이미 존재하던 `portfolioGuideApi.getStandardsByJob(jobGroup, jobRole)`(직군 기준 조회) → 실패 시 `getAllStandards()`(전체 기준 중 첫 번째) 순서로 실제 사용 가능한 `standardId`를 조회해서 사용하도록 변경. 이제 어떤 특정 ID가 삭제/변경되어도 가이드 생성이 깨지지 않음.

### 7-4. 포트폴리오 가이드 저장 버튼 복구 (적용 완료)

6-3에서 제거했던 수동 저장 버튼을 복구함(`src/views/resume/PortfolioStepbystep.vue`). "모든 단계를 작성해야 다운로드 가능" 안내는 이미 다운로드 버튼 근처에 있던 기존 경고 문구("완전하지 않은 상태로 PDF 생성 시 일부 내용이 비어있을 수 있습니다")가 같은 역할을 하고 있어 별도로 추가하지 않음.

**브라우저 End-to-End 검증(로컬, debugtest01 계정)**:
1. `/resume/portfolio/stepbystep` 진입 → 회원의 직군(`개발`)에 맞는 평가 기준으로 가이드 자동 생성 확인(콘솔: `✅ 가이드 생성 완료: 14`, FK 에러 없음).
2. 1단계 항목에 내용 입력 후 **저장 버튼 클릭** → 화면에 "방금 저장됨" 표시, 백엔드 로그에서 `UPDATE portfolio_guide SET guide_content = ...` 쿼리로 실제 반영 확인.
3. **PDF 다운로드 버튼 클릭** → "✅ PDF 다운로드 완료!" 모달 정상 표시, 백엔드 로그에서 `PortfolioGuidePdfService.generateGuidePdf()` 정상 실행 확인. 더 이상 "가이드가 생성되지 않았다" 팝업 발생하지 않음.

### 7-5. 최종 결론

포트폴리오 분석(비동기 전환으로 타임아웃 해소) · 포트폴리오 가이드(저장 버튼 복구 + standardId 하드코딩 근본 수정으로 생성/저장/다운로드 전부 정상화) 모두 로컬에서 End-to-End로 검증 완료. 사용자 승인 후 배포하여 실제 Render 환경에서도 동일하게 재확인 필요.
