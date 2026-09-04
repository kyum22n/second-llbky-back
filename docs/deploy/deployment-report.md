# 배포 결과 검증 보고서

- **작성일**: 2026-09-04
- **대상**: `docs/prompts/db-deploy-setup-prompt.md` 2번 프롬프트(Vercel + Render + Neon 배포) 실행 결과 검증

## 배포 구성 요약

| 구성 요소 | 플랫폼 | URL |
|---|---|---|
| 프론트엔드 | Vercel | https://second-llbky-front.vercel.app |
| 백엔드 | Render Free Web Service (Docker) | https://second-llbky-back.onrender.com |
| DB | Neon (PostgreSQL + pgvector) | (URL 등 민감정보 제외) |

계획대로 프론트/백엔드/DB를 역할별로 분리 배포하는 구성이 실제로 그대로 적용됐습니다.

## 1. 배포 상태

| 항목 | 결과 |
|---|---|
| 백엔드 빌드/배포 성공 | O |
| 백엔드 URL 응답 | O — `GET /` 404(JSON, 정상 라우팅 확인), `GET /health` → `{"status":"UP"}` 200 |
| 프론트엔드 빌드/배포 성공 | O (배포 전 로컬 빌드에서 실패 2건 발견 → 수정 후 성공, 아래 "발견된 신규 이슈" 참고) |
| 프론트엔드 URL 응답 | O — 루트/딥링크 모두 200 (SPA rewrite 정상) |

## 2. 콜드 스타트

검증 세션 동안 서비스가 계속 활성 상태였기 때문에 15분 슬립 이후의 실제 콜드 스타트 시간을 별도로 측정하지는 못했습니다(첫 확인 시점에 이미 0.3초 응답 — 직전 작업으로 깨어있는 상태였음). `db-deploy-setup-prompt.md` 3-3의 "수십 초~1분" 가정은 Render 공식 문서(15분 미사용 시 슬립, 재시작에 약 1분) 기준으로는 유효합니다. 콜드 스타트 대응(UptimeRobot 등 상시 핑)은 이번에 적용하지 않기로 결정했습니다 — 무료 750시간 한도를 상시 핑으로 소모하는 트레이드오프보다, 포트폴리오 링크에 "첫 로딩 최대 1분" 안내 문구를 남기는 방향으로 진행 권장.

## 3. 엔드투엔드 검증

**흐름**: Vercel(로그인 페이지) → 사용자가 폼 제출 → axios가 Render 백엔드로 `POST /member/login` → Render가 Neon DB에서 `findByLoginId` 쿼리 실행 → 응답 반환 → 프론트 알림 표시

| 항목 | 결과 |
|---|---|
| 프론트 → 백엔드 요청 도달 | O |
| CORS 통과 | O |
| 백엔드 → DB 쿼리 실행 | O (`GET /trend/news/today?memberId=1` 호출 시 "회원 정보를 찾을 수 없습니다" 비즈니스 로직 에러 반환 — DB 연결 실패가 아닌 정상 조회 후 데이터 없음을 확인) |
| 전체 흐름 성공 | O — 테스트 계정이 실제 DB에 없어 최종 로그인 자체는 실패(500)했지만, 이는 계정 미존재에 따른 정상적 실패이며 프론트-백엔드-DB 연결 자체는 끊김 없이 동작 |

## 4. CORS 최종 처리

- 기존 `allowedOrigins("*")` → `"https://second-llbky-front.vercel.app"`, `"http://localhost"`로 제한 완료 ([CorsConfig.java](../../src/main/java/com/example/demo/config/CorsConfig.java))
- 재배포 후 실제 preflight로 검증: Vercel 도메인은 `Access-Control-Allow-Origin` 헤더 정상 반환, 임의 도메인(`evil-example.com`)은 헤더 없음(차단) 확인

## 5. SSLHelper 처리

- **유지하기로 결정.** 호출부는 `NewsAIService.java:119`(뉴스 URL Jsoup 스크래핑 직전) 한 곳뿐이며, 이번 배포 범위에서는 제거하지 않음.
- 위험성은 인지된 상태: JVM 전역 `HttpsURLConnection` 인증서/호스트네임 검증을 비활성화하므로 이론상 다른 HTTPS 통신에도 영향 줄 수 있음(다만 OpenAI/Naver 호출은 별도 클라이언트를 쓰는 것으로 보여 실제 영향은 제한적일 가능성). 추후 별도로 재검토 필요.

## 6. 환경변수 등록 상태

| 변수 | Render 등록 | 비고 |
|---|---|---|
| `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` | O | Neon 연결 확인됨 |
| `OPENAI_API_KEY` | O | |
| `NAVER_CLIENT_ID`/`NAVER_CLIENT_SECRET` | O | 팀원 키 → 본인 키 교체 진행 중(네이버 개발자센터 UI 변경으로 정확한 등록 경로는 사용자가 직접 재확인 필요) |
| `NEWS_API_KEY` | 미등록 | 코드에서 실제로 사용되는 곳이 없는 죽은 설정으로 확인됨 — 등록 불필요 |
| `GOOGLE_SEARCH_API_KEY`/`GOOGLE_SEARCH_ENGINE_ID` | 미등록 | 위와 동일하게 죽은 설정 — 등록 불필요 |
| `VUE_APP_API_BASE_URL`(Vercel) | O | `https://second-llbky-back.onrender.com` |

