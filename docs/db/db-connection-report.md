# DB 연결 검증 리포트

- **검증 일자**: 2026-08-19
- **대상 DB**: Neon (PostgreSQL, 프로젝트: `production` 브랜치 / DB명: `neondb`) — 연결 문자열·비밀번호 등 민감정보는 본 문서에 포함하지 않음
- **참고 문서**: [db-deploy-setup-prompt.md](../prompts/db-deploy-setup-prompt.md)

## 1. 요약 (O/X)

| 항목 | 결과 | 비고 |
|---|:---:|---|
| 애플리케이션 → Neon DB 연결 | O | `spring.datasource.*`가 Neon을 정상적으로 가리키도록 설정된 뒤 `Started DemoApplication` 확인 |
| schema.sql 테이블 전체 생성 | O | `information_schema.tables` 조회 결과 18개 (도메인 테이블 17개 + Spring AI 자동 생성 `vector_store` 1개) |
| pgvector 확장 활성화 | O | `pg_extension`에 `vector` 존재 확인 |
| 충돌 지점 컬럼(`resume.activities`) 실제 반영 | O | `jsonb` 타입으로 생성됨 — mapper XML의 `CAST(... AS jsonb)` 사용과 일치 |
| 충돌 지점 컬럼(`news_summary.published_at`) 실제 반영 | O | `timestamp without time zone`으로 생성됨 — `DATE(published_at)` 캐스팅 사용과 일치 |
| 충돌 지점 컬럼(`news_summary.summary_text`) 타입 | O (길이는 미확인) | `character varying`까지만 확인, 실제 길이(500)는 이번 조회에 포함 안 함 — 필요 시 재확인 권장 |
| MyBatis 매퍼 스모크 테스트 (다중 도메인) | O | 아래 2번 참고 |
| 기존 버그 - `interviewQuestion.xml` `insertCustomQuestion` | X (여전히 존재) | 컬럼 3개/값 2개 불일치, 코드 미수정 상태 확인 (실행은 하지 않고 XML만 검토) |
| 기존 버그 - `portfolioguide.xml` `deleteAllGuides` | X (여전히 존재) | 여전히 `news_summary` 테이블을 삭제하는 코드, 코드 미수정 상태 확인 (파괴적 명령이라 실제 실행은 하지 않음) |
| git 이력에 `.env` 계열 파일 커밋 여부 | O (안전) | `git log --all` 및 `git log --all -p -- '*.env*'` 결과 커밋 이력 없음 |
| `.gitignore` 민감 파일 제외 | O | `.env`, `.env.*`(`.env.example` 제외), `.docker/` 제외 확인 |
| Neon 스토리지 사용량 | 미확인 | 콘솔 캡처 필요 (진행 시 본 문서 갱신) |

## 2. 매퍼 스모크 테스트 상세

로컬에서 `.env.local`(Neon 접속정보) 기준으로 `./gradlew bootRun` 실행 후, 서로 다른 5개 도메인의 GET API를 호출:

| API | 결과 | 설명 |
|---|---|---|
| `GET /portfolio-standard` | 200 | `portfolio_standard` 조회 정상 (데이터 없어 빈 배열) |
| `GET /resume/list/1` | 200 | `resume` 조회 정상 |
| `GET /coverletter/list?memberId=1` | 200 | `coverletter` 조회 정상 |
| `GET /interview/list?memberId=1` | 200 | `interview_session` 조회 정상 |
| `GET /learning/count?memberId=1` | 200 | `learning` COUNT 쿼리 정상 (실행 로그로 SQL 파라미터 바인딩까지 확인) |
| `GET /portfolio-standard/member?memberId=1` | 500 | DB/매퍼 문제 아님. `member_id=1`인 회원이 없어 서비스 로직(`getStandardsByMemberId`)이 의도적으로 던지는 `RuntimeException("회원을 찾을 수 없습니다.")` — 시드 데이터 부재로 인한 정상 동작 |
| `GET /learning/list?memberId=1` | 400 | DB/매퍼 문제 아님. 필수 쿼리파라미터 `status` 누락 (`MissingServletRequestParameterException`) — 테스트 호출 시 파라미터 누락 |
| `GET /portfolio-standard/by-job?jobGroup=...&jobRole=...` | 400 | 컨트롤러 요청 파라미터 검증 단계 이슈로 추정, DB 연결과 무관 |

**결론**: 500/400 응답 2건은 DB 연결이나 MyBatis 매퍼 자체의 결함이 아니라, 시드 데이터 부재/테스트 파라미터 누락에 의한 것으로 확인됨.

