import http from 'k6/http';
import { check, sleep } from 'k6';

// =============================================
// CoreBridge CQRS 캐시 + Circuit Breaker 검증
//
// 정상: k6 run -e SCENARIO=정상_view_like_comment_UP k6-cache-test.js
// 장애: k6 run -e SCENARIO=장애_view_like_comment_DOWN k6-cache-test.js
// =============================================

const scenario = __ENV.SCENARIO || '미지정';

export const options = {
  vus: 10,
  duration: '10s',
  thresholds: {
    http_req_duration: ['p(95)<500'],
  },
};

const BASE_URL = 'http://localhost:8007';
const JOBPOSTING_ID = '16797438048858112';

export function setup() {
  console.log(`\n========================================`);
  console.log(`  테스트 시나리오: ${scenario}`);
  console.log(`  VU: 10 | Duration: 10s`);
  console.log(`  Target: ${BASE_URL}/api/v1/jobposting-read/${JOBPOSTING_ID}`);
  console.log(`========================================\n`);
}

export default function () {
  const res = http.get(`${BASE_URL}/api/v1/jobposting-read/${JOBPOSTING_ID}`);

  check(res, {
    'status 200': (r) => r.status === 200,
  });

  sleep(0.1);
}
