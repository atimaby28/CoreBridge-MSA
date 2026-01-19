-- CoreBridge Database Initialization Script

-- ==========================================
-- Enum Types
-- ==========================================

-- User Role
CREATE TYPE user_role AS ENUM ('USER', 'COMPANY', 'ADMIN');

-- User Status
CREATE TYPE user_status AS ENUM ('ACTIVE', 'INACTIVE', 'SUSPENDED');

-- Application Status (Legacy)
CREATE TYPE application_status AS ENUM (
    'APPLIED', 'DOCUMENT_PASS', 'DOCUMENT_FAIL',
    'CODING_TEST', 'INTERVIEW_1', 'INTERVIEW_2',
    'FINAL_PASS', 'FINAL_FAIL'
);

-- Process Step (State Machine)
CREATE TYPE process_step AS ENUM (
    'APPLIED', 'DOCUMENT_REVIEW',
    'DOCUMENT_PASS', 'DOCUMENT_FAIL',
    'CODING_TEST', 'CODING_PASS', 'CODING_FAIL',
    'INTERVIEW_1', 'INTERVIEW_1_PASS', 'INTERVIEW_1_FAIL',
    'INTERVIEW_2', 'INTERVIEW_2_PASS', 'INTERVIEW_2_FAIL',
    'FINAL_REVIEW', 'FINAL_PASS', 'FINAL_FAIL'
);

-- Schedule Type
CREATE TYPE schedule_type AS ENUM ('CODING_TEST', 'INTERVIEW_1', 'INTERVIEW_2');

-- Schedule Status
CREATE TYPE schedule_status AS ENUM ('SCHEDULED', 'COMPLETED', 'CANCELLED');

-- Notification Type
CREATE TYPE notification_type AS ENUM (
    'APPLICATION_RECEIVED', 'APPLICATION_VIEWED',
    'PROCESS_UPDATE',
    'DOCUMENT_PASS', 'DOCUMENT_FAIL',
    'CODING_TEST_SCHEDULED', 'INTERVIEW_SCHEDULED', 'INTERVIEW_REMINDER',
    'SCHEDULE_CREATED', 'SCHEDULE_UPDATED', 'SCHEDULE_CANCELLED',
    'FINAL_PASS', 'FINAL_FAIL',
    'JOBPOSTING_DEADLINE', 'JOBPOSTING_CLOSED',
    'COMMENT_REPLY', 'SYSTEM_NOTICE'
);

-- Notification Status
CREATE TYPE notification_status AS ENUM ('UNREAD', 'READ', 'DELETED');

-- ==========================================
-- Sample Data (Optional)
-- ==========================================

-- 테스트 계정은 애플리케이션 시작 시 생성하거나
-- 별도 seed-data.sql로 분리하는 것을 권장합니다.

-- ==========================================
-- Indexes (Performance)
-- ==========================================

-- 자주 사용되는 쿼리에 대한 인덱스는
-- JPA가 자동 생성하거나, 운영 단계에서 추가합니다.
