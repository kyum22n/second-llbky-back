# 프롬프트 템플릿: DB 연결 복구 + 배포

이 프로젝트(second-llbky-back)를 위해 만든 재사용 가능한 프롬프트 템플릿입니다.
`[ ]`로 표시된 부분만 채워서 새 Claude Code 세션(또는 다른 AI 어시스턴트)에 붙여넣어 사용하세요.
같은 세션 안이라면 그대로 이어서 요청해도 됩니다.

---

## 0. 배포 플랫폼 결정 (확정 · 무료 · 프론트+백엔드+DB 모두 배포 기준)

취업 포트폴리오용으로 **무료 배포**, **프론트엔드(Vue.js)/백엔드/DB 전부 배포**한다는 조건에서 아래 조합으로 **확정**합니다.
비교 근거는 6번 섹션 참고.

| 구성 요소 | 확정 플랫폼 | 이유 요약 |
|---|---|---|
| 프론트엔드 (Vue.js) | **Vercel** | Vue(Vite/Vue CLI 모두) 공식 프레임워크 프리셋 지원, 카드 등록 없이 무료(대역폭 100GB/월, 커스텀 도메인·HTTPS 포함), 만료·자동정지 없음 |
| 백엔드 (Spring Boot) | **Render** Free Web Service (Docker) | Java 컨테이너를 무료로 상시 실행 가능한 몇 안 되는 옵션. Railway는 더 이상 상시 무료 티어가 없어서 제외 |
| DB (PostgreSQL + pgvector) | **Neon** Free Tier | pgvector 지원, 무료 DB가 90일 뒤 삭제되는 Render Postgres와 달리 만료가 없고, 비활성 시에도 접속하면 자동으로 깨어나서 수동 재개(resume) 조작이 필요 없음 |

**Vue.js + Vercel 조합 검증**: Vercel은 Vue를 React/Next.js와 동급으로 취급하는 공식 프레임워크 프리셋을 제공해서 빌드 커맨드(`vite build` 또는 `vue-cli-service build`)와 출력 디렉토리(`dist`)를 자동 인식합니다. 다만 `vue-router`를 history 모드로 쓰면 새로고침 시 404가 날 수 있어 `vercel.json`에 SPA rewrite 규칙(`"rewrites": [{ "source": "/(.*)", "destination": "/index.html" }]`)을 추가해야 합니다 — 이건 프론트엔드 저장소에 새 설정 파일을 추가하는 것뿐이라 "기존 소스 코드 침범 금지" 원칙과 충돌하지 않습니다. 이 설정 외에는 무료 티어 사용에 걸림돌이 없다고 판단해서 Vercel로 확정했습니다.

**세 플랫폼을 하나로 통일하지 않고 역할별로 분리한 이유**: Java 백엔드를 상시 무료로 돌릴 수 있는 곳(Render), SPA를 CDN으로 빠르게 서빙하는 곳(Vercel), pgvector를 지원하면서 무료 DB가 사라지지 않는 곳(Neon)이 서로 다르기 때문입니다. 자세한 비교는 6번 섹션 참고.

아래 항목은 프롬프트를 채우기 전에 직접 정해야 하는 값입니다.

- **프론트엔드 프로젝트 위치**: [별도 저장소 경로/URL 입력, 예: C:\kyum\project\second-llbky-front 또는 GitHub URL]
- **프론트엔드 빌드 도구**: [Vite / Vue CLI — Vercel 빌드 설정에 영향]
- **도메인 유무**: 없음(플랫폼 기본 서브도메인) / 보유 도메인 연결
- **GitHub 저장소**: 이 백엔드 저장소와 프론트엔드 저장소가 이미 GitHub에 push되어 있는지 여부 (Render/Vercel 모두 GitHub 연동 배포가 기본 흐름)

---

## 1. 프롬프트 템플릿 — DB 연결 (Neon)

