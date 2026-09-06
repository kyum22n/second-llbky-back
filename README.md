LLBKY (Career Coach)
AI가 이력서·자소서·면접·학습·트렌드 분석까지 취업 준비 전 과정을 코칭해주는 AI 커리어 코칭 플랫폼

📌 프로젝트 소개
취업 준비 과정에서 지원자들은 이력서·자소서 첨삭, 면접 대비, 직무별 학습 계획 수립, 채용 시장 트렌드 파악 등 서로 다른 성격의 문제를 각각 다른 서비스와 방법으로 해결해야 하는 어려움을 겪습니다. LLBKY는 이 흩어진 과정을 하나의 플랫폼에서 AI 에이전트 기반으로 지원하는 것을 목표로 시작한 프로젝트입니다.

이력서/자소서를 업로드하면 AI가 실시간으로 코칭하고, 모의 면접 질문 생성부터 오디오/영상 답변에 대한 피드백까지 제공하며, 목표 직무에 맞는 학습 로드맵을 주/일 단위로 생성해주고, 채용 트렌드·뉴스·직무 인사이트를 분석해 보여줍니다. 이 저장소는 팀 프로젝트로 진행했던 llbky-back을 기반으로, 개인 포트폴리오 목적의 보완 개발(DB 재연동, 배포 환경 구축, 보안 강화 등)을 위해 별도로 분리한 저장소이며, 원본 프로젝트의 커밋 이력은 포함하지 않고 이 저장소부터의 커밋은 개인 작업입니다.

🛠️ Tech Stack
Backend
Java 21
Spring Boot 3.4.11
Spring AI 1.0.3 (OpenAI gpt-4o-mini, 임베딩 text-embedding-3-large, pgvector 연동)
MyBatis
Apache PDFBox / iText7 (PDF 생성·파싱)
Jsoup (뉴스 크롤링)
Frontend
Vue.js
TypeScript
Database
PostgreSQL (+ pgvector extension)
Neon (Serverless Postgres)
Infrastructure
Docker (멀티스테이지 빌드)
Render (백엔드 배포)
Vercel (프론트엔드 배포)
✨ 주요 기능
이력서 · 자소서 AI 코칭
이력서/자소서를 저장하면 AI가 실시간 코칭, 문체 스타일 적용, 최종 피드백을 제공합니다. 각 도메인은 전용 AI 에이전트(ai/resume, ai/coverletter 패키지)를 통해 분석 결과를 리포트 형태로 반환합니다.

AI 모의 면접
지원 직무에 맞는 면접 질문을 AI가 생성하고, 음성/영상으로 제출한 답변을 STT·시각 분석 에이전트가 분석하여 최종 피드백과 리포트를 제공합니다. 지원 회사 검색과 이상적 인재상 조회 기능도 함께 제공합니다.

맞춤 학습 로드맵
목표 직무를 기반으로 스킬을 추천하고, 주 단위·일 단위 로드맵을 생성/보완합니다. 사용자는 일일 학습 요약을 제출하며 로드맵을 관리할 수 있습니다.

채용 트렌드 · 뉴스 분석 & 포트폴리오 가이드
오늘의 채용 뉴스와 트렌드를 AI가 요약·분석하고, 관심 키워드를 저장해 직무 인사이트를 확인할 수 있습니다. 또한 포트폴리오 PDF를 업로드하면 페이지별 AI 피드백과 평가 표준에 맞춘 맞춤 가이드를 제공합니다.

🏗️ 시스템 아키텍처
[Vue.js Frontend] → Vercel
        │  (REST API, CORS 허용 도메인 제한)
        ▼
[Spring Boot Backend] → Render (Docker, 멀티스테이지 빌드)
        │  (MyBatis)
        ▼
[PostgreSQL + pgvector] → Neon
        │
        └─ OpenAI API (Spring AI / gpt-4o-mini, text-embedding-3-large)
프론트엔드(Vue.js)는 Vercel, 백엔드(Spring Boot)는 Render, 데이터베이스는 Neon으로 분리 배포한 3-tier 무료 티어 구조입니다. /health 엔드포인트를 통해 Render의 헬스체크를 지원하며, CORS는 초기 전체 허용에서 실제 배포된 프론트엔드 도메인으로 제한하도록 보안을 강화했습니다.

🗄️ ERD
주요 도메인 테이블 (docs/db/schema.sql)

