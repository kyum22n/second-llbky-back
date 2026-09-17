# 인계 문서 — 네이버 검색/데이터랩 API 401 원인 및 후속 작업

> 작성 시점: 세션 토큰 만료 임박으로 중단, 약 4시간 후 재개 예정. 이 문서 하나로 다음 세션이 컨텍스트 없이 이어갈 수 있도록 결론·근거·다음 할 일을 정리했다.

## 결론 (재확인 완료)

**자격증명(Client ID/Secret) 값 자체의 문제가 아니라, 코드가 호출하는 API 엔드포인트/인증 헤더가 네이버의 구(舊) Developers Center 방식으로 되어 있는데, 실제 발급받은 키는 신(新) NAVER API HUB(네이버클라우드플랫폼) 방식이라 서로 호환되지 않아 발생하는 401이다.** → **코드 수정이 필요한 것이 맞음.**

### 근거
- 사용자 확인: Naver Developers 콘솔에서 검색/데이터랩 API 둘 다 등록돼 있었고, "최근 네이버 개발자센터의 검색/데이터랩 API가 **네이버 API Hub**로 옮겨가서 그 플랫폼에 앱을 새로 등록하고 키를 발급받았다"고 확인. 그 키가 `.env.local`/Render 양쪽에 등록된 것과 동일함.
- [NAVER API HUB 이관 가이드](https://guide.ncloud-docs.com/docs/apihub-migration)(공식 문서, WebFetch로 확인)에 명시된 변경 사항:

| 항목 | 기존(Developers Center, 현재 코드가 쓰는 방식) | 신규(API HUB, 사용자가 실제 발급받은 키의 방식) |
|---|---|---|
| 도메인 | `https://openapi.naver.com` | `https://naverapihub.apigw.ntruss.com` |
| 경로 예시(뉴스 검색) | `/v1/search/news.json` | `/search/v1/news` |
| Client ID 헤더 | `X-Naver-Client-Id` | `X-NCP-APIGW-API-KEY-ID` |
| Client Secret 헤더 | `X-Naver-Client-Secret` | `X-NCP-APIGW-API-KEY` |
| 데이터랩 인증 | (동일하게 `X-Naver-Client-*` 사용 중) | **`x-api-key`** 단일 헤더 방식일 가능성 (검색 웹검색 결과 스니펫에서 언급됨, **공식 문서로 최종 확인 필요** — 아래 "다음 할 일" 참조) |

- 가이드 원문: "**기존 Developers Center의 인증 정보는 API HUB에서 사용 불가능합니다.**" → 지금 코드가 구방식 도메인/헤더로 신규 키를 보내고 있으니 401이 나는 게 논리적으로 당연함. 실제로 로컬(.env.local)과 Render 양쪽에서 동일하게 401 재현 확인됨(이전 세션에서 검증).

## 영향받는 코드 (전부 확인 완료, 3개 파일)

1. **`src/main/java/com/example/demo/newstrend/service/NewsCollectorService.java`**
   - 39-50행: `baseUrl("https://openapi.naver.com/v1/search")`
   - 56-76행: `getNaverNews()` — `.path("/news.json")`, 헤더 `X-Naver-Client-Id`/`X-Naver-Client-Secret`
   - 용도: 뉴스 수집(`/trend/news/*` 계열 기능의 원천 데이터)

2. **`src/main/java/com/example/demo/ai/interview/CompanySearchAgent.java`**
   - 22-26행: `@Value` 필드
   - 37-39행: `baseUrl("https://openapi.naver.com")`
   - 48-60행: `.path("/v1/search/webkr.json")`(웹문서 검색), 헤더 동일 패턴
   - 용도: 면접 기능의 "기업 검색"(`GET /interview/search`)

3. **`src/main/java/com/example/demo/ai/newstrend/TrendDataAgent.java`**
   - 56-63행: `@Value` 필드 + `naver.datalab.trend.url`(`application.properties:31`, `https://openapi.naver.com/v1/datalab/search`)
   - 79-86행: `getTrendData()` — POST 요청, 헤더 `X-Naver-Client-Id`/`X-Naver-Client-Secret`
   - 용도: 트렌드 분석(`GET /trend/today`)의 검색어 트렌드 데이터

공통: `application.properties`의 `naver.api.client-id=${NAVER_CLIENT_ID}` / `naver.api.client-secret=${NAVER_CLIENT_SECRET}` 프로퍼티명은 그대로 재사용 가능(환경변수 이름을 바꿀 필요는 없음 — 값은 이미 API HUB에서 발급받은 것이므로).

## 예상 소요 시간

**총 1~2시간** 정도로 예상:
- **30~45분**: NAVER API HUB 공식 문서에서 검색(뉴스/웹문서) API와 데이터랩 API 각각의 정확한 경로·쿼리 파라미터·응답 JSON 구조 확인. 특히 데이터랩의 인증 헤더가 검색 API와 같은 `X-NCP-APIGW-API-KEY-ID/KEY` 방식인지, 웹 검색 스니펫에 나온 `x-api-key` 단일 헤더 방식인지는 **아직 미확정** — 반드시 공식 문서로 재확인 필요(추측으로 코드 작성 시 또 401 날 위험).
- **30~45분**: 3개 파일의 도메인/경로/헤더 수정, 응답 파싱 로직이 기존과 다르면 함께 수정.
- **15~30분**: 로컬 재구동 후 실제 API 호출로 재검증(아래 "재개 시 실행 절차" 참고).

## 수정 범위 (예상)

- **코드만 수정, 스키마/DB/프론트엔드 변경 없음** (프론트는 이미 이 API들을 정상 호출 중이며, 백엔드 응답 형태만 기존과 동일하게 맞추면 프론트 수정 불필요 — 단, 네이버 응답 JSON 구조 자체가 바뀌었다면 백엔드의 파싱 로직도 함께 손봐야 함. 이번 조사에서는 응답 구조 차이 여부까지는 확인하지 못함).
- 환경변수(`NAVER_CLIENT_ID`/`NAVER_CLIENT_SECRET`) 이름 변경 불필요, 값도 이미 올바른 것으로 확인됨(문제는 코드가 그 값을 잘못된 방식으로 사용하고 있는 것).

## NEWS_API_KEY / GOOGLE_SEARCH_API_KEY / GOOGLE_SEARCH_ENGINE_ID 확인 결과

**결론: 셋 다 코드에서 전혀 사용되지 않는 죽은 설정값이라, 비어있어도 현재 운영에 영향 없음.**

- `application.properties`에 프로퍼티 정의만 있음:
  - `35: newsapi.api.key=${NEWS_API_KEY}`
  - `38-40: google.search.endpoint=...`, `google.search.apiKey=${GOOGLE_SEARCH_API_KEY}`, `google.search.engineId=${GOOGLE_SEARCH_ENGINE_ID}`
- `src/main/java` 전체를 `newsapi`, `googleapis`, `GoogleSearch`, `NewsApi` 키워드로 검색한 결과 **일치하는 코드 없음** — 즉 이 프로퍼티들을 `@Value`로 주입받아 쓰는 클래스가 하나도 없음.
- 실제 뉴스/검색 기능은 전부 네이버 API(위 3개 파일)로만 동작하며, News API·Google Custom Search는 애초에 연동되지 않은 미완성/미사용 설정으로 보임.
- **따라서 이 값들이 비어있는 것은 버그가 아니고, 지금 조사 중인 401 문제와도 무관함.** (이전 세션의 `docs/deploy/deployment-report.md`에서도 "Render에 미등록"으로만 언급되었을 뿐, 별도 이슈로 취급되지 않았음 — 이번 확인으로 "미사용 확정"까지 결론지음.)

## 재개 시 실행 절차

1. 이 문서와 `docs/debug/debugging-plan.md` / `debugging-result.md`를 먼저 읽고 전체 맥락 파악.
2. NAVER API HUB 공식 문서(https://api.ncloud-docs.com/ 또는 https://guide.ncloud-docs.com/docs/apihub-migration 링크를 따라 검색 API/데이터랩 API 개별 레퍼런스 페이지)에서 정확한 스펙 확인.
3. 위 3개 파일 수정 (도메인/경로/헤더, 필요 시 응답 파싱).
4. 로컬 재구동:
   ```bash
   cd /c/kyum/project/second-llbky-back
   bash -c 'set -a; source .env.local; set +a; nohup ./gradlew bootRun > /tmp/backend.log 2>&1 & echo "PID:$!"'
   ```
5. 재검증:
   ```bash
   curl -s -w "\n%{http_code}\n" "http://localhost:8080/interview/search?query=네이버"
   curl -s -w "\n%{http_code}\n" "http://localhost:8080/trend/news/search?keywords=백엔드&memberId=5&period=month&limit=10"
   curl -s -w "\n%{http_code}\n" "http://localhost:8080/trend/today?memberId=5"
   ```
   세 요청 모두 200이면서 실제 데이터가 채워지는지(단순 200/success인데 내부 데이터가 0건인 것은 이전에도 나왔던 패턴이므로, 반드시 응답 바디의 실제 내용까지 확인할 것) 확인.
6. 확인되면 `docs/debug/debugging-result.md`에 이번 수정 내용과 재검증 결과를 추가로 기록(네이버 관련 항목의 O/X를 갱신).

## 현재 세션 상태

- 이전에 승인된 P0~P3 수정 6개 파일은 모두 완료·검증되어 디스크에 반영되어 있음(재부팅에도 유지됨 확인됨).
- 로컬 테스트용 백엔드/프론트엔드 프로세스는 세션 종료 전 정리(종료)함 — 재개 시 위 4번처럼 다시 구동 필요.
- 테스트 계정: `loginId: debugtest01` / `password: Test1234!` (memberId=5, 로컬 Neon DB에 생성됨, 이력서/자소서/로드맵 등 테스트 데이터 일부 존재).
