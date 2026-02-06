#!/bin/bash
# ============================================================
# CoreBridge AI Pipeline - AFTER Load Test (n8n Async)
# Git Bash 호환 | UTF-8 tmpfile 방식 | Swagger 검증 완료
#
# Usage:
#   chmod +x test_after_load.sh
#   ./test_after_load.sh http://localhost:9001 3
#
# Before와 차이점:
#   PART B의 Ollama 엔드포인트를 /async/* 로 교체
#   → 즉시 task_id 반환 (ms 단위) → 폴링으로 결과 확인
# ============================================================

BASE_URL="${1:-http://localhost:9001}"
REPEAT="${2:-3}"
TMPDIR=$(mktemp -d)

echo "=========================================="
echo "  CoreBridge AI Pipeline - AFTER Load Test"
echo "  (n8n Async Pipeline)"
echo "=========================================="
echo "URL:    $BASE_URL"
echo "Repeat: ${REPEAT}x per endpoint"
echo "Start:  $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

# ----------------------------------------------------------
# Health check
# ----------------------------------------------------------
echo "0. Health Check"
echo "------------------------------------------"
HC=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/docs" 2>/dev/null)
if [ "$HC" = "200" ]; then
    echo "  FastAPI Swagger OK"
else
    echo "  HTTP $HC - check server"
fi
echo ""

# ----------------------------------------------------------
# call_api: endpoint, jsonfile, index, show_body(yes/no)
# ----------------------------------------------------------
call_api() {
    local endpoint=$1
    local jsonfile=$2
    local i=$3
    local show_body=$4

    if [ "$show_body" = "yes" ]; then
        local result
        result=$(curl -s -w "\nHTTPCODE:%{http_code} TIME:%{time_total}" \
            -X POST "$BASE_URL$endpoint" \
            -H "Content-Type: application/json; charset=utf-8" \
            -d @"$jsonfile")
        local meta
        meta=$(echo "$result" | grep "HTTPCODE:" | tail -1)
        local body
        body=$(echo "$result" | grep -v "HTTPCODE:")
        local http_code
        http_code=$(echo "$meta" | sed 's/.*HTTPCODE:\([0-9]*\).*/\1/')
        local total_time
        total_time=$(echo "$meta" | sed 's/.*TIME:\([0-9.]*\).*/\1/')
        echo "  [$i/$REPEAT] ${total_time}s (HTTP $http_code)"
        if [ "$http_code" != "200" ]; then
            echo "  >> $body"
        fi
    else
        local result
        result=$(curl -s -o /dev/null -w "HTTPCODE:%{http_code} TIME:%{time_total}" \
            -X POST "$BASE_URL$endpoint" \
            -H "Content-Type: application/json; charset=utf-8" \
            -d @"$jsonfile")
        local http_code
        http_code=$(echo "$result" | sed 's/.*HTTPCODE:\([0-9]*\).*/\1/')
        local total_time
        total_time=$(echo "$result" | sed 's/.*TIME:\([0-9.]*\).*/\1/')
        echo "  [$i/$REPEAT] ${total_time}s (HTTP $http_code)"
    fi
}