```
나는 second-llbky-back(Spring Boot 3.4 / Java 21 / Gradle) 프로젝트의 데이터베이스를 Neon(PostgreSQL, 무료 티어)에 새로 연결하려고 해.

배경:
- 이 프로젝트는 원래 팀 프로젝트에서 개인 저장소로 분리한 것이고, 팀 프로젝트 때 쓰던 DB 서버 연결이 끊긴 상태야.
- src/main/resources/application.properties 에 spring.datasource.url/username/password 가 환경변수(${DB_URL}, ${DB_USERNAME}, ${DB_PASSWORD})로 되어 있는데 현재 이 값들이 없어.
- MyBatis를 사용 중이고(mybatis-spring-boot-starter), 매퍼 XML은 src/main/resources/mapper/*.xml 에 있어. JPA/Hibernate가 아니라서 자동 스키마 생성이 안 돼.
- 스키마는 docs/db/schema.sql 에 이미 정리해뒀어(mapper XML → ERD → 원본 SQL 순으로 우선순위를 두고 교차 검증, pgvector 확장 포함). 파일 상단과 각 테이블 주석에 충돌 지점과 근거가 적혀 있으니 그것도 같이 확인해줘. 이 파일을 그대로 실행하면 돼. 추가로 조사/추론할 필요 없어.
- Spring AI pgvector 의존성(spring-ai-starter-vector-store-pgvector)이 있지만 코드에서 VectorStore를 실제로 쓰는 로직은 아직 없어. 그래도 CREATE EXTENSION vector; 는 미리 해둬.
- DB는 Neon(https://neon.tech) 무료 티어를 새로 만들어서 쓸 거야.

해줬으면 하는 것:
1. docs/db/schema.sql 을 검토하고(문법 오류 없는지, FK 순서가 맞는지) 그대로 실행할 수 있게 정리해줘.
2. 내가 Neon 콘솔에서 프로젝트를 만들고 나면(직접 할게), 발급받은 connection string을 spring.datasource.url/username/password 형식(JDBC URL)에 맞게 변환하는 방법을 알려줘. Neon은 기본적으로 sslmode=require가 필요하니 JDBC URL에 반영해줘.
3. 로컬 개발/테스트용으로는 Docker로 PostgreSQL(+ pgvector 이미지, 예: pgvector/pgvector:pg16)을 띄우는 docker-compose.yml도 하나 만들어줘. 로컬에서 먼저 스키마 적용과 연결을 검증하고, 그다음에 Neon으로 넘어갈 수 있게.
4. .env 또는 로컬 실행용 환경변수 설정 방법을 안내해줘. 이 프로젝트에 필요한 전체 환경변수 목록을 application.properties에서 다시 한번 확인해서 빠짐없이 정리해줘 (DB_URL, DB_USERNAME, DB_PASSWORD, OPENAI_API_KEY, NAVER_CLIENT_ID, NAVER_CLIENT_SECRET, NEWS_API_KEY, GOOGLE_SEARCH_API_KEY, GOOGLE_SEARCH_ENGINE_ID 등).
5. 애플리케이션을 실행해서 DB 연결과 MyBatis 매퍼가 정상 동작하는지 확인해줘 (간단한 조회 API 하나로 스모크 테스트).
6. .gitignore에 .env, 로컬 DB 데이터 디렉토리 등이 이미 잘 빠져 있는지 확인해줘.
7. interview_answer(오디오/비디오 BYTEA), interview_session/portfolio(PDF BYTEA) 같은 테이블에 실제 파일을 많이 저장하면 Neon 무료 티어 저장 용량(약 0.5GB)을 빨리 채울 수 있다는 점을 알려주고, 데모/포트폴리오용으로 용량을 아끼는 팁(테스트 파일 크기 제한 등)이 있으면 알려줘.

주의:
- application.properties, mapper XML 등 기존 소스/설정은 DB 접속 정보 관련(application.properties의 datasource 부분) 외에는 바꾸지 마.
- 실제 DB 비밀번호는 절대 코드나 커밋에 하드코딩하지 마.(필요한 경우 나에게 직접 입력을 요청할 것)
- Neon 프로젝트 생성, 커넥션 스트링 발급 등 콘솔 조작은 내가 직접 할 테니, 어떤 화면에서 뭘 눌러야 하는지만 순서대로 안내해줘.
```

---

## 2. 프롬프트 템플릿 — 배포 (Vercel + Render + Neon)