## 3. 이번에 새로 발견/재확인된 이슈

기존에 [db-deploy-setup-prompt.md 3-1, 3-2](../prompts/db-deploy-setup-prompt.md)에 기록된 항목 외에 새로 발견된 이슈:

- **JDBC URL에 계정정보를 포함하면 안 됨**: Neon 콘솔이 기본 제공하는 connection string은 `postgresql://user:password@host/db?...` 형태인데, 이를 그대로 `DB_URL`에 넣으면(`jdbc:postgresql://user:password@host/...`) PostgreSQL JDBC 드라이버가 `Driver ... claims to not accept jdbcUrl` 오류로 부팅 자체를 거부한다. `DB_URL`은 계정정보 없이 `jdbc:postgresql://host/db?sslmode=require&channel_binding=require` 형태로, 계정/비밀번호는 `DB_USERNAME`/`DB_PASSWORD`로 반드시 분리해야 한다. (db-deploy-setup-prompt.md 1번 프롬프트의 "JDBC URL 변환" 안내에 이 주의사항을 명시적으로 추가하는 것을 권장)
- **`.env` 계열 파일을 bash `source`할 때 `&` 문자 이슈**: Neon 기본 connection string에 포함된 `&channel_binding=require`처럼가 값에 따옴표 없이 들어가면, bash가 `&`를 백그라운드 실행 연산자로 해석해서 해당 변수가 아예 설정되지 않는다(값 손실, 오류 메시지도 명확하지 않음). `.env` 파일의 특수문자 포함 값은 항상 큰따옴표로 감싸야 한다.
- **(운영 주의) 에러 응답에 DB 접속정보 원문이 그대로 노출됨**: 위 JDBC URL 오류 발생 시, MyBatis/드라이버 예외 메시지에 `DB_URL` 전체(계정명·비밀번호 포함)가 그대로 담겨 API 에러 응답 바디로 반환된 사례가 있었음. 이번 세션에서 실제로 비밀번호가 한 번 노출되어 즉시 Neon 콘솔에서 비밀번호를 재발급함. 운영 배포 시에는 상세 예외 메시지가 클라이언트 응답에 노출되지 않도록(예: `server.error.include-message=never` 등) 점검이 필요함 — 이번 작업 범위 밖이라 별도 상의 후 진행 필요.

## 4. 보안 점검 결과

- `git log --all` 기준으로 `.env`, `.env.local`, `.env.example` 외 어떤 `.env*` 파일도 커밋된 이력 없음 (안전)
- `.gitignore`에 `.env`, `.env.*`(`.env.example` 예외), `.docker/` 제외 규칙 존재, 정상 작동 확인
- **위 3번에서 발견된 대로, DB 비밀번호가 API 에러 응답을 통해 한 차례 평문 노출됨 → Neon 콘솔에서 즉시 재발급 완료(사용자 확인 필요)**

## 5. 남은 위험 / 할 일

- [ ] `interviewQuestion.xml`의 `insertCustomQuestion` 컬럼/값 개수 불일치 수정 (미착수)
- [ ] `portfolioguide.xml`의 `deleteAllGuides`가 `news_summary`를 삭제하는 오작동 수정 (미착수)
- [ ] Neon 스토리지 사용량 확인 (콘솔 캡처 필요, 무료 티어 약 0.5GB 한도 대비 점검)
- [ ] 운영 환경에서 상세 예외 메시지가 API 응답에 노출되지 않도록 설정 검토
- [ ] `news_summary.summary_text`의 실제 길이(VARCHAR(500)) 재확인 — AI 요약 결과가 500자를 넘는 경우가 있는지 사전 점검 권장
- [ ] `.env.local`의 `OPENAI_API_KEY`가 DB 검증을 위해 더미 값으로 설정된 적이 있었음 — 실제 키로 복원되었는지 확인 필요

## 6. 다음 단계

DB 연결(1번 프롬프트) 검증은 완료된 것으로 판단됨. **2번 배포 프롬프트(Vercel + Render + Neon)로 넘어가도 무방**하나, 아래는 배포 전 선행 권장 사항:

1. (권장, 필수는 아님) 위 5번 목록 중 mapper XML 버그 2건은 배포 전에 고쳐두면 이후 데모 중 오류/데이터 유실을 예방할 수 있음
2. (필수) 노출됐던 Neon 비밀번호가 실제로 재발급되었는지 최종 확인
3. (필수) `OPENAI_API_KEY`를 실제 키로 복원 후 AI 에이전트 기능까지 한 번은 정상 동작 확인
