-- second-llbky-back 스키마
--
-- 출처 및 우선순위: 1) mapper XML(src/main/resources/mapper/*.xml — 실제 애플리케이션이
-- 참조하는 컬럼명/타입, entity 클래스의 필드 타입까지 포함) 2) ERD(사용자 제공 다이어그램)
-- 3) 사용자가 제공한 원본 SQL 순으로 신뢰도를 두고 교차 검증함.
-- 세 출처가 100% 일치하지는 않아서 충돌이 나면:
--   - XML(+entity)이 명확한 근거(컬럼 존재 여부, Java 필드 타입 등)를 주면 그 값을 그대로 채택.
--   - XML이 컬럼 존재만 알려주고 타입/길이 정보가 없는 경우(mapper XML은 DDL이 아니라서 대부분 여기 해당)에는
--     ERD 값을 채택.
--   - XML과 ERD 둘 다 정보가 없으면 원본 SQL 값을 채택.
-- 각 충돌 지점과 근거는 아래 주석에 표시해뒀다. 자세한 배경과 위험도는
-- docs/prompts/db-deploy-setup-prompt.md 의 "위험 관리" 섹션 참고.

-- pgvector 확장 (spring-ai-starter-vector-store-pgvector 의존성이 있어서 필요)
CREATE EXTENSION IF NOT EXISTS vector;

---멤버
CREATE TABLE member (
    member_id      SERIAL PRIMARY KEY,
    member_name           VARCHAR(100) NOT NULL,
    login_id       VARCHAR(200) UNIQUE,
    member_password       VARCHAR(255) NOT NULL,
    member_email          VARCHAR(255) UNIQUE NOT NULL,
    job_group      VARCHAR(100),
    job_role       VARCHAR(100),
    career_years   INT,
    created_at     TIMESTAMP DEFAULT NOW(),
    updated_at     TIMESTAMP DEFAULT NOW()
);

---면접 세션
CREATE TABLE interview_session (
    session_id        SERIAL PRIMARY KEY,
    member_id         INT NOT NULL REFERENCES member(member_id) ON DELETE CASCADE,
    interview_type    VARCHAR(100) NOT NULL,
    target_company    VARCHAR(100),
    document_file_name VARCHAR(255),
    document_file_type VARCHAR(100),
    document_file_data BYTEA,
    report_feedback   JSONB,
    created_at        TIMESTAMP DEFAULT NOW(),
    updated_at        TIMESTAMP DEFAULT NOW()
);

---면접 예상 질문
CREATE TABLE interview_question (
    question_id    SERIAL PRIMARY KEY,
    session_id     INT NOT NULL REFERENCES interview_session(session_id) ON DELETE CASCADE,
    question_text  TEXT NOT NULL,
    created_at     TIMESTAMP DEFAULT NOW()
);

---면접 답변
CREATE TABLE interview_answer (
    answer_id        SERIAL PRIMARY KEY,
    question_id      INT NOT NULL REFERENCES interview_question(question_id) ON DELETE CASCADE,
    audio_file_name  VARCHAR(255),
    audio_file_type  VARCHAR(100),
    audio_file_data  BYTEA,
    video_file_name  VARCHAR(255),
    video_file_type  VARCHAR(100),
    video_file_data  BYTEA,
    answer_text      TEXT,
    answer_feedback  JSONB,
    created_at       TIMESTAMP DEFAULT NOW(),
    updated_at       TIMESTAMP DEFAULT NOW()
);

---자소서
CREATE TABLE coverletter (
    coverletter_id   SERIAL PRIMARY KEY,
    member_id        INT NOT NULL REFERENCES member(member_id) ON DELETE CASCADE,

    title               VARCHAR(200),
    support_motive      TEXT,
    growth_experience   TEXT,
    job_capability      TEXT,
    future_plan         TEXT,

    cover_feedback      JSONB,

    created_at          TIMESTAMP DEFAULT NOW(),
    updated_at          TIMESTAMP DEFAULT NOW()
);

--- 학습
-- [충돌] title/status 길이: XML에는 길이 정보가 없어서 2순위인 ERD 값(title VARCHAR(200), status VARCHAR(100))을
--        채택. 원본 SQL은 각각 (100)/(20)이었음.
-- [주의] ERD 다이어그램에는 learning_summary/learning_recommendation 컬럼이 그려져 있지 않지만,
--        mapper XML(learning.xml)의 select/update가 이 두 컬럼을 실제로 사용하므로 삭제하지 않고 유지함(ERD 누락으로 판단).
CREATE TABLE learning (
    learning_id SERIAL PRIMARY KEY,
    member_id INT NOT NULL REFERENCES member(member_id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,

    status VARCHAR(100) NOT NULL,

    learning_summary TEXT,
    learning_recommendation TEXT,

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

--- 주별 학습
-- [충돌] title/status 길이: XML에는 길이 정보가 없어서 2순위인 ERD 값(title VARCHAR(200), status VARCHAR(100))을
--        채택. 원본 SQL은 title VARCHAR(255), status VARCHAR(20)이었음.
-- [주의] goal, learning_week_summary는 ERD에 없지만(1순위 XML 근거로 존재 확인) learningWeek.xml이 실제로 사용해서 유지함.
CREATE TABLE learning_week (
    week_id SERIAL PRIMARY KEY,
    learning_id INT NOT NULL REFERENCES learning(learning_id) ON DELETE CASCADE,

    week_number INT NOT NULL,
    title VARCHAR(200),
    goal TEXT,
    status VARCHAR(100) NOT NULL,
    learning_week_summary TEXT,

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

--- 일별 학습
-- [충돌] title/status 길이: XML에는 길이 정보가 없어서 2순위인 ERD 값(title VARCHAR(200), status VARCHAR(100))을
--        채택. 원본 SQL은 title VARCHAR(255), status VARCHAR(20)이었음.
-- [주의] study_time_min은 ERD에 없지만(1순위 XML 근거로 존재 확인) learningDay.xml의 select/update가 사용해서 유지함.
CREATE TABLE learning_day (
    day_id SERIAL PRIMARY KEY,
    week_id INT NOT NULL REFERENCES learning_week(week_id) ON DELETE CASCADE,

    day_number INT NOT NULL,
    title VARCHAR(200),
    content TEXT,
    study_time_min INT,
    status VARCHAR(100),
    learning_day_summary TEXT,

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 이력서
-- [충돌 - XML 근거로 확정] ERD 컬럼명은 "activity"(단수)이지만 mapper XML(resume.xml)은
-- insert/select/update 12곳 전부 "activities"(복수)를 사용함. 우선순위 1순위인 XML이 명확하므로
-- "activities"를 채택. ERD 다이어그램 쪽이 오래된 것으로 보이니, 다음에 ERD를 갱신할 기회가 있으면
-- "activity" → "activities"로 고쳐서 문서와 코드를 맞춰둘 것 (docs/prompts/db-deploy-setup-prompt.md 참고).
CREATE TABLE resume (
    resume_id SERIAL PRIMARY KEY,
    member_id INT NOT NULL REFERENCES member(member_id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL DEFAULT '새 이력서',

    career_info JSONB,
    education_info JSONB,
    skills JSONB,
    certificates JSONB,
    awards JSONB,
    activities JSONB,

    resume_feedback JSONB,

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 포트폴리오
-- [충돌] original_filename 길이: XML/entity에는 길이 정보가 없어서 2순위인 ERD 값(VARCHAR(200))을 채택.
--        원본 SQL은 VARCHAR(255)였음.
CREATE TABLE portfolio (
    portfolio_id SERIAL PRIMARY KEY,
    member_id INT NOT NULL REFERENCES member(member_id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL DEFAULT '새 포트폴리오',
    pdf_file BYTEA,
    original_filename VARCHAR(200),
    content_type VARCHAR(100) DEFAULT 'application/pdf',
    page_count INTEGER,
    portfolio_feedback JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 포트폴리오 페이지별 피드백
CREATE TABLE portfolio_image (
    image_id SERIAL PRIMARY KEY,
    portfolio_id INT NOT NULL REFERENCES portfolio(portfolio_id) ON DELETE CASCADE,
    page_no INT NOT NULL,
    page_feedback JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

-- 포트폴리오 평가 표준 (템플릿)
CREATE TABLE portfolio_standard (
    standard_id SERIAL PRIMARY KEY,
    standard_name VARCHAR(100) NOT NULL,
    standard_description TEXT,
    prompt_template TEXT NOT NULL,
    weight_percentage INTEGER DEFAULT 20,
    job_group VARCHAR(100),
    job_role VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    evaluation_items TEXT,
    score_range_description TEXT
);

-- 사용자별 포트폴리오 가이드
CREATE TABLE portfolio_guide (
    guide_id SERIAL PRIMARY KEY,
    member_id INT NOT NULL REFERENCES member(member_id) ON DELETE CASCADE,
    standard_id INT NOT NULL REFERENCES portfolio_standard(standard_id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL DEFAULT '새 포트폴리오 가이드',

    guide_content JSONB,

    completion_percentage INTEGER DEFAULT 0 CHECK (completion_percentage >= 0 AND completion_percentage <= 100),
    is_completed BOOLEAN DEFAULT FALSE,
    current_step INTEGER DEFAULT 1,
    total_steps INTEGER DEFAULT 5,

    guide_feedback JSONB,

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

---뉴스 요약
-- [충돌 - XML(+entity) 근거로 확정] published_at: ERD는 DATE, 원본 SQL은 TIMESTAMP.
--        newsSummary.xml이 "DATE(published_at) = CURRENT_DATE" 처럼 published_at에 DATE() 함수를
--        직접 적용해서 걸러내고 있고(이미 DATE 타입이면 의미 없는 캐스팅), 대응하는 엔티티
--        NewsSummary.java의 publishedAt 필드도 LocalDateTime으로 선언돼 있어 시간 정보까지
--        저장/사용한다는 근거가 명확함 → 우선순위 1순위인 XML/entity 근거를 따라 TIMESTAMP 채택
--        (ERD보다 우선). ERD 다이어그램 쪽을 나중에 TIMESTAMP로 갱신해둘 것.
-- [충돌] summary_text: ERD는 VARCHAR(500), 원본 SQL은 VARCHAR(1000). XML/entity에는 길이 정보가 없어서
--        2순위인 ERD 값을 채택. (AI가 생성하는 3줄 요약이 500자를 넘기면 INSERT 시
--        "value too long" 오류가 날 수 있음 — 위험 관리 섹션 참고. 실제 운영 전에 길이 재검증 권장)
CREATE TABLE news_summary (
    summary_id      SERIAL PRIMARY KEY,
    member_id       INT REFERENCES member(member_id) ON DELETE CASCADE,

    source_name     VARCHAR(200),
    source_url      VARCHAR(1000),
    title           VARCHAR(500),
    published_at    TIMESTAMP,

    summary_text    VARCHAR(500),
    detail_summary  TEXT,

    analysis_json   JSONB,
    keywords_json   JSONB,

    created_at      TIMESTAMP DEFAULT NOW()
);

---저장된 키워드
CREATE TABLE saved_keyword (
    saved_keyword_id SERIAL PRIMARY KEY,
    member_id        INT REFERENCES member(member_id) ON DELETE CASCADE,

    keyword          VARCHAR(200) NOT NULL,
    source_label     VARCHAR(200),

    created_at       TIMESTAMP DEFAULT NOW()
);

---트렌드 분석
CREATE TABLE trend_insight (
    trend_id     SERIAL PRIMARY KEY,
    member_id    INT NOT NULL REFERENCES member(member_id) ON DELETE CASCADE,

    start_date   DATE NOT NULL,
    end_date     DATE NOT NULL,

    trend_json   JSONB NOT NULL,
    insight_json JSONB NOT NULL,

    created_at   TIMESTAMP DEFAULT NOW()
);

---직무 인사이트
CREATE TABLE job_insight (
    insight_id        SERIAL PRIMARY KEY,
    member_id         INT REFERENCES member(member_id) ON DELETE CASCADE,

    analysis_json     JSONB NOT NULL,
    related_jobs_json JSONB,

    created_at        TIMESTAMP DEFAULT NOW()
);

-- 참고: 코드베이스에 Spring AI VectorStore(pgvector)를 실제로 사용하는 로직은
-- 아직 없음(spring-ai-starter-vector-store-pgvector는 의존성으로만 존재).
-- 향후 RAG 기능을 붙일 경우 Spring AI가 기대하는 기본 테이블(vector_store)을
-- spring.ai.vectorstore.pgvector.initialize-schema=true 로 자동 생성하거나
-- 아래처럼 수동으로 만들 수 있음 (임베딩 차원은 사용 모델에 맞게 조정, 예: text-embedding-3-large는
-- 기본 3072차원이며 dimensions 파라미터로 축소 가능):
--
-- CREATE TABLE IF NOT EXISTS vector_store (
--     id UUID PRIMARY KEY,
--     content TEXT,
--     metadata JSONB,
--     embedding VECTOR(3072)
-- );
-- CREATE INDEX IF NOT EXISTS vector_store_embedding_idx ON vector_store
--     USING hnsw (embedding vector_cosine_ops);
