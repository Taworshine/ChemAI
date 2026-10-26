-- P4 增强：任务回滚需要精确知道"本次任务新建了哪些节点"，避免误删种子节点或被合并的已有节点。
-- created_node_ids 保存提交入库时新建节点的 Neo4j elementId 列表（JSON 数组）。
-- MySQL 8.0 的 ALTER 不支持 IF NOT EXISTS，用 information_schema 判断保证幂等，可重复执行。

SET @col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'mind_empower' AND TABLE_NAME = 'extraction_task' AND COLUMN_NAME = 'created_node_ids'
);
SET @sql := IF(@col = 0,
    'ALTER TABLE mind_empower.extraction_task ADD COLUMN created_node_ids MEDIUMTEXT NULL COMMENT ''本次任务新建节点的 Neo4j elementId 列表（JSON）''',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
