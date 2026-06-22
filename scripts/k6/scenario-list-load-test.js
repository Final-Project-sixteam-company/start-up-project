import http from 'k6/http';
import { check, sleep } from 'k6';

// 테스트 옵션: 목표 트래픽 기준 (RPS)
export const options = {
    // 1. Smoke Test (간단한 워밍업)
    // vus: 1,
    // duration: '5s',

    // 2. Load Test (실제 부하 발생)
    stages: [
        { duration: '10s', target: 50 },  // 10초 동안 가상 유저(VU)를 50명까지 증가
        { duration: '30s', target: 50 },  // 30초 동안 50명 유지 (목표 트래픽 지속)
        { duration: '10s', target: 0 },   // 10초 동안 0명으로 감소
    ],
    
    // 목표 임계값 설정 (Thresholds)
    thresholds: {
        // 전체 HTTP 요청 중 95%가 200ms 이내에 완료되어야 함 (P95 Latency)
        http_req_duration: ['p(95)<200'],
        // 전체 HTTP 요청 중 실패(에러)율이 1% 미만이어야 함
        http_req_failed: ['rate<0.01'], 
    },
};

// 메인 테스트 로직 (가상 유저들이 반복해서 실행할 코드)
export default function () {
    // 1. 앱 홈 화면 진입 시나리오 (시나리오 목록 최신순 조회)
    // - 가장 많이 호출되는 API (병목 예상 지점)
    const url = 'http://localhost:8080/api/scenarios?page=0&size=20&sort=createdAt,desc';

    const res = http.get(url);

    // 응답 검증
    check(res, {
        'status is 200': (r) => r.status === 200,
        'has scenarios': (r) => {
            try {
                return JSON.parse(r.body).data.content.length >= 0;
            } catch (e) {
                return false;
            }
        },
    });

    // 유저가 다음 행동을 하기 전 생각하는 시간 (Think Time)
    // sleep(1); // 최대 부하 테스트를 위해 sleep 제거
}
