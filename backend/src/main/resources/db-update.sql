-- 添加 deadline 字段到 tasks 表
-- 如果使用 H2 数据库（内存数据库），重启会自动重建表
-- 如果使用 MySQL/PostgreSQL，需要手动执行此 SQL

ALTER TABLE tasks ADD COLUMN deadline TIMESTAMP NULL;
