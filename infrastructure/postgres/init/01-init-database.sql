
-- Link Service
CREATE USER link_user WITH PASSWORD 'link_dev_password';
CREATE DATABASE link_db OWNER link_user;

-- Extraction Service
CREATE USER extraction_user WITH PASSWORD 'extraction_dev_password';
CREATE DATABASE extraction_db OWNER extraction_user;

-- Summarization Service
CREATE USER summary_user WITH PASSWORD 'summary_dev_password';
CREATE DATABASE summary_db OWNER summary_user;

-- User Service
CREATE USER user_user WITH PASSWORD 'user_dev_password';
CREATE DATABASE user_db OWNER user_user;