```
DB 연결이 끝난 second-llbky-back(Spring Boot 3.4 / Java 21 / Gradle) 프로젝트와, 별도 저장소에 있는 프론트엔드 프로젝트를 무료로 배포하려고 해. 취업 포트폴리오용이라 프론트엔드/백엔드/DB를 모두 실제로 접속 가능한 상태로 올리는 게 목표야.

배포 조합:
- 프론트엔드: Vercel
- 백엔드: Render Free Web Service (Docker)
- DB: Neon (이미 연결 완료)

배경:
- 백엔드는 Spring Boot, 빌드는 Gradle(gradlew), 정적 뷰 엔진으로 Thymeleaf를 쓰지만 사실상 API 서버로 쓰고 있어.
- WebFlux, MyBatis, Spring AI(OpenAI, pgvector), PDFBox/iText(PDF 생성), Jsoup을 사용해.
- 파일 업로드 최대 용량이 50MB로 설정돼 있어 (spring.servlet.multipart.max-file-size). Render 무료 web service의 요청/응답 제한과 충돌하지 않는지 확인해줘.
- CORS가 현재 모든 origin(*)에 열려 있는 상태야 (src/main/java/com/example/demo/config/CorsConfig.java). Vercel에 배포된 프론트엔드 도메인만 허용하도록 바꿀지 판단해줘.
- src/main/java/com/example/demo/config/SSLHelper.java 가 SSL 인증서 검증을 완전히 우회하는 코드인데, 실제로 어디서 호출되는지 찾아서 운영 배포 시에도 필요한지, 보안 위험은 없는지 검토해줘.
- Render 무료 web service는 15분 정도 요청이 없으면 슬립 상태가 되고, 다음 요청 때 콜드 스타트로 수십 초가 걸릴 수 있어. 포트폴리오 데모 시 첫 요청이 느릴 수 있다는 점을 감안해줘 (필요하면 무료 범위 내 대응 방법도 제안해줘, 유료 전환은 하지 말고).
- 프론트엔드는 Vue.js(빌드 도구: [Vite / Vue CLI])이고 Vercel에 배포할 거야. vue-router를 history 모드로 쓰고 있다면 새로고침 404 방지를 위한 vercel.json rewrite 설정도 같이 확인해줘.
- 프론트엔드 프로젝트 경로/저장소: [경로 또는 URL]
- 도메인: [없음, 플랫폼 기본 서브도메인 사용 / 보유 도메인: ___]
- 이 백엔드 저장소와 프론트엔드 저장소는 GitHub에 [push되어 있음 (URL: ___) / 아직 안 되어 있음]

해줬으면 하는 것:
1. 백엔드를 Render에 배포하기 위한 Dockerfile을 작성해줘 (Java 21 기반, gradle 빌드 → jar 실행하는 멀티스테이지 빌드로). 기존 소스 코드 자체는 건드리지 말고 Dockerfile/설정 파일만 추가해줘.
2. Render Web Service 생성 절차(GitHub 연동, 빌드 커맨드, 환경변수 등록 화면, 카드 등록이 요구되는지 여부)를 순서대로 안내해줘. 계정 생성이나 리포지토리 연결처럼 내가 직접 눌러야 하는 단계는 그렇게 표시해줘. 만약 무료 티어인데도 결제 수단 등록을 요구하는 화면이 나오면 그 사실을 먼저 알려주고, 실제로 과금되는 조건인지(카드 등록만 요구/실사용 시 과금)까지 확인해서 알려줘.
3. 운영 환경에서 필요한 환경변수 전체 목록(DB_URL, DB_USERNAME, DB_PASSWORD, OPENAI_API_KEY, NAVER_CLIENT_ID, NAVER_CLIENT_SECRET, NEWS_API_KEY, GOOGLE_SEARCH_API_KEY, GOOGLE_SEARCH_ENGINE_ID, PORT 등)을 정리해주고, Render 환경변수 설정 화면에 어떻게 등록하는지 안내해줘.
4. 프론트엔드(Vue.js)를 Vercel에 배포하는 절차를 안내해줘. 빌드 커맨드/출력 디렉토리가 자동 인식되는지 확인하고, 필요하면 vercel.json(SPA rewrite)만 새로 추가해줘. 백엔드 API 주소(Render URL)를 프론트엔드 환경변수로 어떻게 넣어야 하는지도 포함해줘.
5. 헬스체크: 먼저 이미 존재하는 GET 엔드포인트 중 인증 없이 호출 가능한 게 있는지 찾아서 그걸 Render 헬스체크 경로로 쓸 수 있는지 확인해줘. 적당한 게 없을 때만 새 헬스체크 엔드포인트 추가를 제안하고, 실제로 추가하기 전에 나한테 먼저 물어봐줘 (DB 설정 외의 소스 변경이라서).
6. CORS 설정을 Vercel 배포 도메인 기준으로 조정할지 판단해줘. 이것도 기존 소스(CorsConfig.java) 수정이니 실제로 바꾸기 전에 나와 상의해줘. 배포 도메인이 확정되기 전까지는 어떻게 임시로 운영할지도 제안해줘.
7. SSLHelper의 SSL 검증 우회 코드가 운영 환경에서도 필요한지 확인하고, 불필요하면 위험성을 알려주고 제거 여부를 나와 상의해줘. 코드를 임의로 바로 지우지는 마.
8. Render/Vercel/Neon 각각에서 로그와 배포 상태를 어떻게 확인하는지 안내해줘.
9. Render 무료 web service의 슬립/콜드 스타트에 대응해서, 과금 없이 쓸 수 있는 방법(예: UptimeRobot 같은 무료 모니터링 서비스로 주기적 핑)을 제안해줘. 다만 이런 핑이 Render 무료 정책(월 사용 시간 한도 등)에 위배되지 않는지도 같이 확인해줘.
10. 배포가 끝나면 프론트엔드 → 백엔드 → DB로 이어지는 실제 API 호출(예: 로그인 또는 조회 API)을 같이 검증해줘.

주의:
- 기존 소스 코드/설정은 Dockerfile, docker-compose.yml, vercel.json, 배포 설정 파일 등 새로 추가하는 파일 외에는 바꾸지 마. (CORS, SSLHelper, 헬스체크 엔드포인트 추가처럼 검토가 필요한 부분은 결과만 보고하고 실제 수정은 나와 상의 후에 해줘.)
- 실제 배포 실행(플랫폼 가입, GitHub 연동 승인, 결제 정보 입력 등)은 반드시 나한테 확인받고, 콘솔 조작이 필요한 단계는 내가 직접 하도록 안내만 해줘.
- 유료 플랜/애드온으로 전환되는 옵션은 제안하지 말고, 무료 티어 범위 안에서만 방법을 찾아줘. 애매하면 과금 가능성을 먼저 알려주고 나한테 판단을 맡겨줘.
- 시크릿 값은 절대 커밋하지 말고, 각 플랫폼의 환경변수/시크릿 매니저에만 등록해.
```

