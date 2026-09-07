# 배포 결과 검증 보고서

- **작성/갱신일**: 2026-09-04 (2026-09-07 재검증 — §9 참고)
- **대상**: `docs/prompts/db-deploy-setup-prompt.md` 2번 프롬프트(Vercel + Render + Neon 배포) 실행 결과 재검증
- **배포 URL**
  - 프론트엔드(Vercel): https://second-llbky-front.vercel.app
  - 백엔드(Render): https://second-llbky-back.onrender.com
  - DB(Neon): 별도 URL/커넥션 문자열은 민감정보라 생략

## 검증 결과 요약 (O/X)

| 구분 | 항목 | 결과 |
|---|---|---|
| 배포 상태 | 백엔드(Render) 배포 성공 | O |
| 배포 상태 | 프론트엔드(Vercel) 배포 성공 | O |
| 배포 상태 | 백엔드 URL 정상 응답 | O |
| 배포 상태 | 프론트엔드 URL 정상 응답 (SPA rewrite 포함) | O |
| 콜드 스타트 | 슬립 후 첫 요청 응답 | O (약 105초 소요, 아래 2번 참고) |
| E2E | 프론트 → 백엔드 요청 도달 | O |
| E2E | CORS 통과 | O |
| E2E | 백엔드 → DB(Neon) 쿼리 실행 | O |
| 보안/설정 | CORS를 Vercel 도메인으로 제한 | O |
| 보안/설정 | SSLHelper 제거 여부 | X (의도적으로 유지) |
| 보안/설정 | 시크릿 커밋 여부 | X (커밋 이력에 없음 — 정상) |
| 과금 | Render/Vercel/Neon 무료 한도 초과 여부 | X (한도 내, 과금 없음) |
| 외부 API | OpenAI/Naver 사용량 확인 | 미확인 (사용자가 이번 회차에는 생략 선택) |

## 1. 배포 상태

이번 검증에서는 Render/Vercel 콘솔에 직접 로그인해서 대시보드 로그를 열람할 수는 없었고(계정 접근 권한 없음), 대신 **실제 배포된 URL을 직접 호출**해서 배포 성공 여부를 확인했습니다.

| 확인 | 결과 |
|---|---|
| `GET https://second-llbky-back.onrender.com/` | 404 (Spring Boot 기본 JSON 에러) → DispatcherServlet까지 정상 기동한 상태에서 나오는 정상적인 404 |
| `GET https://second-llbky-back.onrender.com/health` | 200, `{"status":"UP"}` |
| `GET https://second-llbky-front.vercel.app/` | 200 |
| `GET https://second-llbky-front.vercel.app/login` | 200 |
| `GET https://second-llbky-front.vercel.app/some/deep/route` (임의 딥링크) | 200 — SPA rewrite 정상 동작 |

빌드/배포 자체는 실패 없이 완료된 상태로 확인됩니다.

## 2. 콜드 스타트 실측

이번 검증에서는 실제로 **16분간 백엔드에 아무 요청도 보내지 않고 대기**시킨 뒤 첫 요청 응답 시간을 측정했습니다.

- **실측치: 약 104.8초 (1분 45초)**
- `db-deploy-setup-prompt.md` 3-3에 적힌 "수십 초" 가정보다 실제로는 더 오래 걸렸습니다. Render 공식 문서상의 "약 1분" 안내보다도 길게 나온 편입니다(정확한 사유는 알 수 없으나, Docker 이미지 크기나 Spring Boot 컨텍스트 초기화 시간이 영향을 줬을 가능성이 있습니다).
- **문서 갱신 권장**: `db-deploy-setup-prompt.md` 3-3의 "콜드 스타트" 행을 "수십 초"에서 "1~2분 정도(실측 약 105초)"로 갱신하는 걸 권장합니다. (이번 보고서에서는 실측만 기록하고, 원본 프롬프트 문서 수정은 별도 승인 후 진행하겠습니다.)
- **대응 방법**: UptimeRobot 등 상시 핑은 적용하지 않기로 결정했습니다 — 무료 750 instance hours를 상시 핑으로 소모하는 트레이드오프보다, 포트폴리오 링크에 "첫 로딩 최대 2분 정도 걸릴 수 있습니다" 안내 문구를 남기는 방향으로 진행 권장.

