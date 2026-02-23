#!/bin/bash
# ============================================================
# CoreBridge AI Pipeline - Before/After Load Test
# Git Bash 호환 | UTF-8 tmpfile 방식 | Swagger 검증 완료
#
# Usage:
#   chmod +x test_load.sh
#   ./test_load.sh http://localhost:9001 3
#
# Endpoints (9):
#   Ollama X (fast): /save_resume, /save_jobposting,
#                    /match_candidates, /match_jd, /match_jobpostings
#   Ollama O (slow): /skills, /score, /skill_gap, /summary
# ============================================================

BASE_URL="${1:-http://localhost:9001}"
REPEAT="${2:-3}"
TMPDIR=$(mktemp -d)

echo "=========================================="
echo "  CoreBridge AI Pipeline Load Test"
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
#  PART A: No Ollama (fast endpoints)
# ==========================================================
echo "=========================================="
echo "  PART A: No Ollama (fast)"
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
#  PART B: Ollama (slow endpoints)
# ==========================================================
echo "=========================================="
echo "  PART B: Ollama (slow)"
echo "=========================================="
echo ""

echo "6. POST /skills (Ollama ~10-20s)"
echo "------------------------------------------"
for i in $(seq 1 $REPEAT); do
    show="no"; [ "$i" = "1" ] && show="yes"
    call_api "/skills" "$TMPDIR/skills.json" $i $show
done
echo ""

echo "7. POST /score (Ollama ~10s)"
echo "------------------------------------------"
for i in $(seq 1 $REPEAT); do
    show="no"; [ "$i" = "1" ] && show="yes"
    call_api "/score" "$TMPDIR/score.json" $i $show
done
echo ""

echo "8. POST /skill_gap (Ollama ~15-20s)"
echo "------------------------------------------"
for i in $(seq 1 $REPEAT); do
    show="no"; [ "$i" = "1" ] && show="yes"
    call_api "/skill_gap" "$TMPDIR/skill_gap.json" $i $show
done
echo ""

echo "9. POST /summary (Ollama ~28-35s)"
echo "------------------------------------------"
for i in $(seq 1 $REPEAT); do
    show="no"; [ "$i" = "1" ] && show="yes"
    call_api "/summary" "$TMPDIR/summary.json" $i $show
done
echo ""

# ==========================================================
#  Prometheus Metrics
# ==========================================================
echo "=========================================="
echo "  Prometheus Metrics"
echo "=========================================="
sleep 2
curl -s "$BASE_URL/metrics" 2>/dev/null | grep -iE "latency|duration|requests_total|ollama" | grep -v "^#" | head -30
echo ""

echo "=========================================="
echo "  Done: $(date '+%Y-%m-%d %H:%M:%S')"
echo "=========================================="
echo ""
echo "  Grafana capture checklist:"
echo "    - Time range: Last 5~15 minutes"
echo "    - Panels: QPS, Latency, Endpoint Comparison,"
echo "              Ollama, Embedding, Error"
echo ""
echo "  Ollama O (slow): /summary /skills /score /skill_gap"
echo "  Ollama X (fast): /save_resume /save_jobposting /match_*"

# Cleanup
rm -rf "$TMPDIR"