---

## 3. 위험 관리 — 보완 작업 후 실행 시 주의할 점

DB 연결/배포 작업을 마친 뒤에도 아래 사항들 때문에 애플리케이션이 실행되지 않거나, 실행은 되지만 특정 기능에서 오류가 날 수 있습니다. 새 세션에 작업을 맡길 때 이 섹션을 통째로 프롬프트에 붙여서 "이 내용도 감안해서 진행해줘"라고 하면 됩니다.

### 3-1. 스키마 재구성 과정에서 생긴 잠재 위험 (docs/db/schema.sql 관련)

`docs/db/schema.sql`은 **1) mapper XML(+entity 클래스) → 2) ERD → 3) 원본 SQL** 순서로 우선순위를 두고 세 자료를 교차 검증해서 만들었습니다. XML/entity가 컬럼 존재나 타입에 대한 명확한 근거를 줄 때는 그 값을, XML에 정보가 없을 때(주로 길이 제약처럼 mapper XML에는 안 나타나는 정보)는 ERD 값을 따랐습니다. 그 과정에서 아래와 같은 불일치가 확인됐습니다.

- **`resume.activities` vs ERD의 `activity` 네이밍 불일치 (XML 근거로 확정)**: ERD 다이어그램에는 컬럼명이 `activity`(단수)로 되어 있지만, 실제 `resume.xml` 매퍼는 insert/select/update 12곳 전부 `activities`(복수)를 사용합니다. 우선순위 1순위인 XML을 따라 `schema.sql`은 `activities`로 만들었습니다. 즉 **ERD 다이어그램 쪽이 최신 상태가 아닌 것**으로 보입니다. 나중에 ERD를 갱신할 기회가 있으면 `activity` → `activities`로 맞춰두는 게 좋고, 그 전까지 ERD만 보고 작업하는 사람이 있다면 컬럼명을 `activity`로 착각해서 `column "activity" does not exist` 오류를 만들 수 있으니 주의하세요.
- **`news_summary.published_at`을 다시 TIMESTAMP로 확정 (XML/entity 근거로 확정)**: ERD에는 DATE로 그려져 있지만, `newsSummary.xml`이 `DATE(published_at) = CURRENT_DATE`처럼 이 컬럼에 직접 `DATE()` 함수를 적용해서 날짜만 걸러내고 있고(이미 DATE 타입이면 불필요한 캐스팅), 대응하는 `NewsSummary.java` 엔티티의 `publishedAt` 필드도 `LocalDateTime`으로 선언되어 있습니다. 이건 시:분:초까지 저장/비교한다는 뜻이라 TIMESTAMP로 확정했습니다. 여기서도 **ERD가 오래된 것**으로 보이니, ERD를 다시 그릴 때 DATE → TIMESTAMP로 맞춰두세요. (참고: 이전 버전의 이 스키마에서 ERD를 따라 DATE로 만들었었는데, 그렇게 했다면 뉴스 API가 시간 정보를 포함해서 내려줄 때 `INSERT` 시 시간 부분이 그대로 버려지고 `ORDER BY published_at DESC` 같은 정렬이 기대와 다르게 동작했을 것입니다.)
- **`news_summary.summary_text`를 VARCHAR(1000) → VARCHAR(500)로 좁힘 (XML에 근거 없어 2순위 ERD 값 채택, 여전히 위험)**: mapper XML에는 이 컬럼의 길이를 판단할 단서가 없어서(단순 `ILIKE` 검색에만 쓰임) 2순위인 ERD 값(500자)을 그대로 채택했습니다. 다만 AI가 생성하는 "3줄 요약"이 실제로 500자를 넘는 경우 `INSERT`/`UPDATE` 시 `value too long for type character varying(500)` 오류로 뉴스 저장 기능 전체가 실패할 수 있습니다. 배포 전에 실제 요약 프롬프트 결과물 길이를 몇 건 확인해보고, 500자를 넘기는 경우가 있으면 스키마를 다시 TEXT나 더 큰 길이로 바꿀지 판단해야 합니다.
- **`learning`/`learning_week`/`learning_day`의 `title`/`status` 길이를 ERD 기준으로 확장(VARCHAR(20)→100 등)**: XML에는 길이 정보가 없어서 2순위인 ERD 값을 채택했습니다. 넓히는 방향이라 데이터가 잘릴 위험은 없습니다. 반대로 ERD에는 없고 XML에만 있는 `learning_summary`, `goal`, `learning_week_summary`, `study_time_min` 같은 컬럼은 1순위인 XML 근거로 실제 사용이 확인돼서 스키마에 그대로 남겨뒀습니다. 혹시 다른 자료(예: 원래 팀의 최신 ERD)에 이 컬럼들이 실제로 삭제된 것으로 되어 있다면 다시 확인이 필요합니다.
- **이 프로젝트는 Flyway/Liquibase 같은 마이그레이션 도구가 없습니다.** `schema.sql`을 한 번 수동으로 실행해서 스키마를 만드는 구조라, 로컬 Docker DB와 Neon DB에 각각 따로 적용해야 하고, 이후 스키마를 또 바꾸면 두 곳에 수동으로 반영하지 않는 한 스키마가 어긋날 수 있습니다.

