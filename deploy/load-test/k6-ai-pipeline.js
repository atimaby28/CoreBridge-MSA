/**
 * ============================================================
 * CoreBridge AI Pipeline - k6 Load Test
 * ============================================================
 *
 * Before vs After (n8n Async) 성능 비교 부하 테스트
 *
 * 사전 준비:
 *   1. k6 설치: https://k6.io/docs/getting-started/installation/
 *      - Windows: choco install k6  또는  winget install k6
 *      - WSL: sudo apt install k6  또는  snap install k6
 *   2. FastAPI + Ollama + Redis + n8n 기동
 *   3. 데이터 시딩: save_resume, save_jobposting 먼저 실행
 *
 * 실행:
 *   # Before 테스트 (동기)
 *   k6 run --env MODE=before k6-ai-pipeline.js
 *
 *   # After 테스트 (비동기)
 *   k6 run --env MODE=after k6-ai-pipeline.js
 *
 *   # 전체 비교 (Before → After 순차)
 *   k6 run k6-ai-pipeline.js
 *
 *   # 결과 JSON 출력
 *   k6 run --out json=result.json k6-ai-pipeline.js
 *
 * 환경변수:
 *   BASE_URL  - FastAPI 서버 URL (기본: http://localhost:9001)
 *   MODE      - before | after | full (기본: full)
 * ============================================================
 */

import http from "k6/http";
import { check, sleep, group } from "k6";
import { Rate, Trend, Counter } from "k6/metrics";

// ============================================================
// Custom Metrics
// ============================================================

// Before (동기) 메트릭
const beforeSummaryDuration = new Trend("before_summary_duration", true);
const beforeSkillsDuration = new Trend("before_skills_duration", true);
const beforeScoreDuration = new Trend("before_score_duration", true);
const beforeSkillGapDuration = new Trend("before_skill_gap_duration", true);

// After (비동기) 메트릭 - dispatch 시간
const afterSummaryDispatch = new Trend("after_summary_dispatch", true);
const afterSkillsDispatch = new Trend("after_skills_dispatch", true);
const afterScoreDispatch = new Trend("after_score_dispatch", true);
const afterSkillGapDispatch = new Trend("after_skill_gap_dispatch", true);

// 공통 메트릭
const saveResumeDuration = new Trend("save_resume_duration", true);
const saveJobpostingDuration = new Trend("save_jobposting_duration", true);
const matchCandidatesDuration = new Trend("match_candidates_duration", true);
const matchJobpostingsDuration = new Trend("match_jobpostings_duration", true);

// 에러율
const errorRate = new Rate("error_rate");
const asyncPollingSuccess = new Rate("async_polling_success");

// ============================================================
// Configuration
// ============================================================

const BASE_URL = __ENV.BASE_URL || "http://localhost:9001";
const MODE = __ENV.MODE || "full"; // before | after | full

export const options = {
  scenarios: {
    // Scenario 1: 데이터 시딩 (VU 1명, 1회)
    seed_data: {
      executor: "shared-iterations",
      vus: 1,
      iterations: 1,
      exec: "seedData",
      startTime: "0s",
      maxDuration: "60s",
    },

    // Scenario 2: Before 테스트 (동기 Ollama 호출)
    ...(MODE !== "after" && {
      before_test: {
        executor: "per-vu-iterations",
        vus: 3,
        iterations: 2,
        exec: "beforeTest",
        startTime: "10s",  // 시딩 완료 후
        maxDuration: "300s", // Ollama 느리므로 여유 있게
      },
    }),

    // Scenario 3: After 테스트 (n8n 비동기)
    ...(MODE !== "before" && {
      after_test: {
        executor: "per-vu-iterations",
        vus: 3,
        iterations: 2,
        exec: "afterTest",
        startTime: MODE === "before" ? "10s" : "180s", // Before 후 실행
        maxDuration: "120s",
      },
    }),

    // Scenario 4: 비 Ollama 엔드포인트 부하 (동시성 테스트)
    fast_endpoints: {
      executor: "ramping-vus",
      exec: "fastEndpointsTest",
      startTime: MODE === "full" ? "300s" : "10s",
      stages: [
        { duration: "10s", target: 5 },
        { duration: "20s", target: 10 },
        { duration: "10s", target: 20 },
        { duration: "10s", target: 5 },
        { duration: "5s", target: 0 },
      ],
    },
  },

  thresholds: {
    // 비동기 dispatch는 1초 이내
    after_summary_dispatch: ["p(95)<1000"],
    after_skills_dispatch: ["p(95)<1000"],
    // 임베딩 기반 엔드포인트는 5초 이내
    save_resume_duration: ["p(95)<5000"],
    match_candidates_duration: ["p(95)<5000"],
    // 에러율 10% 이하
    error_rate: ["rate<0.1"],
  },
};

