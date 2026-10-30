-- Minimal local test data (ASCII only, safe with any console encoding)
USE daijia_customer;
INSERT INTO customer_info (id, wx_open_id, nickname, phone, status)
VALUES (1, 'local_test_customer_openid', 'LocalTestCustomer', '13800000001', 1)
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), status = 1;

USE daijia_driver;
INSERT INTO driver_info (id, wx_open_id, nickname, phone, name, auth_status, status)
VALUES (1, 'local_test_driver_openid', 'LocalTestDriver', '13800000002', 'TestDriver', 2, 1)
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), auth_status = 2, status = 1;

INSERT INTO driver_set (id, driver_id, service_status, order_distance, accept_distance, is_auto_accept)
VALUES (1, 1, 1, 0.00, 50.00, 0)
ON DUPLICATE KEY UPDATE service_status = 1, order_distance = 0.00, accept_distance = 50.00;

-- Rescue trips are long: order_distance=0 (no mileage cap), accept_distance=50km.
-- Demo face check for today so /driver/startService works without Tencent Cloud face API.
INSERT INTO driver_face_recognition (driver_id, face_date) VALUES (1, CURDATE());

INSERT INTO driver_account (id, driver_id)
VALUES (1, 1)
ON DUPLICATE KEY UPDATE driver_id = VALUES(driver_id);