### 3-2. mapper XML에서 발견된 기존 버그 (스키마 문제 아님, 코드 문제)

스키마를 검토하는 과정에서 DB 연결/배포와 무관하게 이미 존재하던 버그 2개를 발견했습니다. DB를 새로 연결하면 지금까지 숨겨져 있던(연결이 끊겨 있어서 아예 실행이 안 됐던) 이 버그들이 처음으로 드러날 수 있습니다.

- **[interviewQuestion.xml] `insertCustomQuestion` 컬럼/값 개수 불일치**: `INSERT INTO interview_question (session_id, question_text, created_at) VALUES (#{sessionId}, #{questionText})` — 컬럼은 3개인데 값은 2개만 있어서, "사용자가 직접 면접 질문을 추가하는" 기능을 호출하면 SQL 오류(500 에러)가 납니다.
- **[portfolioguide.xml] `deleteAllGuides`가 엉뚱한 테이블을 삭제함**: `delete from news_summary where member_id = #{memberId}` — 이름은 "가이드 전체 삭제"인데 실제로는 `portfolio_guide`가 아니라 `news_summary`(뉴스 요약) 테이블을 지웁니다. 이 API를 호출하면 포트폴리오 가이드는 그대로 남고, 대신 그 사용자의 뉴스 요약 데이터가 전부 삭제되는 **데이터 유실 버그**입니다.

이 두 가지는 DB 연결/배포 프롬프트의 범위를 벗어나는 코드 수정 사항이라 스키마에는 반영하지 않았습니다. 배포 전에 별도로 고칠지 결정하고, 고치기로 하면 별도 세션에서 "mapper XML 버그 수정"으로 요청하세요.

### 3-3. 배포/운영 단계에서 흔히 생기는 문제와 해결 방안