// ============================================================
// Test Data
// ============================================================

const RESUMES = [
  {
    candidate_id: "k6-1",
    resume_text:
      "Java Spring Boot JPA MySQL Redis Kafka Docker Kubernetes developer 5 years experience. MSA architecture design and implementation. CI/CD pipeline with Jenkins.",
    skills: ["Java", "Spring Boot", "JPA", "MySQL", "Redis", "Kafka"],
  },
  {
    candidate_id: "k6-2",
    resume_text:
      "Python FastAPI Django PostgreSQL developer 3 years. Machine learning with TensorFlow, data pipeline with Airflow. AWS Lambda serverless experience.",
    skills: ["Python", "FastAPI", "Django", "PostgreSQL", "TensorFlow"],
  },
  {
    candidate_id: "k6-3",
    resume_text:
      "Fullstack developer React TypeScript Node.js Express MongoDB. 4 years experience in startup environment. GraphQL API design and real-time WebSocket systems.",
    skills: ["React", "TypeScript", "Node.js", "MongoDB", "GraphQL"],
  },
];

const JOBPOSTINGS = [
  {
    jobposting_id: "k6-100",
    jobposting_text:
      "Backend developer hiring. Spring Boot, JPA required. Kafka, Kubernetes preferred. MSA experience preferred. 3+ years experience.",
  },
  {
    jobposting_id: "k6-200",
    jobposting_text:
      "AI/ML Engineer position. Python, TensorFlow or PyTorch required. FastAPI for model serving. AWS experience preferred.",
  },
  {
    jobposting_id: "k6-300",
    jobposting_text:
      "Fullstack developer. React, TypeScript frontend required. Node.js backend. MongoDB or PostgreSQL. CI/CD experience.",
  },
];

const SUMMARY_TEXT =
  "Java backend developer with 7 years of experience. Proficient in Spring Boot based MSA architecture design and implementation. Experienced with JPA, QueryDSL for data access layer. Strong background in MySQL, PostgreSQL, Redis for database optimization. Built event-driven async systems using Kafka. Experienced in Docker, Kubernetes container orchestration and Jenkins CI/CD pipeline.";

const SKILLS_TEXT =
  "Java, Spring Boot, JPA, MySQL, Redis, Kafka, Docker, Kubernetes, Jenkins, AWS EC2, S3, RDS, Python, FastAPI, React";

const headers = { "Content-Type": "application/json" };

// ============================================================
// Helper Functions
// ============================================================

function postJson(endpoint, body) {
  const res = http.post(`${BASE_URL}${endpoint}`, JSON.stringify(body), {
    headers,
  });
  const success = res.status === 200;
  errorRate.add(!success);
  return res;
}

/**
 * 비동기 작업 폴링 (결과 대기)
 * - 포트폴리오 수치용: dispatch time만 핵심, polling은 보조
 */
function pollResult(taskId, maxWaitSec) {
  maxWaitSec = maxWaitSec || 120;
  const startTime = Date.now();

  while (true) {
    sleep(3);
    const elapsed = (Date.now() - startTime) / 1000;
    if (elapsed > maxWaitSec) {
      asyncPollingSuccess.add(false);
      return null;
    }

    const res = http.get(`${BASE_URL}/result/${taskId}`);
    if (res.status === 200) {
      try {
        const data = JSON.parse(res.body);
        if (data.status === "completed") {
          asyncPollingSuccess.add(true);
          return data;
        }
      } catch (e) {
        // JSON parse error, continue polling
      }
    }
  }
}

// ============================================================
// Scenario 1: Data Seeding
// ============================================================

export function seedData() {
  group("Seed: Save Resumes", function () {
    for (const resume of RESUMES) {
      const res = postJson("/save_resume", resume);
      check(res, {
        "resume saved": (r) => r.status === 200,
      });
    }
  });

  group("Seed: Save Jobpostings", function () {
    for (const jp of JOBPOSTINGS) {
      const res = postJson("/save_jobposting", jp);
      check(res, {
        "jobposting saved": (r) => r.status === 200,
      });
    }
  });

  sleep(2);
  console.log("✅ Data seeding complete");
}

// ============================================================
// Scenario 2: Before Test (동기 - Ollama 직접 호출)
// ============================================================