# ----------------------------------------------------------
# call_async: endpoint, jsonfile, index
#   → task_id 즉시 반환 시간 측정 (응답 Latency = dispatch 시간)
# ----------------------------------------------------------
call_async() {
    local endpoint=$1
    local jsonfile=$2
    local i=$3

    local result
    result=$(curl -s -w "\nHTTPCODE:%{http_code} TIME:%{time_total}" \
        -X POST "$BASE_URL$endpoint" \
        -H "Content-Type: application/json; charset=utf-8" \
        -d @"$jsonfile")
    local meta
    meta=$(echo "$result" | grep "HTTPCODE:" | tail -1)
    local body
    body=$(echo "$result" | grep -v "HTTPCODE:")
    local http_code
    http_code=$(echo "$meta" | sed 's/.*HTTPCODE:\([0-9]*\).*/\1/')
    local total_time
    total_time=$(echo "$meta" | sed 's/.*TIME:\([0-9.]*\).*/\1/')

    # task_id 추출
    local task_id
    task_id=$(echo "$body" | grep -o '"task_id":"[^"]*"' | head -1 | cut -d'"' -f4)

    echo "  [$i/$REPEAT] ${total_time}s (HTTP $http_code) task=$task_id"

    # 폴링으로 완료 대기 (최대 120초)
    if [ -n "$task_id" ] && [ "$http_code" = "200" ]; then
        local poll_start
        poll_start=$(date +%s)
        local status="processing"
        while [ "$status" = "processing" ]; do
            sleep 3
            local poll_result
            poll_result=$(curl -s "$BASE_URL/result/$task_id" 2>/dev/null)
            status=$(echo "$poll_result" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)
            local elapsed=$(( $(date +%s) - poll_start ))
            if [ "$elapsed" -gt 120 ]; then
                echo "    → TIMEOUT after ${elapsed}s"
                break
            fi
        done
        local poll_end
        poll_end=$(date +%s)
        local poll_elapsed=$(( poll_end - poll_start ))
        echo "    → $status (background: ${poll_elapsed}s)"
    fi
}

# ----------------------------------------------------------
# Generate JSON payloads (UTF-8 tmpfiles)
# ----------------------------------------------------------

for i in $(seq 1 $REPEAT); do
cat > "$TMPDIR/save_resume_$i.json" << ENDJSON
{"candidate_id":"$i","resume_text":"Java Spring Boot JPA MySQL Redis Kafka Docker Kubernetes developer $i years experience. MSA architecture design and implementation."}
ENDJSON
done

for i in $(seq 1 $REPEAT); do
cat > "$TMPDIR/save_jobposting_$i.json" << ENDJSON
{"jobposting_id":"${i}00","jobposting_text":"Backend developer hiring. Spring Boot, JPA required. Kafka, Kubernetes preferred. MSA experience preferred."}
ENDJSON
done

cat > "$TMPDIR/match_candidates.json" << 'ENDJSON'
{"jd_text":"Backend developer. Spring Boot, MySQL required.","top_k":3}
ENDJSON

cat > "$TMPDIR/match_jd.json" << 'ENDJSON'
{"jd_text":"Fullstack developer. React, Node.js required.","top_k":3}
ENDJSON

cat > "$TMPDIR/match_jobpostings.json" << 'ENDJSON'
{"resume_text":"Java Spring Boot 5 years experience. Redis, Kafka specialist.","top_k":5}
ENDJSON

cat > "$TMPDIR/skills.json" << 'ENDJSON'
{"text":"Java, Spring Boot, JPA, MySQL, Redis, Kafka, Docker, Kubernetes, Jenkins, AWS EC2, S3, RDS, Python, FastAPI, React"}
ENDJSON

cat > "$TMPDIR/score.json" << 'ENDJSON'
{"candidate_id":"1","jd_text":"Spring Boot and Docker experience required","required_skills":["Spring Boot","Docker","MySQL"]}
ENDJSON

cat > "$TMPDIR/skill_gap.json" << 'ENDJSON'
{"candidate_id":"1","jobposting_id":"100"}
ENDJSON

cat > "$TMPDIR/summary.json" << 'ENDJSON'
{"text":"Java backend developer with 7 years of experience. Proficient in Spring Boot based MSA architecture design and implementation. Experienced with JPA, QueryDSL for data access layer. Strong background in MySQL, PostgreSQL, Redis for database optimization. Built event-driven async systems using Kafka. Experienced in Docker, Kubernetes container orchestration and Jenkins CI/CD pipeline."}
ENDJSON

# ==========================================================
#  PART A: No Ollama (fast endpoints) — 동기, Before와 동일
# ==========================================================
echo "=========================================="
echo "  PART A: No Ollama (fast) — Sync"
echo "=========================================="
echo ""

echo "1. POST /save_resume"
echo "------------------------------------------"
for i in $(seq 1 $REPEAT); do
    show="no"; [ "$i" = "1" ] && show="yes"
    call_api "/save_resume" "$TMPDIR/save_resume_$i.json" $i $show
