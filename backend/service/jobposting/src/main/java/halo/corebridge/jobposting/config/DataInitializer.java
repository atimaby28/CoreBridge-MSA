package halo.corebridge.jobposting.config;

import halo.corebridge.common.snowflake.Snowflake;
import halo.corebridge.jobposting.model.entity.Jobposting;
import halo.corebridge.jobposting.repository.JobpostingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 애플리케이션 시작 시 샘플 채용공고 5개를 생성하는 초기화 클래스.
 * - PostgreSQL 저장 + FastAPI /save_jobposting (Redis Vector DB 등록)
 * - AI 서비스 미실행 시에도 PostgreSQL 저장은 정상 동작
 */
@Slf4j
@Component
public class DataInitializer implements ApplicationRunner {

    private final JobpostingRepository jobpostingRepository;
    private final RestTemplate restTemplate;
    private final Snowflake snowflake;

    @Value("${ai.service.url:http://localhost:9001}")
    private String aiServiceUrl;

    public DataInitializer(JobpostingRepository jobpostingRepository,
                           RestTemplate restTemplate) {
        this.jobpostingRepository = jobpostingRepository;
        this.restTemplate = restTemplate;
        this.snowflake = new Snowflake();
    }

    // User 서비스 DataInitializer의 COMPANY_ID와 동일 (Snowflake 범위)
    private static final Long COMPANY_USER_ID = 11234023833772034L;
    private static final Long BOARD_ID = 1L;

    @Override
    public void run(ApplicationArguments args) {
        if (jobpostingRepository.count() > 0) {
            log.info("채용공고 데이터가 이미 존재합니다. 초기화 스킵.");
            return;
        }

        createJobposting(
                "백엔드 개발자 (Java/Spring Boot)",
                "[담당업무]\n" +
                "- 대규모 트래픽 처리를 위한 백엔드 API 설계 및 개발\n" +
                "- MSA 기반 서비스 아키텍처 설계\n" +
                "- 데이터베이스 모델링 및 최적화\n\n" +
                "[자격요건]\n" +
                "- Java/Spring Boot 경험 3년 이상\n" +
                "- RDBMS 및 NoSQL 활용 경험\n" +
                "- RESTful API 설계 경험\n\n" +
                "[우대사항]\n" +
                "- Kubernetes/Docker 운영 경험\n" +
                "- Kafka, Redis 활용 경험\n" +
                "- MSA 전환 프로젝트 경험",
                "[\"Java\", \"Spring Boot\", \"PostgreSQL\"]",
                "[\"Kubernetes\", \"Docker\", \"Kafka\"]"
        );

        createJobposting(
                "프론트엔드 개발자 (Vue.js)",
                "[담당업무]\n" +
                "- Vue.js 기반 웹 애플리케이션 개발\n" +
                "- 컴포넌트 설계 및 상태 관리\n" +
                "- UI/UX 개선 및 성능 최적화\n\n" +
                "[자격요건]\n" +
                "- Vue.js 또는 React 경험 2년 이상\n" +
                "- TypeScript 활용 경험\n" +
                "- RESTful API 연동 경험\n\n" +
                "[우대사항]\n" +
                "- Pinia/Vuex 상태관리 경험\n" +
                "- Vite 빌드 시스템 경험\n" +
                "- 웹 접근성 및 반응형 디자인 경험",
                "[\"Vue.js\", \"TypeScript\", \"JavaScript\"]",
                "[\"React\", \"Node.js\", \"Vite\"]"
        );

        createJobposting(
                "DevOps 엔지니어",
                "[담당업무]\n" +
                "- Kubernetes 클러스터 운영 및 관리\n" +
                "- CI/CD 파이프라인 구축 및 최적화\n" +
                "- 모니터링 시스템 구축 (Prometheus, Grafana)\n\n" +
                "[자격요건]\n" +
                "- DevOps/SRE 경험 2년 이상\n" +
                "- Kubernetes 운영 경험\n" +
                "- AWS/GCP 클라우드 경험\n\n" +
                "[우대사항]\n" +
                "- Helm Chart 작성 경험\n" +
                "- Terraform/Ansible 경험\n" +
                "- 장애 대응 및 포스트모템 경험",
                "[\"Kubernetes\", \"Docker\", \"Jenkins\"]",
                "[\"Terraform\", \"Prometheus\", \"AWS\"]"
        );

        createJobposting(
                "AI/ML 엔지니어",
                "[담당업무]\n" +
                "- LLM 기반 AI 서비스 개발\n" +
                "- AI 모델 서빙 파이프라인 구축\n" +
                "- FastAPI 기반 AI API 서버 개발 및 운영\n\n" +
                "[자격요건]\n" +
                "- Python 경험 3년 이상\n" +
                "- PyTorch/TensorFlow 활용 경험\n" +
                "- LLM Fine-tuning 경험\n\n" +
                "[우대사항]\n" +
                "- Ollama, vLLM 등 로컬 LLM 운영 경험\n" +
                "- RAG 시스템 구축 경험\n" +
                "- MLOps 파이프라인 경험",
                "[\"Python\", \"PyTorch\", \"FastAPI\"]",
                "[\"Docker\", \"Kubernetes\", \"LangChain\"]"
        );

        createJobposting(
                "풀스택 개발자 (ERP 현대화)",
                "[담당업무]\n" +
                "- 사내 ERP 시스템 현대화 프로젝트\n" +
                "- Vue.js 프론트엔드 + Spring Boot 백엔드 개발\n" +
                "- 레거시 시스템 MSA 전환\n\n" +
                "[자격요건]\n" +
                "- Java/Spring Boot 경험 2년 이상\n" +
                "- Vue.js 또는 React 경험\n" +
                "- RDBMS 설계 및 쿼리 최적화\n\n" +
                "[우대사항]\n" +
                "- ERP/그룹웨어 개발 경험\n" +
                "- CI/CD 파이프라인 구축 경험\n" +
                "- Agile/Scrum 방법론 경험",
                "[\"Java\", \"Spring Boot\", \"Vue.js\"]",
                "[\"TypeScript\", \"Docker\", \"Jenkins\"]"
        );

        log.info("=== 채용공고 DataInitializer 완료: 5개 생성 ===");
    }

    private void createJobposting(String title, String content,
                                   String requiredSkills, String preferredSkills) {
        Jobposting jobposting = Jobposting.create(
                snowflake.nextId(), title, content, BOARD_ID, COMPANY_USER_ID,
                requiredSkills, preferredSkills
        );
        jobpostingRepository.save(jobposting);
        log.info("채용공고 생성: {}", title);

        // FastAPI /save_jobposting 호출 → Redis Vector DB에 임베딩 등록
        registerToVectorDb(jobposting.getJobpostingId(),
                title + "\n" + content + "\n필수 스킬: " + requiredSkills +
                        "\n우대 스킬: " + preferredSkills);
    }

    /**
     * FastAPI /save_jobposting 호출하여 Redis Vector DB에 채용공고 임베딩 등록.
     * AI 서비스가 실행 중이 아니면 경고 로그만 남기고 스킵.
     */
    private void registerToVectorDb(Long jobpostingId, String jobpostingText) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("jobposting_id", String.valueOf(jobpostingId));
            body.put("jobposting_text", jobpostingText);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            String url = aiServiceUrl + "/save_jobposting";
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            log.info("채용공고 Vector DB 등록 완료: jobpostingId={}", jobpostingId);

        } catch (Exception e) {
            log.warn("채용공고 Vector DB 등록 실패 (AI 서비스 미실행?): {}", e.getMessage());
        }
    }
}
