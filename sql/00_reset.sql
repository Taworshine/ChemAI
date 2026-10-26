-- 重置：整库删除（init-sql.bat 在导入前执行，保证幂等）
-- 与 seeds/00_reset.cypher 对应：MySQL 侧每次启动回到种子状态
DROP DATABASE IF EXISTS mind_empower;
