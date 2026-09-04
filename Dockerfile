# ---- Build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 의존성 캐시를 위해 gradle 관련 파일 먼저 복사
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew

# 소스 복사 후 빌드 (테스트는 배포 이미지 빌드 단계에서 생략 — CI에서 별도로 돌리는 걸 권장)
COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon

# ---- Run stage ----
FROM eclipse-temurin:21-jre AS run
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

# Render는 컨테이너가 리슨하는 포트를 PORT 환경변수로 알려준다.
# application.properties의 server.port=8080이 기본값이라, PORT가 안 오면 8080으로 폴백.
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT}"]