시크릿은 각 플랫폼 환경변수에만 등록했고 커밋 이력에 포함되지 않았습니다(코드는 `${VAR}` 플레이스홀더만 참조).

## 7. 과금 여부

Render 공식 문서 기준으로 무료 web service는 결제 수단 등록이 필수가 아니며, 등록해도 무료 한도(월 750 instance hours) 내에서는 과금되지 않고 한도 초과 시 서비스가 일시 정지되는 구조입니다. 실제 이번 배포 과정에서 결제 정보 입력 여부/청구 화면은 사용자가 직접 콘솔에서 확인하지 않아 별도로 캡처받지 못했습니다 — **필요 시 Render/Vercel/Neon 각 대시보드의 Billing/Usage 페이지를 확인해서 알려주시면 이 항목을 갱신하겠습니다.**

## 8. 외부 API 사용량

OpenAI/Naver/News API/Google Custom Search의 실시간 쿼터 사용량은 각 서비스 콘솔에서만 확인 가능해 이번 검증 범위에서는 조회하지 못했습니다. 특히 News API/Google Custom Search는 코드에서 사용되지 않는 죽은 설정이라 쿼터 소모 자체가 발생하지 않습니다.

## 발견된 신규 이슈 (db-deploy-setup-prompt.md에 없던 것)

1. **프론트엔드 저장소에 `package.json`/`package-lock.json`이 아예 없었음** — `.gitignore`에 두 파일이 잘못 등록돼 있어 커밋된 적이 없었고, 이 상태로는 Vercel이 저장소를 클론해도 `npm install` 자체가 불가능해 배포가 원천적으로 막혀있었습니다. 사용자가 파일을 복원 → `.gitignore`에서 제외 항목 삭제 → 커밋으로 해결.
2. **`vue.config.js`의 `transpileDependencies: true`가 빌드를 깨뜨림** — 전체 `node_modules`를 Babel 변환 대상으로 삼아서 `pdfjs-dist`의 private class 문법을 처리하지 못해 프로덕션 빌드가 실패했습니다. 원래 `@ffmpeg/ffmpeg` 경고 제거용이었던 설정을 해당 패키지들만 포함하는 배열로 좁혀서 해결.
3. **`axiosConfig.js`의 API base URL이 하드코딩(`localhost:8080`)돼 있었음** — 배포 환경에서 백엔드를 못 찾는 문제였는데, `VUE_APP_API_BASE_URL` 환경변수로 전환(없으면 기존처럼 localhost 폴백)해서 해결.
4. **로그인 실패 시 항상 500 반환** — `MemberService.login()`이 `RuntimeException`을 던지는데 이를 401/404로 변환하는 예외 핸들러가 없음([MemberService.java:60-66](../../src/main/java/com/example/demo/member/service/MemberService.java:60)). 배포와 무관한 기존 버그이며 이번에는 수정하지 않음.
5. **`NEWS_API_KEY`/`GOOGLE_SEARCH_API_KEY`/`GOOGLE_SEARCH_ENGINE_ID`가 코드에서 전혀 사용되지 않는 죽은 설정**이라는 걸 확인 — `application.properties`에만 정의돼 있고 어떤 `@Value`도 이를 주입받지 않음. 배포 시 이 값들을 비워도 무방.
6. **네이버 개발자센터의 애플리케이션 등록 화면 UI가 기존 가이드 문서와 달라짐** — "사용 API" 드롭다운에 "검색"/"데이터랩" 옵션이 보이지 않는 상태를 확인했으나, 원격으로 콘솔에 접근할 수 없어 정확한 원인(검색형 콤보박스의 필터링 문제인지, 등록 플로우 자체가 변경된 것인지)은 사용자가 직접 재확인 필요.

## 남은 위험/할 일

- 네이버 API 키를 팀원 것에서 본인 것으로 교체하는 작업이 아직 진행 중 (등록 화면 UI 이슈로 보류)
- Render/Vercel/Neon의 실제 과금 상태(카드 등록 여부, 청구 발생 여부)를 사용자가 직접 대시보드에서 확인 필요
- `MemberService.login()` 등 예외 처리 미비로 인한 500 응답 — 별도 세션에서 수정 검토 권장
- Neon 무료 티어 스토리지 사용량(약 0.5GB 한도) — BYTEA로 파일을 저장하는 구조라 데모 데이터를 작게 유지할 것

## 다음 단계

DB 연결과 배포가 모두 정상 확인됐으므로, 추가 작업은 선택 사항입니다:
- 네이버 키 교체 마무리
- `db-deploy-setup-prompt.md` 4번 섹션의 포트폴리오 보완 작업(README 배포 URL/아키텍처 다이어그램 추가, 기존 mapper XML 버그 수정 등) 중 원하는 항목 진행