| 위험 | 증상 | 해결 방안 |
|---|---|---|
| **Neon sslmode 누락** | JDBC URL에 `sslmode=require`를 빠뜨리면 로컬에서는 되던 연결이 배포 환경에서 실패. `spring.datasource.*`는 필수 값이라 연결 실패 시 앱이 아예 안 뜸 | Neon 콘솔이 제공하는 connection string을 그대로 쓰고, JDBC 형식으로 변환할 때 `jdbc:postgresql://<host>/<db>?sslmode=require`처럼 `sslmode=require`를 반드시 포함시킨다. 로컬 Docker에서 먼저 같은 JDBC URL 형식으로 연결 테스트를 해본다. |
| **pgvector 확장 생성 권한** | `CREATE EXTENSION vector;` 실행 시 권한 오류가 나면 스키마 적용 전체가 멈춤 | Neon은 무료 티어에서도 `vector` 확장을 기본 허용하므로 대부분 그대로 실행되지만, 실패하면 Neon 콘솔의 "Extensions" 메뉴에서 `pgvector`를 먼저 활성화한 뒤 다시 실행한다. |
| **BYTEA 저장 구조 + 무료 DB 용량 한도** | 오디오/비디오/PDF를 DB 컬럼에 직접 저장하는 구조라 Neon 무료 티어(약 0.5GB)가 금방 참 → 용량 초과 시 INSERT 실패로 면접/포트폴리오 기능 전체가 막힐 수 있음 | 포트폴리오 데모용 샘플 데이터는 짧은 오디오(수 초~수십 초)/저해상도 영상으로 제한해서 등록한다. Neon 콘솔에서 스토리지 사용량을 주기적으로 확인하고, 한도에 가까워지면 오래된 테스트 데이터를 지운다. (파일을 S3 등 오브젝트 스토리지로 옮기는 구조 개선은 이번 작업 범위 밖 — 4번 섹션의 보완 작업 후보로만 남겨둔다.) |
| **Render 콜드 스타트** | 15분 이상 요청이 없다가 첫 요청이 오면 수십 초가 걸림. 자동화된 스모크 테스트/헬스체크가 이를 실패로 오판할 수 있음 | 배포 후 검증 스크립트의 타임아웃을 60초 이상으로 넉넉히 잡는다. 포트폴리오 링크 옆에 "첫 로딩은 최대 1분 정도 걸릴 수 있습니다" 안내 문구를 남긴다. 원한다면 UptimeRobot 같은 무료 모니터링으로 주기적으로 핑을 보내 슬립을 늦출 수 있지만, Render 무료 티어의 월 사용 시간 한도(750시간) 안에서만 효과가 있다는 점을 감안한다. |
| **CORS 전체 허용(`*`) 상태로 배포** | 아무 도메인에서나 API를 호출할 수 있는 상태로 운영됨 | 포트폴리오 데모 단계에서는 우선 그대로 두고, Vercel 배포 도메인이 확정되면 `CorsConfig.java`의 `allowedOrigins("*")`를 실제 도메인으로 좁히는 걸 별도로 상의해서 진행한다(코드 수정이 필요한 사항이라 DB 연결/배포 프롬프트 범위에서는 "판단만" 하도록 요청해뒀다). |
| **외부 API 사용료는 이 세 플랫폼(Vercel/Render/Neon)의 무료 티어와 별개** | OpenAI(gpt-4o-mini, text-embedding-3-large), Naver, News API, Google Custom Search는 각각 자체 과금/쿼터 정책을 가진 별도 서비스다. Vercel/Render/Neon이 무료여도 이 API들의 사용량이 많아지면 별도로 비용이 발생하거나 무료 쿼터를 초과해 요청이 막힐 수 있음 | 배포 전에 OpenAI 계정에 사용량 한도(usage limit)를 설정해서 예상치 못한 과금을 막는다. Naver/News API/Google Custom Search는 무료 쿼터(일일 호출 수 등)를 미리 확인하고, 포트폴리오 데모 트래픽이 그 한도를 넘지 않는지 점검한다. |
| **Render 무료 티어 가입 시 결제 수단 등록 요구 가능성** | Render가 계정 확인 목적으로 카드 등록을 요구하는 경우가 있는데, 이것과 "무료 티어 사용료가 청구되는 것"은 다른 문제라 혼동하기 쉬움 | 카드 등록 화면이 나오면 등록 전에 무료 web service 한도 내에서는 청구되지 않는지 Render의 현재 요금 정책을 먼저 확인하고, 확인이 끝나기 전까지는 카드 정보를 입력하지 않는다. |

