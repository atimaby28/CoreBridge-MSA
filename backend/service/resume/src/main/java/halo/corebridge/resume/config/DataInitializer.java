package halo.corebridge.resume.config;

import halo.corebridge.common.snowflake.Snowflake;
import halo.corebridge.resume.model.entity.Resume;
import halo.corebridge.resume.model.entity.ResumeVersion;
import halo.corebridge.resume.repository.ResumeRepository;
import halo.corebridge.resume.repository.ResumeVersionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 시작 시 샘플 이력서를 생성하는 초기화 클래스.
 * - user1@test.com (양승우) 계정의 이력서 1건
 * - 이미 데이터가 있으면 스킵
 */
@Slf4j
@Component
public class DataInitializer implements ApplicationRunner {

    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final Snowflake snowflake;

    public DataInitializer(ResumeRepository resumeRepository,
                           ResumeVersionRepository resumeVersionRepository) {
        this.resumeRepository = resumeRepository;
        this.resumeVersionRepository = resumeVersionRepository;
        this.snowflake = new Snowflake();
    }

    // User 서비스 DataInitializer의 USER1_ID와 동일 (Snowflake 범위)
    private static final Long USER_ID = 11234028028076038L;

    @Override
    public void run(ApplicationArguments args) {
        if (resumeRepository.count() > 0) {
            log.info("이력서 데이터가 이미 존재합니다. 초기화 스킵.");
            return;
        }

        String title = "양승우 - 백엔드 개발자 이력서";

        String content = """
                # 양승우
                
                ## 기본 정보
                - 희망 직무: 백엔드 개발자
                - 이메일: auqageek94@naver.com
                
                ## 학력
                - 전남대학교 IoT AI 융합전공 (2015 ~ 2021)
                  - 학점: 3.59 / 4.5
                  - 부전공: 해양학 (3.38 / 4.5)
                
                ## 경력
                
                ### 한화시스템 BEYOND SW Camp 6기 (2025.05 ~ 2025.11)
                - MSA 기반 채용 관리 플랫폼 (CoreBridge) 백엔드 설계 및 구현
                - 13개 마이크로서비스 아키텍처 설계, 6가지 핵심 패턴 적용
                - 수료 우수상 수상
                
                ### FIS 인턴 (2023.03 ~ 2023.12)
                - 금융 데이터 처리 업무 자동화
                - 수작업 30분 → 8초로 단축 (자동화 스크립트 개발)
                
                ### NIA 인턴 (2022)
                - 공공데이터 품질 검증 및 분석 업무
                
                ## 기술 스택
                - Backend: Java, Spring Boot, Spring Cloud Gateway, JPA/Hibernate
                - Database: PostgreSQL, MySQL, Redis
                - Messaging: Apache Kafka (Outbox Pattern)
                - DevOps: Docker, Kubernetes(K3s), Jenkins CI/CD
                - AI: Python, FastAPI, Ollama (LLM), Redis Vector Search
                - Frontend: Vue.js 3, TypeScript, Pinia, Tailwind CSS
                - Monitoring: Prometheus, Grafana
                
                ## 프로젝트 경험
                
                ### CoreBridge-MSA — 채용 관리 플랫폼 (2025)
                - 역할: 백엔드 아키텍트 (13개 서비스 설계 및 구현)
                - 핵심 패턴:
                  - Outbox Pattern + Kafka: 서비스 간 데이터 일관성 보장
                  - Circuit Breaker (Resilience4j): 장애 전파 차단
                  - CQRS + ConcurrentHashMap 캐시: 조회 성능 3배 향상
                  - AI Pipeline (FastAPI + Ollama + n8n): 99.7% 응답시간 단축 (80초 → 100ms)
                  - API Gateway: JWT 중앙 검증 + Cookie 기반 인증
                  - K3s + Jenkins: Rolling Update 무중단 배포
                - 기술: Spring Boot 3.4, PostgreSQL, Kafka, Redis, Docker, K3s
                - 성과: Java 231파일 + 테스트 27개
                
                ## 자격증
                - AWS Solutions Architect Associate
                - 정보처리기사
                - 빅데이터분석기사
                - SQLD
                - PCCP Level 2
                
                ## 어학
                - TOEIC 815
                - TOEIC Speaking IM3
                
                ## 자기소개
                MSA 아키텍처와 분산 시스템에 깊은 관심을 가진 백엔드 개발자입니다.
                단순 구현이 아닌 "왜 이 기술을 선택했는가"에 대한 근거를 항상 고민합니다.
                Outbox Pattern 도입 시 CDC(Debezium) 대비 운영 복잡도를 비교 분석하고,
                Circuit Breaker 파라미터를 실험적으로 튜닝하며,
                AI Pipeline에서 동기 처리의 스레드 풀 고갈 문제를 n8n 비동기로 해결한 경험이 있습니다.
                """;

        String skills = "[\"Java\", \"Spring Boot\", \"PostgreSQL\", \"Kafka\", \"Docker\", " +
                "\"Kubernetes\", \"Redis\", \"JPA\", \"Vue.js\", \"TypeScript\", " +
                "\"FastAPI\", \"Python\", \"Jenkins\"]";

        // Resume 생성
        long resumeId = snowflake.nextId();
        Resume resume = Resume.create(resumeId, USER_ID, title, content);
        resume.updateSkills(skills);
        resumeRepository.save(resume);

        // ResumeVersion 생성 (v1)
        ResumeVersion version = ResumeVersion.create(
                snowflake.nextId(), resumeId, 1, title, content, "최초 작성"
        );
        resumeVersionRepository.save(version);

        log.info("=== 이력서 DataInitializer 완료: {} ===", title);
    }
}
