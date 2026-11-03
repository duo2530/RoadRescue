-- 司机手机号登录：wx_open_id 加唯一索引，防止同一手机号并发注册出两条司机。可重复执行：索引已存在则跳过
-- 手机号注册的司机 wx_open_id 写 phone_<手机号>，与微信 openId 共用这一列
-- 执行前先跑下面的查询确认没有重复值；有重复时 ALTER 会以 Duplicate entry 报错且不加索引，需先合并重复司机
-- SELECT wx_open_id, COUNT(*) FROM driver_info GROUP BY wx_open_id HAVING COUNT(*) > 1;

SET @db = DATABASE();

SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE driver_info ADD UNIQUE KEY uni_open_id (wx_open_id) USING BTREE',
  'SELECT 1')
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'driver_info' AND INDEX_NAME = 'uni_open_id');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