export function beforeTest() {
  group("Before: /summary (sync)", function () {
    const res = postJson("/summary", { text: SUMMARY_TEXT });
    beforeSummaryDuration.add(res.timings.duration);
    check(res, {
      "summary 200": (r) => r.status === 200,
      "summary has content": (r) => {
        try {
          return JSON.parse(r.body).summary.length > 0;
        } catch {
          return false;
        }
      },
    });
  });

  group("Before: /skills (sync)", function () {
    const res = postJson("/skills", { text: SKILLS_TEXT });
    beforeSkillsDuration.add(res.timings.duration);
    check(res, {
      "skills 200": (r) => r.status === 200,
      "skills extracted": (r) => {
        try {
          return JSON.parse(r.body).skills.length > 0;
        } catch {
          return false;
        }
      },
    });
  });

  group("Before: /score (sync)", function () {
    const res = postJson("/score", {
      candidate_id: "k6-1",
      jd_text: "Spring Boot and Docker experience required",
      required_skills: ["Spring Boot", "Docker", "MySQL"],
    });
    beforeScoreDuration.add(res.timings.duration);
    check(res, {
      "score 200": (r) => r.status === 200,
    });
  });

  group("Before: /skill_gap (sync)", function () {
    const res = postJson("/skill_gap", {
      candidate_id: "k6-1",
      jobposting_id: "k6-100",
    });
    beforeSkillGapDuration.add(res.timings.duration);
    check(res, {
      "skill_gap 200": (r) => r.status === 200,
    });
  });

  sleep(1);
}

// ============================================================
// Scenario 3: After Test (비동기 - n8n dispatch)
// ============================================================

export function afterTest() {
  group("After: /async/summary", function () {
    const res = postJson("/async/summary", { text: SUMMARY_TEXT });
    afterSummaryDispatch.add(res.timings.duration);
    check(res, {
      "async summary 200": (r) => r.status === 200,
      "has task_id": (r) => {
        try {
          return JSON.parse(r.body).task_id.length > 0;
        } catch {
          return false;
        }
      },
    });

    // 선택적 폴링 (백그라운드 처리 시간 측정용)
    if (res.status === 200) {
      try {
        const taskId = JSON.parse(res.body).task_id;
        pollResult(taskId, 120);
      } catch (e) {
        // polling failure is non-critical
      }
    }
  });

  group("After: /async/skills", function () {
    const res = postJson("/async/skills", { text: SKILLS_TEXT });
    afterSkillsDispatch.add(res.timings.duration);
    check(res, {
      "async skills 200": (r) => r.status === 200,
      "has task_id": (r) => {
        try {
          return JSON.parse(r.body).task_id.length > 0;
        } catch {
          return false;
        }
      },
    });

    if (res.status === 200) {
      try {
        const taskId = JSON.parse(res.body).task_id;
        pollResult(taskId, 120);
      } catch (e) {}
    }
  });

  group("After: /async/score", function () {
    const res = postJson("/async/score", {
      candidate_id: "k6-1",
      jd_text: "Spring Boot and Docker experience required",
      required_skills: ["Spring Boot", "Docker", "MySQL"],
    });
    afterScoreDispatch.add(res.timings.duration);
    check(res, {
      "async score 200": (r) => r.status === 200,
    });

    if (res.status === 200) {
      try {
        const taskId = JSON.parse(res.body).task_id;
        pollResult(taskId, 120);
      } catch (e) {}
    }
  });

  group("After: /async/skill_gap", function () {
    const res = postJson("/async/skill_gap", {
      candidate_id: "k6-1",
      jobposting_id: "k6-100",
    });
    afterSkillGapDispatch.add(res.timings.duration);
    check(res, {
      "async skill_gap 200": (r) => r.status === 200,
    });

    if (res.status === 200) {
      try {
        const taskId = JSON.parse(res.body).task_id;
        pollResult(taskId, 120);
      } catch (e) {}
    }
  });

  sleep(1);
}

// ============================================================
// Scenario 4: Fast Endpoints (비 Ollama, 동시성 테스트)
// ============================================================

export function fastEndpointsTest() {
  const vu = __VU;
  const iter = __ITER;

  group("Fast: /save_resume", function () {
    const res = postJson("/save_resume", {
      candidate_id: `k6-load-${vu}-${iter}`,
      resume_text: `Load test resume VU${vu} iter${iter}. Java Spring Boot MSA Docker Kubernetes Redis Kafka experience.`,
      skills: ["Java", "Spring Boot", "Docker"],
    });
    saveResumeDuration.add(res.timings.duration);
    check(res, { "save_resume 200": (r) => r.status === 200 });
  });

  group("Fast: /save_jobposting", function () {
    const res = postJson("/save_jobposting", {
      jobposting_id: `k6-jp-${vu}-${iter}`,
      jobposting_text: `Load test jobposting VU${vu} iter${iter}. Backend developer hiring Spring Boot JPA required.`,
    });
    saveJobpostingDuration.add(res.timings.duration);
    check(res, { "save_jobposting 200": (r) => r.status === 200 });
  });

  group("Fast: /match_candidates", function () {
    const res = postJson("/match_candidates", {
      jd_text: "Backend developer. Spring Boot, MySQL required.",
      top_k: 3,
    });
    matchCandidatesDuration.add(res.timings.duration);
    check(res, { "match_candidates 200": (r) => r.status === 200 });
  });

  group("Fast: /match_jobpostings", function () {
    const res = postJson("/match_jobpostings", {
      resume_text: "Java Spring Boot 5 years experience. Redis, Kafka specialist.",
      top_k: 5,
    });
    matchJobpostingsDuration.add(res.timings.duration);
    check(res, { "match_jobpostings 200": (r) => r.status === 200 });
  });

  sleep(0.5);
}