## 3. 엔드투엔드 검증

**검증한 흐름**: Vercel 프론트(로그인 페이지, 실제 브라우저로 접속) → 브라우저에서 Render 백엔드로 `POST /member/login` 요청 → Render가 Neon DB에서 `findByLoginId` 쿼리 실행 → 응답 반환

| 항목 | 결과 |
|---|---|
| 프론트 → 백엔드 요청 도달 | O |
| CORS 통과 (Vercel 도메인 기준) | O — `Access-Control-Allow-Origin: https://second-llbky-front.vercel.app` 반환 확인 |
| CORS 차단 (임의 도메인 기준) | O — `evil-example.com`을 Origin으로 보내면 403 Forbidden |
| 백엔드 → DB 쿼리 실행 | O — `GET /trend/news/today?memberId=1` 호출 시 "회원 정보를 찾을 수 없습니다"라는 **비즈니스 로직 에러**가 반환됨(DB 연결 실패가 아니라 정상 조회 후 데이터 없음을 의미) |
| 전체 흐름 | O — 존재하지 않는 테스트 계정으로 로그인 시도 시 500 에러가 났지만, 이는 계정 미존재로 인한 정상적인 실패이며 프론트-백엔드-DB 연결 경로 자체는 끊김 없이 동작 |

**끊긴 구간 없음.** 실패한 부분(로그인 500)은 인프라/배포 문제가 아니라 애플리케이션 레벨의 기존 이슈입니다(아래 "발견된 이슈" 참고).

## 4. CORS 최종 처리

- **결과**: `allowedOrigins("*")` (전체 허용) → `"https://second-llbky-front.vercel.app"`, `"http://localhost"`로 제한 완료 ([CorsConfig.java](../../src/main/java/com/example/demo/config/CorsConfig.java))
- **이유**: Vercel 배포 도메인이 확정된 이후에는 전체 허용을 유지할 이유가 없어서, 실제 프론트 도메인 + 로컬 개발 환경만 허용하도록 좁혔습니다.
- **검증 방법**: preflight 요청에 `Origin` 헤더를 바꿔가며 실제 응답을 비교 — Vercel 도메인은 허용, 임의 도메인은 403.

## 5. SSLHelper 최종 처리

- **결과**: **유지**하기로 결정했고, 이번 재검증 시점에도 코드가 그대로 남아있는 걸 확인했습니다([SSLHelper.java](../../src/main/java/com/example/demo/config/SSLHelper.java)).
- **유지 이유**: 호출부가 [NewsAIService.java:119](../../src/main/java/com/example/demo/newstrend/service/NewsAIService.java:119) 뉴스 URL Jsoup 스크래핑 직전 한 곳뿐이고, 지금 당장 문제를 일으키고 있지 않아 배포 작업 범위에서 무리하게 건드리지 않기로 판단했습니다.
- **인지된 위험**: JVM 전역 `HttpsURLConnection`의 인증서/호스트네임 검증을 비활성화하므로, 이론적으로는 같은 프로세스의 다른 HTTPS 통신에도 영향을 줄 수 있습니다(다만 OpenAI/Naver 호출은 별도 HTTP 클라이언트를 쓰는 것으로 보여 실질적 영향은 제한적일 가능성이 높음). 제거 여부는 추후 별도로 재검토가 필요합니다.

## 6. 보안/설정 점검

- 시크릿 커밋 여부: `git log --all -- .env .env.local`로 백엔드/프론트엔드 저장소 모두 확인한 결과 **커밋 이력 없음**.
- `.gitignore`: 백엔드는 `.env`, `.env.*`(단 `.env.example`은 예외)를 정상적으로 제외 중.
- 환경변수 등록 상태(Render):

