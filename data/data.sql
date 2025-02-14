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


-- Dumping database structure for cartdb
CREATE DATABASE IF NOT EXISTS `cartdb` /*!40100 DEFAULT CHARACTER SET armscii8 COLLATE armscii8_bin */;
USE `cartdb`;

-- Dumping structure for table cartdb.carts
CREATE TABLE IF NOT EXISTS `carts` (
  `cart_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) DEFAULT NULL,
  `created_at` date DEFAULT NULL,
  `updated_at` date DEFAULT NULL,
  PRIMARY KEY (`cart_id`),
  KEY `user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=armscii8 COLLATE=armscii8_bin;

-- Dumping data for table cartdb.carts: ~27 rows (approximately)
INSERT INTO `carts` (`cart_id`, `user_id`, `created_at`, `updated_at`) VALUES
	(1, 101, '2025-01-12', '2025-01-12'),
	(2, 102, '2025-01-12', '2025-01-12'),
	(3, 103, '2025-01-12', '2025-01-12'),
	(4, NULL, '2025-01-12', '2025-01-12'),
	(5, NULL, '2025-01-12', '2025-01-12'),
	(6, NULL, '2025-01-12', '2025-01-12'),
	(7, NULL, '2025-01-12', '2025-01-12'),
	(8, NULL, '2025-01-12', '2025-01-12'),


-- Dumping structure for table cartdb.cart_items
CREATE TABLE IF NOT EXISTS `cart_items` (
  `cart_item_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `cart_id` bigint(20) DEFAULT NULL,
  `product_id` bigint(20) DEFAULT NULL,
  `quantity` int(11) DEFAULT NULL,
  `price` float DEFAULT NULL,
  PRIMARY KEY (`cart_item_id`),
  KEY `cart_id` (`cart_id`),
  KEY `product_id` (`product_id`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=armscii8 COLLATE=armscii8_bin;

-- Dumping data for table cartdb.cart_items: ~30 rows (approximately)
INSERT INTO `cart_items` (`cart_item_id`, `cart_id`, `product_id`, `quantity`, `price`) VALUES
	(1, 1, 1, 2, 150),
	(2, 1, 2, 1, 300),
	(3, 2, 3, 1, 500),
	(4, 2, 1, 3, 120),
	(5, 3, 2, 1, 250),
	(6, 3, 3, 2, 180),
	(7, 4, 1, 1, 15.99),
	(8, 5, 2, 1, 1.99),
	(9, 6, 2, 1, 1.99),
	(10, 7, 2, 1, 1.99),
	(11, 8, 2, 1, 1.99),
	(12, 9, 2, 1, 1.99),
	(13, 10, 2, 1, 1.99),
	(14, 11, 2, 1, 1.99),
	(15, 12, 2, 1, 1.99),



-- Dumping database structure for productdb
CREATE DATABASE IF NOT EXISTS `productdb` /*!40100 DEFAULT CHARACTER SET armscii8 COLLATE armscii8_bin */;
USE `productdb`;

-- Dumping structure for table productdb.products
CREATE TABLE IF NOT EXISTS `products` (
  `product_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `category_id` bigint(20) DEFAULT NULL,
  `name` varchar(255) COLLATE armscii8_bin DEFAULT NULL,
  `description` varchar(255) COLLATE armscii8_bin DEFAULT NULL,
  `price` float DEFAULT NULL,
  `stock` int(11) DEFAULT NULL,
  `create_at` date DEFAULT NULL,
  `update_at` date DEFAULT NULL,
  PRIMARY KEY (`product_id`),
  KEY `FK_products_categories` (`category_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=armscii8 COLLATE=armscii8_bin;

-- Dumping data for table productdb.products: ~4 rows (approximately)
INSERT INTO `products` (`product_id`, `category_id`, `name`, `description`, `price`, `stock`, `create_at`, `update_at`) VALUES
	(1, 1, 'Chocolate Cake', 'Rich chocolate layered cake', 15.99, 10, '2025-01-01', '2025-01-01'),
	(2, 2, 'Chocolate Chip Cookie', 'Classic cookie with chocolate chips', 1.99, 50, '2025-01-01', '2025-01-01'),
	(3, 3, 'Vanilla Cupcake', 'Soft cupcake with vanilla frosting', 2.99, 20, '2025-01-01', '2025-01-01');


-- Dumping database structure for userdb
CREATE DATABASE IF NOT EXISTS `userdb` /*!40100 DEFAULT CHARACTER SET armscii8 COLLATE armscii8_bin */;
USE `userdb`;

-- Dumping structure for table userdb.users
CREATE TABLE IF NOT EXISTS `users` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) COLLATE armscii8_bin DEFAULT NULL,
  `password` varchar(50) COLLATE armscii8_bin DEFAULT NULL,
  `full_name` varchar(255) COLLATE armscii8_bin DEFAULT NULL,
  `role` tinytext COLLATE armscii8_bin,
  `email` varchar(255) COLLATE armscii8_bin DEFAULT NULL,
  `dob` date DEFAULT NULL,
  `phone` int(11) DEFAULT NULL,
  `create_at` date DEFAULT NULL,
  `update_at` date DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=armscii8 COLLATE=armscii8_bin;

-- Dumping data for table userdb.users: ~3 rows (approximately)
INSERT INTO `users` (`id`, `username`, `password`, `full_name`, `role`, `email`, `dob`, `phone`, `create_at`, `update_at`) VALUES
	(1, 'admin', 'admin123', 'Admin User', 'ADMIN', 'admin@cakestore.com', '1990-01-01', 1234567890, '2025-01-01', '2025-01-01'),
	(2, 'john_doe', 'password123', 'John Doe', 'USER', 'john.doe@example.com', '1995-05-15', 987654321, '2025-01-01', '2025-01-01'),
	(3, 'jane_doe1', 'password123', 'Jane Doe', 'USER', 'jane.doe@example.com', '1998-07-20', 1231231234, '2025-01-01', '2025-01-01');

/*!40103 SET TIME_ZONE=IFNULL(@OLD_TIME_ZONE, 'system') */;
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES=IFNULL(@OLD_SQL_NOTES, 1) */;