// ============================================================
// Summary 핸들링 (k6 기본 출력에 추가 정보)
// ============================================================

export function handleSummary(data) {
  // 콘솔에 Before/After 비교 테이블 출력
  const getP95 = (metricName) => {
    const m = data.metrics[metricName];
    if (m && m.values && m.values["p(95)"]) {
      return m.values["p(95)"].toFixed(1);
    }
    return "N/A";
  };

  const getAvg = (metricName) => {
    const m = data.metrics[metricName];
    if (m && m.values && m.values.avg) {
      return m.values.avg.toFixed(1);
    }
    return "N/A";
  };

  let summary = `
╔══════════════════════════════════════════════════════════════╗
║        CoreBridge AI Pipeline - Load Test Results           ║
╠══════════════════════════════════════════════════════════════╣
║                                                              ║
║  Before (동기 Ollama) vs After (n8n 비동기) 비교             ║
║                                                              ║
╠══════════════════════════════════════════════════════════════╣
║  Endpoint       │  Before (P95)   │  After (P95)   │ 개선률  ║
╠══════════════════════════════════════════════════════════════╣
║  /summary       │ ${getP95("before_summary_duration").padStart(10)}ms │ ${getP95("after_summary_dispatch").padStart(10)}ms │        ║
║  /skills        │ ${getP95("before_skills_duration").padStart(10)}ms │ ${getP95("after_skills_dispatch").padStart(10)}ms │        ║
║  /score         │ ${getP95("before_score_duration").padStart(10)}ms │ ${getP95("after_score_dispatch").padStart(10)}ms │        ║
║  /skill_gap     │ ${getP95("before_skill_gap_duration").padStart(10)}ms │ ${getP95("after_skill_gap_dispatch").padStart(10)}ms │        ║
╠══════════════════════════════════════════════════════════════╣
║                                                              ║
║  Fast Endpoints (Avg)                                        ║
║  /save_resume       │ ${getAvg("save_resume_duration").padStart(10)}ms                         ║
║  /save_jobposting   │ ${getAvg("save_jobposting_duration").padStart(10)}ms                         ║
║  /match_candidates  │ ${getAvg("match_candidates_duration").padStart(10)}ms                         ║
║  /match_jobpostings │ ${getAvg("match_jobpostings_duration").padStart(10)}ms                         ║
╚══════════════════════════════════════════════════════════════╝
`;

  console.log(summary);

  // JSON 결과도 파일로 저장
  return {
    stdout: summary,
    "result-summary.json": JSON.stringify(
      {
        timestamp: new Date().toISOString(),
        before: {
          summary_p95: getP95("before_summary_duration"),
          skills_p95: getP95("before_skills_duration"),
          score_p95: getP95("before_score_duration"),
          skill_gap_p95: getP95("before_skill_gap_duration"),
          summary_avg: getAvg("before_summary_duration"),
          skills_avg: getAvg("before_skills_duration"),
          score_avg: getAvg("before_score_duration"),
          skill_gap_avg: getAvg("before_skill_gap_duration"),
        },
        after: {
          summary_dispatch_p95: getP95("after_summary_dispatch"),
          skills_dispatch_p95: getP95("after_skills_dispatch"),
          score_dispatch_p95: getP95("after_score_dispatch"),
          skill_gap_dispatch_p95: getP95("after_skill_gap_dispatch"),
          summary_dispatch_avg: getAvg("after_summary_dispatch"),
          skills_dispatch_avg: getAvg("after_skills_dispatch"),
          score_dispatch_avg: getAvg("after_score_dispatch"),
          skill_gap_dispatch_avg: getAvg("after_skill_gap_dispatch"),
        },
        fast_endpoints: {
          save_resume_avg: getAvg("save_resume_duration"),
          save_jobposting_avg: getAvg("save_jobposting_duration"),
          match_candidates_avg: getAvg("match_candidates_duration"),
          match_jobpostings_avg: getAvg("match_jobpostings_duration"),
        },
      },
      null,
      2
    ),
  };
}
