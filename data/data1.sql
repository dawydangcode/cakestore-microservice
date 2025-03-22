-- --------------------------------------------------------
-- Host:                         127.0.0.1
-- Server version:               5.7.44-log - MySQL Community Server (GPL)
-- Server OS:                    Win64
-- HeidiSQL Version:             12.8.0.6908
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


-- Dumping database structure for users
CREATE DATABASE IF NOT EXISTS `users` /*!40100 DEFAULT CHARACTER SET latin1 */;
USE `users`;

-- Dumping structure for table users.permissions
CREATE TABLE IF NOT EXISTS `permissions` (
  `created_at` datetime(6) DEFAULT NULL,
  `created_by` bigint(20) DEFAULT NULL,
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by` bigint(20) DEFAULT NULL,
  `code` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table users.permissions: ~0 rows (approximately)

-- Dumping structure for table users.roles
CREATE TABLE IF NOT EXISTS `roles` (
  `created_at` datetime(6) DEFAULT NULL,
  `created_by` bigint(20) DEFAULT NULL,
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by` bigint(20) DEFAULT NULL,
  `code` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table users.roles: ~0 rows (approximately)

-- Dumping structure for table users.role_permissions
CREATE TABLE IF NOT EXISTS `role_permissions` (
  `permission_id` bigint(20) NOT NULL,
  `role_id` bigint(20) NOT NULL,
  PRIMARY KEY (`permission_id`,`role_id`),
  KEY `FKn5fotdgk8d1xvo8nav9uv3muc` (`role_id`),
  CONSTRAINT `FKegdk29eiy7mdtefy5c7eirr6e` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`),
  CONSTRAINT `FKn5fotdgk8d1xvo8nav9uv3muc` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table users.role_permissions: ~0 rows (approximately)

-- Dumping structure for table users.tokens
CREATE TABLE IF NOT EXISTS `tokens` (
  `revoked` bit(1) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `created_by` bigint(20) DEFAULT NULL,
  `expiry_date` datetime(6) NOT NULL,
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by` bigint(20) DEFAULT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  `token` varchar(10000) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK2dylsfo39lgjyqml2tbe0b0ss` (`user_id`),
  CONSTRAINT `FK2dylsfo39lgjyqml2tbe0b0ss` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=latin1;

-- Dumping data for table users.tokens: ~4 rows (approximately)
INSERT INTO `tokens` (`revoked`, `created_at`, `created_by`, `expiry_date`, `id`, `updated_at`, `updated_by`, `user_id`, `token`) VALUES
	(b'0', NULL, NULL, '2025-03-18 14:50:55.048923', 1, NULL, NULL, 3, 'eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJpdWguZml0LnNlIiwic3ViIjoiZ2lhaHV5MyIsImV4cCI6MTc0MjMwOTQ1NCwiaWF0IjoxNzQyMzA4ODU0LCJzY29wZSI6W119.lm0XGQq54pLuEJFnNz_fdBi0Jg18_F9Ab3dD8lUo-TO3_FTTDP9xqid1sGbT2eL-QV1pPejqKrmpFPYjLQP-Qmyx5dTSjDtRzZNxj-4snYRFP4tLj55fYntnH7ajv5Ahz_m5nW1DA4hNf5ALa5sdceXFBflq17eUGpBZkeDi5xWXRiWcvuhH9r6nsDzkRP-ARkLRSWzCkjsyFQpT74qIGCNt-fntASGmaE6FMubzc2eC71k2Cq143w1mzEpM-X4kmy_ZiqBqXwKXr0wJBNerdGf0i0EUoIE_HdXZzJHMWa6UfvjY7mMRGHPjF2EmLZ5worG7ybM-5JfJZ8fw1s2rmw'),
	(b'0', NULL, NULL, '2025-03-22 04:04:04.653181', 2, NULL, NULL, 4, 'eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJpdWguZml0LnNlIiwic3ViIjoiZ2lhaHV5NCIsImV4cCI6MTc0MjYxNjI0NCwiaWF0IjoxNzQyNjE1NjQ0LCJzY29wZSI6W119.TjQ8ez8xPhaOdv1YNErvOlqWUN2oDO7uFn2jKZR5Jx17WTE0ue23Xsy-cK6qKgR7W834f61xeCYRvNLH1QRavTqbqE2hgrsjICsscn3phf7t8M3c76ogvcpK005EBeV5zA5_R0UiX_5YQjTjUjDbPGKYrjS9rJvk5W8CNXMx0Pkibbrg3zobgRGlvJSkAogt10X-WrBxPVQS7t3ccV3Bz5KwSDcrMkHR6UFQXfyex8uk47nIhfaNtTK611ohbuJsobe6xF9I2fIw0FULHdMxQyKTksx--9cUapJwn1x1VZsR5A3RtIlGywWevu_k0997fkRy2d9Kx0w5UaAvXW_vPA'),
	(b'0', NULL, NULL, '2025-03-22 04:21:25.524643', 3, NULL, NULL, 3, 'eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJpdWguZml0LnNlIiwic3ViIjoiZ2lhaHV5MyIsImV4cCI6MTc0MjYxNzI4NSwiaWF0IjoxNzQyNjE2Njg1LCJzY29wZSI6W119.ZsEcWAXN3q_SaMrD473TAR39uaVom7YSaB9BglndW6zeYpT6eUzhF6blFTZpc5rBWJVew1rWHekqp0jgKnj5hVGq-UaNx_mfXGFQxXYmXWbAaWsY2hgcMRIDn--HxV2t5yplF5pwvzyc4qwJEQ7WIbBLm-HR3-F9_71_DIqvSGMz2Fyr_GytwL2IfzhiRT8v4N_0-1YjS86xUcCpmfi_k-C7rBPQCEk9yURPSNFOD4J4XGJyJ3PHEmGcxFIwPYn7e2pcGaHzCo2m92l-2nMkleRi3p18jnsXJujGo1bQpozJpB_08bA1dAnuR7OLfIDMyRW7v3ppdXSTHHBIaYy9Nw'),
	(b'0', NULL, NULL, '2025-03-22 10:12:29.863381', 4, NULL, NULL, 1, 'eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJpdWguZml0LnNlIiwic3ViIjoiZ2lhaHV5MSIsImV4cCI6MTc0MjYzODM0OSwiaWF0IjoxNzQyNjM3NzQ5LCJzY29wZSI6W119.hpCpe5ZF5st3l4aX9ID2apY0PCEqm2Wk3ANQjxggmfempEVBfx7mcHqQDOlBtsJfQ2DmFvf8CkzLZC17iSA4pm5oBnsEhvpvKj9UYTb9p1kSo3TRFDmOkpGSNnTNhf8eLyuouviNomNi305-hidmSMcH6asRcr7JlYqPTmczxdIc_NqZx6or6x2H65cy9DYbJQp3DEsW4-JRRko_apUDKdZ2zR0i53nPVWqowo6e9C0jjCi2HZ79MBh4Bp0ZhPVoCcUG7kPO87ya9Zu6-MHWMuFN8ceWqvSQxlvElUshrnFFALog7HjxTCJSruGWSsySlqANOljDBbJsC3P26qI_FQ');

-- Dumping structure for table users.users
CREATE TABLE IF NOT EXISTS `users` (
  `enabled` bit(1) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `created_by` bigint(20) DEFAULT NULL,
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by` bigint(20) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `user_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKk8d0f2n7n88w1a16yhua64onx` (`user_name`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=latin1;

-- Dumping data for table users.users: ~4 rows (approximately)
INSERT INTO `users` (`enabled`, `created_at`, `created_by`, `id`, `updated_at`, `updated_by`, `email`, `password`, `user_name`) VALUES
	(b'1', NULL, NULL, 1, NULL, NULL, 'giahuy@gmail.com', '$2a$10$xek8.4BtspkW7wnr29AGaedR1OUPW2LrNhnwuy9PMdb/8cRWG/gR2', 'giahuy1'),
	(b'1', NULL, NULL, 2, NULL, NULL, 'giahuy2@gmail.com', '$2a$10$M2HyiIAkr4oZxCEHrsqx7u/0Valw28YfI4srdFJtx/vx2qnABy3X2', 'giahuy2'),
	(b'1', NULL, NULL, 3, NULL, NULL, 'giahuy1@gmail.com', '$2a$10$5QffHXGs5uR/A3nuhhEf7uPwees0sDrPFkrbp1BPIRIlMh220F6zu', 'giahuy3'),
	(b'1', NULL, NULL, 4, NULL, NULL, 'alksdj@gmail.com', '$2a$10$7rHsDnPkxq7DjuHlbao.RufO8SEp7boNH793Ar1iGeZInNKk0ieHa', 'giahuy4');

-- Dumping structure for table users.user_roles
CREATE TABLE IF NOT EXISTS `user_roles` (
  `role_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  PRIMARY KEY (`role_id`,`user_id`),
  KEY `FKhfh9dx7w3ubf1co1vdev94g3f` (`user_id`),
  CONSTRAINT `FKh8ciramu9cc9q3qcqiv4ue8a6` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`),
  CONSTRAINT `FKhfh9dx7w3ubf1co1vdev94g3f` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Dumping data for table users.user_roles: ~0 rows (approximately)

/*!40103 SET TIME_ZONE=IFNULL(@OLD_TIME_ZONE, 'system') */;
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES=IFNULL(@OLD_SQL_NOTES, 1) */;