---

## 4. 포트폴리오에 포함할 보완 작업 아이디어

DB 연결 + 배포만으로도 "동작하는 서비스"는 만들 수 있지만, 아래 항목들은 이 작업을 하면서 자연스럽게 발견한 것들이라 실제 이력을 남기면 포트폴리오에서 어필하기 좋습니다. 전부 필수는 아니고, 시간이 되는 만큼 골라서 진행하면 됩니다. DB 설정 외의 소스 수정이 포함된 항목은 표시해뒀으니, 그 항목들은 반드시 별도로 상의한 뒤 진행하세요.

- **무료 인프라 제약을 고려한 설계 의사결정 문서화** (소스 수정 없음): 왜 Vercel/Render/Neon 조합을 골랐는지, Render 콜드 스타트 같은 제약을 어떻게 고려했는지를 README나 별도 문서로 정리해서 남깁니다. "제한된 자원 안에서 실서비스에 가깝게 설계한 경험"은 이력서/면접에서 그대로 이야깃거리가 됩니다. 이 프롬프트 문서의 6번 섹션(플랫폼 선택 근거)을 초안으로 쓸 수 있습니다.
- **README에 배포 URL·아키텍처 다이어그램 추가** (소스 수정 없음, README만 추가/수정): 프론트엔드/백엔드/DB가 어떻게 연결되는지 간단한 다이어그램과 실제 데모 링크를 추가하면 채용 담당자가 코드를 안 열어봐도 바로 확인할 수 있습니다.
- **기존 mapper XML 버그 수정** (⚠ 소스 수정 필요 — 3-2 참고): `interviewQuestion.xml`의 컬럼/값 개수 불일치, `portfolioguide.xml`의 `deleteAllGuides` 오작동(다른 테이블 삭제) 두 건을 고치고, 커밋 메시지에 "기존 버그 발견 및 수정"이라고 남기면 코드 리뷰 능력을 보여주는 이력이 됩니다.
- **CORS를 배포 도메인으로 제한** (⚠ 소스 수정 필요 — `CorsConfig.java`): 전체 허용(`*`) 상태를 실제 배포 도메인 기준으로 좁히면 보안을 신경 썼다는 근거가 됩니다.
- **SSLHelper의 SSL 검증 우회 코드 검토/제거** (⚠ 소스 수정 필요할 수 있음): 실제로 필요 없는 코드라면 제거하고, 왜 위험한지·왜 제거했는지를 커밋 메시지나 문서에 남기면 보안 인지 수준을 보여줄 수 있습니다.
- **환경변수 온보딩 문서화** (소스 수정 없음, 새 파일 추가): `.env.example` 같은 파일을 새로 추가해서 "어떤 환경변수가 필요한지"를 코드를 안 봐도 알 수 있게 만들면 협업 감각을 보여줄 수 있습니다.
- **간단한 CI(GitHub Actions) 추가** (새 워크플로 파일만 추가): PR/push 시 `./gradlew build`가 통과하는지 자동으로 확인하는 워크플로만 추가해도 "CI/CD를 다뤄본 경험"을 어필할 수 있습니다. 배포 자체를 CI에 태우진 않아도 됩니다(무료 티어에서는 Render/Vercel의 GitHub 연동 자동배포만으로 충분).
- **API 문서화(Swagger/OpenAPI)** (⚠ 의존성 추가 필요): `springdoc-openapi` 같은 라이브러리를 추가해서 API 명세를 자동 생성하면, 배포된 백엔드 URL에 API 문서를 바로 붙여서 보여줄 수 있어 완성도가 높아 보입니다. 새 의존성 추가는 "DB 설정 외 미변경" 원칙에서 벗어나는 항목이라 진행 전 상의가 필요합니다.

이 항목들은 DB 연결/배포 프롬프트(1, 2번)와는 별도로, 배포가 끝난 뒤 새 세션에서 "포트폴리오 보완 작업으로 [항목]을 진행하고 싶어"라고 요청하면 됩니다.

---

## 5. 사용 팁

- 순서: **1번(DB 연결) → 2번(배포)**. DB 연결이 안 된 상태로 배포하면 부팅 자체가 실패합니다 (`spring.datasource.*`가 필수 환경변수라 없으면 앱이 뜨지 않음).
- 새 세션에서 시작한다면, 위 프롬프트 앞에 "먼저 프로젝트 구조와 application.properties, docs/db/schema.sql을 읽어봐"를 붙이면 컨텍스트 파악부터 시작합니다.
- `[ ]`로 표시된 선택지(프론트엔드 경로, 도메인 등)를 프롬프트를 보내기 전에 반드시 채우세요. 비워두면 AI가 임의로 가정하고 진행할 수 있습니다.
- 프론트엔드 저장소가 별도라면, 그 저장소에서 별도 세션을 열어 2번 프롬프트의 "프론트엔드를 Vercel에 배포" 부분만 떼어서 써도 됩니다.

