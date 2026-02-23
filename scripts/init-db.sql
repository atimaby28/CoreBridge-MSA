-- ==========================================
-- CoreBridge Database Initialization Script
-- PostgreSQL docker-entrypoint-initdb.d 에서 자동 실행
-- 데이터베이스만 생성 (시드 데이터는 각 서비스 DataInitializer가 담당)
-- ==========================================

CREATE DATABASE "user";
CREATE DATABASE jobposting;
CREATE DATABASE jobposting_comment;
CREATE DATABASE jobposting_view;
CREATE DATABASE jobposting_like;
CREATE DATABASE jobposting_hot;
CREATE DATABASE jobposting_read;
CREATE DATABASE resume;
CREATE DATABASE apply;
CREATE DATABASE notification;
CREATE DATABASE schedule;
CREATE DATABASE admin_audit;
