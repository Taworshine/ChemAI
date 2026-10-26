-- ============================================================
-- 思维赋能系统 v3：知识入库（上传→抽取→确认→发布）+ 高频问题
-- 幂等：可重复执行
-- ============================================================
USE mind_empower;

-- 抽取任务（状态机：CREATED/PARSING/EXTRACTING/MATCHING/PENDING_CONFIRM/CONFIRMED/PUBLISHED/OFFLINE/FAILED/CANCELLED）
CREATE TABLE IF NOT EXISTS extraction_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  teacher_id BIGINT NOT NULL,
  doc_id BIGINT NULL,
  title VARCHAR(255) NOT NULL,
  doc_type VARCHAR(8) NOT NULL DEFAULT 'A',
  status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
  version INT NOT NULL DEFAULT 0,
  doc_text MEDIUMTEXT NULL,
  result_json MEDIUMTEXT NULL,
  fail_reason VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finished_at DATETIME NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 抽取候选条目（教师逐条裁决）
CREATE TABLE IF NOT EXISTS extraction_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  kind VARCHAR(20) NOT NULL,
  payload MEDIUMTEXT NOT NULL,
  match_level VARCHAR(4) NULL,
  match_node_id VARCHAR(64) NULL,
  match_node_label VARCHAR(64) NULL,
  match_node_name VARCHAR(255) NULL,
  similarity DOUBLE NULL,
  decision VARCHAR(10) NOT NULL DEFAULT 'PENDING',
  edited_payload MEDIUMTEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 写入/发布/下线 审计日志
CREATE TABLE IF NOT EXISTS ingest_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  action VARCHAR(20) NOT NULL,
  detail MEDIUMTEXT NULL,
  operator BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 高频问题与官方解答
CREATE TABLE IF NOT EXISTS faq (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  question_text VARCHAR(500) NOT NULL,
  concept_name VARCHAR(255) NULL,
  ask_count INT NOT NULL DEFAULT 1,
  answer MEDIUMTEXT NULL,
  answered_by BIGINT NULL,
  answered_at DATETIME NULL,
  publish_status INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 解答反馈（有用/无用）
CREATE TABLE IF NOT EXISTS faq_feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  faq_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  useful TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_faq_student (faq_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