| 변수 | 등록 상태 | 비고 |
|---|---|---|
| `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` | O | Neon 연결 확인됨 |
| `OPENAI_API_KEY` | O | |
| `NAVER_CLIENT_ID`/`NAVER_CLIENT_SECRET` | O | 팀원 키 → 본인 키 교체가 네이버 개발자센터 UI 이슈로 보류 중 |
| `NEWS_API_KEY` | 미등록 | 코드에서 사용되지 않는 죽은 설정으로 확인됨 — 등록 불필요 |
| `GOOGLE_SEARCH_API_KEY`/`GOOGLE_SEARCH_ENGINE_ID` | 미등록 | 위와 동일하게 죽은 설정 — 등록 불필요 |
| `VUE_APP_API_BASE_URL`(Vercel) | O | `https://second-llbky-back.onrender.com` |

## 7. 과금 여부 (사용자 제공 스크린샷 기준 확인 완료)

사용자가 Render/Vercel/Neon 각 대시보드의 Usage/Billing 화면을 직접 캡처해서 제공했습니다.

| 플랫폼 | 확인 내용 |
|---|---|
| **Render** | Free Instance Hours **0.92 / 750시간**, Bandwidth **0MB / 5GB**, Services **1 / 25**, Pipeline Minutes **4분 / 500분** — 무료 한도 대비 매우 낮은 사용량. 카드 등록 여부는 화면상 별도 확인 안 됨 |
| **Vercel** | Edge Requests **42 / 1M**, Fast Data Transfer **1.41MB / 100GB** 등 전 항목 한도 대비 미미한 수준 |
| **Neon** | **Free Plan, $0/month**로 명시됨 (0.5GB storage, 100 compute hours, 10 branches 포함) — 현재 플랜이 유료로 전환되지 않았음을 직접 확인 |

**결론**: 세 플랫폼 모두 무료 티어 한도 내에서 운영 중이며, 실제 청구는 발생하지 않았고 발생할 조건에도 들어서지 않았습니다.

## 8. 외부 API 사용량

이번 회차에서는 사용자 요청에 따라 OpenAI/Naver 등 외부 API 사용량 확인을 생략했습니다. `NEWS_API_KEY`/`GOOGLE_SEARCH_API_KEY`/`GOOGLE_SEARCH_ENGINE_ID`는 코드에서 실제로 사용되지 않는 죽은 설정이라 애초에 쿼터 소모가 없습니다. OpenAI/Naver 사용량이 궁금해지면 각 콘솔(OpenAI Usage 페이지, 네이버 개발자센터 통계)에서 확인 가능합니다.

## 발견된 이슈 정리 (이전 회차 포함 누적)

1. 프론트엔드 저장소에 `package.json`/`package-lock.json`이 `.gitignore`로 인해 커밋된 적이 없어 Vercel 빌드가 원천적으로 불가능했던 문제 — 해결됨(파일 복원 + gitignore 수정 + 커밋)
2. `vue.config.js`의 `transpileDependencies: true`가 `pdfjs-dist`의 최신 JS 문법을 처리 못 해 빌드가 깨졌던 문제 — 해결됨(ffmpeg 전용 배열로 좁힘)
3. `axiosConfig.js`의 API base URL 하드코딩(`localhost:8080`) — 해결됨(`VUE_APP_API_BASE_URL` 환경변수화)
4. `MemberService.login()`이 로그인 실패 시 항상 500을 반환하는 기존 버그 — **미해결, 배포 범위 밖**. 별도 세션에서 예외 처리 개선 권장
5. `NEWS_API_KEY`/`GOOGLE_SEARCH_API_KEY`/`GOOGLE_SEARCH_ENGINE_ID`가 코드에서 전혀 사용되지 않는 죽은 설정 — 배포 시 값 없이 진행해도 무방함을 확인
6. 네이버 개발자센터 애플리케이션 등록 화면의 "사용 API" 드롭다운에 "검색"/"데이터랩" 옵션이 안 보이는 현상 — 원격 확인 불가로 **미해결, 사용자 재확인 필요**
7. (신규) 콜드 스타트 실측치(약 105초)가 문서상 가정("수십 초")보다 길어서 문서 갱신 필요