done
echo ""

echo "2. POST /save_jobposting"
echo "------------------------------------------"
for i in $(seq 1 $REPEAT); do
    show="no"; [ "$i" = "1" ] && show="yes"
    call_api "/save_jobposting" "$TMPDIR/save_jobposting_$i.json" $i $show
done
echo ""

echo "3. POST /match_candidates"
echo "------------------------------------------"
for i in $(seq 1 $REPEAT); do
    show="no"; [ "$i" = "1" ] && show="yes"
    call_api "/match_candidates" "$TMPDIR/match_candidates.json" $i $show
done
echo ""

echo "4. POST /match_jd [compat]"
echo "------------------------------------------"
for i in $(seq 1 $REPEAT); do
    show="no"; [ "$i" = "1" ] && show="yes"
    call_api "/match_jd" "$TMPDIR/match_jd.json" $i $show
done
echo ""

echo "5. POST /match_jobpostings"
echo "------------------------------------------"
for i in $(seq 1 $REPEAT); do
    show="no"; [ "$i" = "1" ] && show="yes"
    call_api "/match_jobpostings" "$TMPDIR/match_jobpostings.json" $i $show
done
echo ""

# ==========================================================
#  PART B: Async (n8n) — 핵심 비교 대상
# ==========================================================
echo "=========================================="
echo "  PART B: Async via n8n (비동기)"
echo "=========================================="
echo ""
echo "  ※ dispatch 시간 = 클라이언트 응답 Latency"
echo "  ※ background 시간 = n8n→Ollama 실제 처리 시간"
echo ""

echo "6. POST /async/skills"
echo "------------------------------------------"
for i in $(seq 1 $REPEAT); do
    call_async "/async/skills" "$TMPDIR/skills.json" $i
done
echo ""

echo "7. POST /async/score"
echo "------------------------------------------"
for i in $(seq 1 $REPEAT); do
    call_async "/async/score" "$TMPDIR/score.json" $i
done
echo ""

echo "8. POST /async/skill_gap"
echo "------------------------------------------"
for i in $(seq 1 $REPEAT); do
    call_async "/async/skill_gap" "$TMPDIR/skill_gap.json" $i
done
echo ""

echo "9. POST /async/summary"
echo "------------------------------------------"
for i in $(seq 1 $REPEAT); do
    call_async "/async/summary" "$TMPDIR/summary.json" $i
done
echo ""

# ==========================================================
#  Prometheus Metrics
# ==========================================================
echo "=========================================="
echo "  Prometheus Metrics"
echo "=========================================="
sleep 2
curl -s "$BASE_URL/metrics" 2>/dev/null | grep -iE "latency|duration|requests_total|ollama|async" | grep -v "^#" | head -30
echo ""

echo "=========================================="
echo "  Done: $(date '+%Y-%m-%d %H:%M:%S')"
echo "=========================================="
echo ""
echo "  Before/After 비교 포인트:"
echo "  ┌──────────────┬──────────┬──────────┐"
echo "  │ Endpoint     │ Before   │ After    │"
echo "  ├──────────────┼──────────┼──────────┤"
echo "  │ /summary     │ ~30s     │ ~0.0Xs   │"
echo "  │ /skills      │ ~20s     │ ~0.0Xs   │"
echo "  │ /skill_gap   │ ~15-20s  │ ~0.0Xs   │"
echo "  │ /score       │ ~10s     │ ~0.0Xs   │"
echo "  └──────────────┴──────────┴──────────┘"
echo "  ※ After = dispatch time (즉시 응답)"
echo "  ※ 실제 처리는 n8n 백그라운드에서 수행"
echo ""
echo "  Grafana capture checklist:"
echo "    - Time range: Last 5~15 minutes"
echo "    - Panels: QPS, Latency, Endpoint Comparison,"
echo "              Ollama, Embedding, Error"

# Cleanup
rm -rf "$TMPDIR"