member — 회원 정보(로그인 ID, 직군/직무/경력)
resume — 이력서(경력/학력/스킬/자격증/수상/활동 정보를 JSONB로 저장)
coverletter — 자소서(지원동기/성장경험/직무역량/포부 + AI 피드백 JSONB)
interview_session / interview_question / interview_answer — 면접 세션·질문·답변(오디오/영상 BYTEA, 피드백 JSONB)
learning / learning_week / learning_day — 학습 로드맵(주/일 단위 계층 구조)
portfolio / portfolio_image / portfolio_standard / portfolio_guide — 포트폴리오 PDF, 페이지별 피드백, 평가 표준, 맞춤 가이드
news_summary / saved_keyword / trend_insight / job_insight — 뉴스 요약, 저장 키워드, 트렌드/직무 인사이트
vector_store — pgvector 확장을 위한 임베딩 저장 테이블(향후 RAG 확장 대비)
💡 주요 구현
1. 도메인별 AI 에이전트 구조
컨트롤러-서비스 계층과 별도로 ai 패키지 아래 도메인별(이력서/자소서/면접/학습/뉴스·트렌드/포트폴리오) AI 에이전트를 분리 설계했습니다. 예를 들어 면접 도메인은 질문 생성(CreateQuestionAgent), 답변 피드백(AnswerFeedbackAgent), 음성 인식(STTAgent), 영상 분석(VisualAnalysisAgent)처럼 역할을 세분화된 에이전트 단위로 나누어, 서비스 로직과 AI 프롬프트/응답 처리 로직의 책임을 분리했습니다.

2. 팀 프로젝트 분리 후 DB 및 배포 환경 복구
개인 저장소로 분리하면서 사라진 DB 연동을 스키마부터 재구성했습니다. MyBatis 매퍼 XML, ERD, 원본 SQL 세 가지 근거를 교차 검증해 컬럼 불일치(예: resume.activities(복수형) vs 문서상 activity(단수형))를 해결하고, Neon(PostgreSQL) 기반으로 DB를 재연동했습니다. 이후 Docker 멀티스테이지 빌드로 이미지를 구성하고 Render 배포, 헬스체크 엔드포인트 추가, CORS 제한 순으로 배포 환경을 단계적으로 복구·검증했습니다.

🧪 테스트
현재는 Spring Boot 컨텍스트 로딩을 검증하는 기본 스모크 테스트(DemoApplicationTests)만 포함되어 있으며, 도메인별 단위/통합 테스트는 추후 보강 예정입니다. 실제 API 동작 검증은 배포 후 curl 기반 수동 스모크 테스트로 진행했고, 그 결과와 발견된 이슈는 docs/db/db-connection-report.md, docs/deploy/deployment-report.md에 기록되어 있습니다.

🚀 배포
백엔드: Docker 이미지를 빌드해 Render Web Service(무료 티어)에 배포. Render가 주입하는 PORT 환경변수를 반영해 실행됩니다.
프론트엔드: Vue.js 애플리케이션을 Vercel에 배포.
데이터베이스: Neon(Serverless PostgreSQL, pgvector 확장 포함)을 사용하며, 별도 마이그레이션 도구 없이 docs/db/schema.sql을 기준으로 스키마를 관리합니다.
GitHub 저장소 연동을 통해 Render/Vercel에서 각각 자동 배포되며, 별도의 CI 파이프라인은 아직 구성하지 않았습니다.
📂 프로젝트 구조
second-llbky-back
├── Dockerfile
├── docs
│   ├── db                     # 스키마 정의, DB 연동 검증 보고서
│   ├── deploy                 # 배포 검증 보고서
│   └── prompts                # 작업 프롬프트 기록
├── src
│   ├── main
│   │   ├── java/com/example/demo
│   │   │   ├── ai             # 도메인별 AI 에이전트 (resume, coverletter, interview, learning, newstrend, portfolio, portfolioguide)
│   │   │   ├── config         # CORS, SSL 등 공통 설정
│   │   │   ├── exception       # 전역 예외 처리
│   │   │   ├── health          # 헬스체크
│   │   │   ├── member          # 회원 (controller/dao/dto/entity/service)
│   │   │   ├── resume          # 이력서
│   │   │   ├── coverletter     # 자소서
│   │   │   ├── interview       # 면접
│   │   │   ├── learning        # 학습 로드맵
│   │   │   ├── newstrend       # 뉴스/트렌드
│   │   │   └── portfolio       # 포트폴리오
│   │   └── resources
│   │       ├── application.properties
│   │       ├── mapper-config.xml
│   │       ├── mapper/*.xml   # MyBatis 매퍼
│   │       └── fonts          # PDF용 한글 폰트
│   └── test
│       └── java/com/example/demo
└── build.gradle
