-- 司机责任险保单字段，可重复执行：列已存在则跳过
-- policy_photo_url 用 MEDIUMTEXT：演示版存压缩后的 dataURL（约 40-100KB base64），VARCHAR 装不下

SET @db = DATABASE();

SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE driver_info ADD COLUMN policy_no varchar(50) DEFAULT NULL COMMENT ''责任险保单号'' AFTER face_model_id',
  'SELECT 1')
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'driver_info' AND COLUMN_NAME = 'policy_no');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE driver_info ADD COLUMN policy_photo_url mediumtext DEFAULT NULL COMMENT ''责任险保单照片，演示版为压缩后的dataURL（约40-100KB），线上为对象存储URL，故用MEDIUMTEXT'' AFTER policy_no',
  'SELECT 1')
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'driver_info' AND COLUMN_NAME = 'policy_photo_url');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE driver_info ADD COLUMN policy_expire_date date DEFAULT NULL COMMENT ''责任险保单到期日'' AFTER policy_photo_url',
  'SELECT 1')
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'driver_info' AND COLUMN_NAME = 'policy_expire_date');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
