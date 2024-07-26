ALTER TABLE captured_images ADD COLUMN name VARCHAR(255);
DELETE FROM captured_images WHERE id = 2;
drop TABLE captured_images;
CREATE TABLE captured_images (
    id INT AUTO_INCREMENT PRIMARY KEY,
    image_data LONGBLOB NOT NULL,
    capture_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE captured_images ADD COLUMN name VARCHAR(255);

DELETE FROM captured_images;

CREATE DATABASE IF NOT EXISTS face_detection_db;

USE face_detection_db;

CREATE TABLE IF NOT EXISTS captured_images (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    image_data LONGBLOB
);