## 9. 재검증 (2026-09-07)

README 정리 작업 중 배포 URL이 여전히 유효한지 브라우저로 직접 재확인했다.

| 확인 | 결과 |
|---|---|
| `GET https://second-llbky-back.onrender.com/` | 404 (문서 §1과 동일 — DispatcherServlet 정상 기동 상태의 정상적인 404) |
| `GET https://second-llbky-back.onrender.com/health` | 200, `{"status":"UP"}` |
| `GET https://second-llbky-front.vercel.app/login` | 200, 정적 리소스(css/js) 전부 정상 로드 |
| 존재하지 않는 계정으로 로그인 시도 (프론트 `/login` 폼) | 콘솔에 404 + **500** 에러 발생, alert 팝업 표시 — §"발견된 이슈" #4(`MemberService.login()`이 로그인 실패 시 500 반환)가 **그대로 재현됨, 여전히 미해결** |

CORS 관련 에러 메시지 없이 요청이 백엔드까지 도달해 500 응답을 받았으므로, §4에서 확인한 CORS 허용 설정(Vercel 도메인)은 계속 정상 동작 중인 것으로 판단된다. 콜드 스타트 재실측, Render/Vercel/Neon 과금 대시보드 재확인은 이번 회차에서는 생략했다(변경 사유 없음).

**결론**: 2026-09-04 검증 시점과 비교해 인프라 상태에 달라진 점 없음. 유일한 미해결 이슈(로그인 500)도 동일하게 남아있다.

## 남은 위험/할 일

- 네이버 API 키 팀원 → 본인 교체 마무리 (등록 화면 이슈 해결 필요)
- `MemberService.login()` 예외 처리 개선 (500 → 401/404)
- `db-deploy-setup-prompt.md` 3-3의 콜드 스타트 가정치를 실측값으로 갱신할지 결정
- Neon 무료 스토리지(0.5GB) — BYTEA 저장 구조라 데모 데이터 용량 관리 필요
- OpenAI/Naver 실제 사용량은 여전히 미확인 상태 (필요 시 재확인)

## 포트폴리오 소개 초안

> **AI 커리어 코칭 플랫폼 (Career Coach)**
> 이력서·자소서·면접·학습·트렌드 분석을 AI로 지원하는 취업 준비 플랫폼입니다. 프론트엔드(Vue.js)는 Vercel, 백엔드(Spring Boot, Docker)는 Render, 데이터베이스(PostgreSQL + pgvector)는 Neon에 각각 배포해, 무료 티어만으로 프론트·백엔드·DB가 실제로 통신하는 3-tier 아키텍처를 완성했습니다. Render 무료 인스턴스의 콜드 스타트(15분 미사용 시 슬립, 재시작에 약 1~2분)와 같은 인프라 제약을 파악하고, 상시 헬스체크 핑 대신 사용자 안내 문구로 대응하는 등 무료 자원 안에서 실서비스에 가까운 배포 경험을 설계했습니다.
>
> 배포 과정에서 프론트엔드 저장소의 `.gitignore` 설정 오류로 `package.json`이 누락되어 빌드 자체가 불가능했던 문제, `transpileDependencies` 설정이 의존 라이브러리의 최신 문법과 충돌해 빌드가 실패하던 문제 등을 직접 진단하고 해결했으며, CORS를 배포된 프론트엔드 도메인으로 제한하는 등 보안 설정도 함께 정리했습니다.

---

## 다음 단계

배포와 E2E 검증이 모두 정상 확인된 상태입니다. 선택적으로 진행할 수 있는 항목:
- 네이버 API 키 교체 마무리
- `MemberService.login()` 등 기존 버그 수정
- `db-deploy-setup-prompt.md`의 콜드 스타트 가정치 갱신
- README에 배포 URL/아키텍처 다이어그램 추가 등 포트폴리오 보완 작업