---

## 6. 플랫폼 선택 근거 (참고용)

**"모두 한 플랫폼" vs "역할별로 분리"**: 프론트/백엔드/DB를 무료로 다 올리면서 셋을 한 플랫폼에 몰아넣을 방법이 마땅치 않아서(예: Render는 Java 서비스와 정적 사이트는 잘 지원하지만, 무료 Postgres는 90일 후 자동 삭제됨 — 포트폴리오처럼 오래 유지해야 하는 용도에 부적합) 컴포넌트별로 가장 적합한 무료 플랫폼을 조합하는 쪽을 권장합니다. 오히려 "프론트/백엔드/DB를 각각 특화된 서비스에 배포했다"는 경험 자체가 포트폴리오에서 어필 포인트가 될 수 있습니다.

- **DB 후보 비교**
  - Neon (권장): PostgreSQL, pgvector 지원, 무료 티어 만료 없음, 비활성 시 컴퓨트가 자동 정지(scale-to-zero)했다가 다음 접속 때 자동으로 깨어남(별도 수동 조작 불필요) → 오랜만에 접속하는 채용담당자 데모 시나리오에 유리.
  - Supabase: PostgreSQL, pgvector 지원, 무료 티어 만료는 없지만 일정 기간(약 7일) 미사용 시 프로젝트가 일시정지되고 대시보드에서 수동으로 "Restore"를 눌러야 다시 살아남 → 데모 직전에 미리 깨워둬야 하는 번거로움이 있음.
  - Render Postgres: 무료 DB가 생성 후 90일이 지나면 자동 삭제됨 → 포트폴리오처럼 장기간 유지해야 하는 용도에는 부적합해서 제외.
- **백엔드 후보 비교**
  - Render (권장): Docker 기반 Java 앱을 무료로 상시 배포 가능. 단, 15분 미사용 시 슬립 → 첫 요청 콜드 스타트 발생(수십 초). 포트폴리오 링크에 안내 문구를 남기는 정도로 대응 가능.
  - Railway: 예전엔 상시 무료 티어가 있었지만 현재는 가입 시 일회성 크레딧(트라이얼)만 제공하고 소진되면 결제가 필요해서, "무료로 유지"라는 조건과 맞지 않아 제외.
  - Fly.io: 신규 계정은 결제 카드 등록이 필요하고 무료 사용량 정책이 자주 바뀌어 왔음 → 무료로 확실하게 유지하려는 목적에는 Render보다 불확실성이 커서 2순위.
- **프론트엔드 (Vue.js, 확정)**: Vercel/Netlify 둘 다 정적/SPA 호스팅 무료 티어가 넉넉하고 만료가 없어서 후보로는 동급이지만, Vercel은 Vue(Vite/Vue CLI)를 공식 프레임워크 프리셋으로 지원해서 빌드 설정을 거의 자동으로 잡아주고, 카드 등록 없이 무료로 커스텀 도메인·HTTPS까지 제공하므로 Vercel로 확정했습니다. `vue-router` history 모드를 쓴다면 `vercel.json`에 SPA rewrite 규칙만 추가하면 배포·운영에 문제가 없습니다.

**주의할 제약사항 (코드 변경과 무관하게 알아둘 것)**:
- 이 프로젝트는 오디오/비디오/PDF 파일을 파일 스토리지가 아니라 DB 컬럼(BYTEA)에 직접 저장하는 구조입니다 (`interview_answer.audio_file_data`, `interview_answer.video_file_data`, `interview_session.document_file_data`, `portfolio.pdf_file`). Neon 무료 티어 저장 용량(약 0.5GB)을 이 구조 그대로 쓰면 면접 영상 몇 개만 올려도 금방 찰 수 있습니다. 코드를 바꾸지 않는 선에서는 "데모용 샘플 데이터를 작게 유지"하는 정도로 대응하고, 장기적으로는 파일을 오브젝트 스토리지(S3 등)로 옮기는 구조 변경을 고려해볼 만합니다(단, 이번 작업 범위에는 포함하지 않음).
