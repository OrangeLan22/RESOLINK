/*
Navicat MySQL Data Transfer

Source Server         : 本地服务器
Source Server Version : 80038
Source Host           : localhost:3306
Source Database       : resolink

Target Server Type    : MYSQL
Target Server Version : 80038
File Encoding         : 65001

Date: 2026-05-21 11:51:41
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for `adminaccount`
-- ----------------------------
DROP TABLE IF EXISTS `adminaccount`;
CREATE TABLE `adminaccount` (
  `id` int NOT NULL,
  `emp_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `adacc` varchar(255) NOT NULL,
  `adpass` varchar(255) NOT NULL,
  `salt` char(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of adminaccount
-- ----------------------------
INSERT INTO `adminaccount` VALUES ('1', 'ADM001', 'Admin', 'c639bc173ca40f552ef493a7a049488554dfa25591f891a63d8f36bbc0e657f04ecaa135ed3eda36796c5411b9a07418638f87e0d5bac043c07ea458f3ea714d', 'fgrsbght');

-- ----------------------------
-- Table structure for `appointment`
-- ----------------------------
DROP TABLE IF EXISTS `appointment`;
CREATE TABLE `appointment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键，自增字段',
  `emp_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '员工工号',
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '预约人姓名',
  `res_id` bigint NOT NULL COMMENT '资源ID',
  `res_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '资源名称',
  `type` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '资源类型（空间资源/实物资源）',
  `appointment_date` datetime(6) NOT NULL,
  `start_time` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '开始时间',
  `end_time` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '结束时间',
  `status` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '预约状态',
  `approval` int NOT NULL,
  `check` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=57 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约表';

-- ----------------------------
-- Records of appointment
-- ----------------------------
INSERT INTO `appointment` VALUES ('1', 'ADM001', '开发者', '13', '会议室', 'space', '2026-02-26 20:20:47.000000', '1774000800', '1774015200', '-1', '1', '0');
INSERT INTO `appointment` VALUES ('2', 'ADM001', '开发者', '13', '会议室', 'space', '2026-02-26 18:18:37.000000', '1774018800', '1774021800', '0', '1', '0');
INSERT INTO `appointment` VALUES ('3', 'ADM001', '开发者', '13', '会议室', 'space', '2026-02-28 09:27:34.000000', '1773964800', '1773972000', '0', '1', '0');
INSERT INTO `appointment` VALUES ('6', 'ADM001', '凯法哲', '13', '会议室', 'space', '2026-03-01 00:00:00.000000', '1772389440', '1772393520', '0', '1', '0');
INSERT INTO `appointment` VALUES ('7', 'ADM001', '凯法哲', '13', '会议室', 'space', '2026-03-01 00:00:00.000000', '1772383620', '1772399880', '0', '1', '0');
INSERT INTO `appointment` VALUES ('8', 'ADM001', '凯法哲', '13', '会议室', 'space', '2026-03-02 00:00:00.000000', '1772449560', '1772565960', '0', '1', '0');
INSERT INTO `appointment` VALUES ('9', 'ADM001', '凯法哲', '13', '会议室', 'space', '2026-03-05 00:00:00.000000', '1772676000', '1772787720', '0', '1', '0');
INSERT INTO `appointment` VALUES ('10', 'Admin', 'Admin', '14', '会议室', 'space', '2026-03-05 16:28:15.000000', '1772704800', '1772708400', '-1', '1', '0');
INSERT INTO `appointment` VALUES ('11', 'ADM001', '凯法哲', '14', '会议室', 'space', '2026-03-05 16:54:46.000000', '32204', '29897', '0', '-1', '0');
INSERT INTO `appointment` VALUES ('12', 'EMP7612', '林辰', '14', '会议室', 'space', '2026-03-05 16:55:09.000000', '1779367357', '1779453757', '-1', '1', '0');
INSERT INTO `appointment` VALUES ('13', 'ADM001', '凯法哲', '15', '会议室', 'space', '2026-03-05 19:24:18.000000', '1772755200', '1772758800', '0', '1', '0');
INSERT INTO `appointment` VALUES ('14', 'ADM001', '凯法哲', '15', '会议室', 'space', '2026-03-05 21:42:26.000000', '1772780400', '1772784000', '0', '1', '0');
INSERT INTO `appointment` VALUES ('15', 'ADM001', '凯法哲', '15', '会议室', 'space', '2026-03-05 21:43:04.000000', '1772784000', '1772787600', '0', '1', '0');
INSERT INTO `appointment` VALUES ('16', 'EMP7612', '林辰', '15', '会议室', 'space', '2026-03-05 21:52:03.000000', '1772757000', '1772760600', '0', '1', '0');
INSERT INTO `appointment` VALUES ('17', 'EMP1698', '王悦然', '14', '会议室', 'space', '2026-03-08 11:15:36.000000', '1773039600', '1773046800', '0', '1', '0');
INSERT INTO `appointment` VALUES ('18', 'ADM001', '凯法哲', '21', '审片室', 'space', '2026-03-11 12:43:00.000000', '1773298800', '1773302400', '0', '1', '0');
INSERT INTO `appointment` VALUES ('19', 'ADM001', '凯法哲', '11', '哈苏 XD2', 'physical', '2026-03-11 12:58:54.000000', '1773205200', '1773306000', '0', '1', '0');
INSERT INTO `appointment` VALUES ('20', 'ADM001', '凯法哲', '11', '哈苏 XD2', 'physical', '2026-03-14 00:00:00.000000', '1773489000', '1773494340', '0', '1', '0');
INSERT INTO `appointment` VALUES ('21', 'ADM001', '凯法哲', '14', '大疆 Inspire 3', 'physical', '2026-03-14 00:00:00.000000', '1773517740', '1773530520', '0', '1', '0');
INSERT INTO `appointment` VALUES ('22', 'EMP7612', '林辰', '16', '封闭剪辑室', 'space', '2026-03-14 00:00:00.000000', '1773515400', '1773533520', '0', '1', '0');
INSERT INTO `appointment` VALUES ('23', 'EMP7612', '林辰', '21', '审片室', 'space', '2026-03-14 00:00:00.000000', '1773486960', '1773501240', '0', '1', '0');
INSERT INTO `appointment` VALUES ('24', 'EMP7612', '林辰', '17', '大疆 Ronin 4D-8K', 'physical', '2026-03-17 00:00:00.000000', '1773788160', '1773834960', '0', '1', '0');
INSERT INTO `appointment` VALUES ('25', 'ADM001', '凯法哲', '13', '会议室', 'space', '2026-03-15 00:00:00.000000', '1773615900', '1773667260', '0', '1', '0');
INSERT INTO `appointment` VALUES ('26', 'ADM001', '凯法哲', '13', '会议室', 'space', '2026-03-15 00:00:00.000000', '1773601740', '1773605580', '0', '1', '0');
INSERT INTO `appointment` VALUES ('27', 'ADM001', '凯法哲', '14', '会议室', 'space', '2026-03-15 00:00:00.000000', '1773609780', '1773615360', '0', '1', '0');
INSERT INTO `appointment` VALUES ('28', 'ADM001', '凯法哲', '11', '哈苏 XD2', 'physical', '2026-03-15 00:00:00.000000', '1773604560', '1773609900', '0', '1', '0');
INSERT INTO `appointment` VALUES ('29', 'ADM001', '凯法哲', '11', '哈苏 XD2', 'physical', '2026-03-16 00:00:00.000000', '1773620760', '1773636120', '0', '1', '0');
INSERT INTO `appointment` VALUES ('30', 'ADM001', '凯法哲', '14', '大疆 Inspire 3', 'physical', '2026-03-16 00:00:00.000000', '1773683040', '1773687780', '0', '1', '0');
INSERT INTO `appointment` VALUES ('31', 'ADM001', '凯法哲', '21', '审片室', 'space', '2026-03-16 00:00:00.000000', '1773695760', '1773707460', '0', '1', '0');
INSERT INTO `appointment` VALUES ('33', 'ADM001', '凯法哲', '14', '大疆 Inspire 3', 'physical', '2026-03-17 00:00:00.000000', '1773758040', '1773763140', '0', '1', '0');
INSERT INTO `appointment` VALUES ('34', 'ADM001', '凯法哲', '11', '哈苏 XD2', 'physical', '2026-03-17 00:00:00.000000', '1773756780', '1773759780', '0', '1', '0');
INSERT INTO `appointment` VALUES ('35', 'EMP268', '凯法哲', '11', '哈苏 XD2', 'physical', '2026-03-17 00:00:00.000000', '1773765120', '1773769860', '0', '1', '0');
INSERT INTO `appointment` VALUES ('36', 'ADM001', '凯法哲', '13', '哈苏 XD2', 'physical', '2026-03-17 00:00:00.000000', '1773751357', '1773760544', '0', '1', '0');
INSERT INTO `appointment` VALUES ('37', 'ADM001', '凯法哲', '16', '封闭剪辑室', 'space', '2026-03-17 00:00:00.000000', '1773763860', '1773763140', '0', '1', '0');
INSERT INTO `appointment` VALUES ('45', 'ADM001', '凯法哲', '20', '开放剪辑室', 'space', '2026-03-26 00:00:00.000000', '1774515000', '1774531140', '0', '1', '0');
INSERT INTO `appointment` VALUES ('46', 'ADM001', '凯法哲', '21', '审片室', 'space', '2026-03-18 22:07:45.000000', '1773903600', '1773907200', '0', '1', '0');
INSERT INTO `appointment` VALUES ('47', 'ADM001', '凯法哲', '12', '哈苏 XD2', 'physical', '2026-03-25 00:00:00.000000', '1774403100', '1774435140', '0', '1', '0');
INSERT INTO `appointment` VALUES ('48', 'ADM001', '凯法哲', '14', '会议室', 'space', '2026-04-29 00:00:00.000000', '1777486560', '1777498440', '0', '1', '0');
INSERT INTO `appointment` VALUES ('49', 'ADM001', '凯法哲', '18', '调音室', 'space', '2026-03-26 00:00:00.000000', '1774564620', '1774575480', '0', '1', '0');
INSERT INTO `appointment` VALUES ('50', 'ADM001', '凯法哲', '13', '会议室', 'space', '2026-04-10 00:00:00.000000', '1773601740', '1775816535', '0', '1', '0');
INSERT INTO `appointment` VALUES ('51', 'ADM001', '凯法哲', '14', '会议室', 'space', '2026-04-16 00:00:00.000000', '1776371280', '1776384780', '0', '-1', '0');
INSERT INTO `appointment` VALUES ('52', 'ADM001', '凯法哲', '17', '封闭剪辑室', 'space', '2026-05-12 00:00:00.000000', '1778547600', '1778565600', '-1', '-1', '0');
INSERT INTO `appointment` VALUES ('53', 'ADM001', '凯法哲', '17', '大疆 Ronin 4D-8K', 'physical', '2026-05-18 00:00:00.000000', '1779134880', '1778498862', '0', '1', '1');
INSERT INTO `appointment` VALUES ('54', 'ADM001', '凯法哲', '15', '会议室', 'space', '2026-05-19 00:00:00.000000', '1779206400', '1778499413', '0', '0', '0');
INSERT INTO `appointment` VALUES ('55', 'ADM001', '凯法哲', '14', '会议室', 'space', '2026-05-11 19:53:10.000000', '1778569200', '1778576400', '0', '0', '0');
INSERT INTO `appointment` VALUES ('56', 'ADM001', '凯法哲', '14', '会议室', 'space', '2026-05-19 00:00:00.000000', '1779206400', '1779216240', '0', '0', '0');

-- ----------------------------
-- Table structure for `authoritys`
-- ----------------------------
DROP TABLE IF EXISTS `authoritys`;
CREATE TABLE `authoritys` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tag` char(255) NOT NULL,
  `appointment` int NOT NULL,
  `public-info` int NOT NULL,
  `account-mgm` int NOT NULL,
  `resource-mgm` int NOT NULL,
  `history` int NOT NULL,
  `check` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=110 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of authoritys
-- ----------------------------
INSERT INTO `authoritys` VALUES ('99', '高级管理员', '1', '1', '1', '1', '1', '1');
INSERT INTO `authoritys` VALUES ('104', '资源管理员', '1', '0', '0', '1', '1', '0');
INSERT INTO `authoritys` VALUES ('105', '员工', '1', '0', '0', '0', '0', '0');
INSERT INTO `authoritys` VALUES ('109', '设备检察员', '1', '0', '0', '0', '0', '1');

-- ----------------------------
-- Table structure for `department`
-- ----------------------------
DROP TABLE IF EXISTS `department`;
CREATE TABLE `department` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dep_name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of department
-- ----------------------------
INSERT INTO `department` VALUES ('16', '内容创作部');
INSERT INTO `department` VALUES ('17', '市场营销部');
INSERT INTO `department` VALUES ('18', '运营部');
INSERT INTO `department` VALUES ('19', '技术部');
INSERT INTO `department` VALUES ('20', '设计与创意部');
INSERT INTO `department` VALUES ('21', '后勤部');

-- ----------------------------
-- Table structure for `physical_resource`
-- ----------------------------
DROP TABLE IF EXISTS `physical_resource`;
CREATE TABLE `physical_resource` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `equipment_name` varchar(255) NOT NULL,
  `location` varchar(255) NOT NULL,
  `type` varchar(255) NOT NULL,
  `public` int NOT NULL,
  `dep_id` varchar(255) DEFAULT NULL,
  `check` int NOT NULL,
  `note` text NOT NULL,
  `stage` int NOT NULL,
  `tag` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of physical_resource
-- ----------------------------
INSERT INTO `physical_resource` VALUES ('11', '哈苏 XD2', '3号楼-101', '相机', '0', '16', '1', '贵重设备，注意保护', '0', '办公设备');
INSERT INTO `physical_resource` VALUES ('12', '哈苏 XD2', '3号楼-101', '相机', '0', '16', '1', '贵重设备，注意保护', '0', '办公设备');
INSERT INTO `physical_resource` VALUES ('13', '哈苏 XD2', '3号楼-101', '相机', '0', '16', '1', '贵重设备，注意保护', '0', '办公设备');
INSERT INTO `physical_resource` VALUES ('14', '大疆 Inspire 3', '3号楼-101', '无人机', '0', '16', '1', '贵重设备，注意保护', '0', '办公设备');
INSERT INTO `physical_resource` VALUES ('15', '大疆 Inspire 3', '3号楼-101', '无人机', '0', '16', '1', '贵重设备，注意保护', '0', '办公设备');
INSERT INTO `physical_resource` VALUES ('16', '大疆 Ronin 4D-8K', '3号楼-101', '专业相机', '0', '16', '1', '贵重设备，注意保护', '0', '办公设备');
INSERT INTO `physical_resource` VALUES ('17', '大疆 Ronin 4D-8K', '3号楼-101', '专业相机', '0', '16', '1', '贵重设备，注意保护', '0', '办公设备');
INSERT INTO `physical_resource` VALUES ('19', 'A4纸（箱装）', 'A-106', '打印纸', '1', null, '0', '', '0', '办公耗材');
INSERT INTO `physical_resource` VALUES ('20', '硒鼓', 'A-106', '打印机耗材', '1', null, '0', '', '0', '办公耗材');
INSERT INTO `physical_resource` VALUES ('21', '商务接待车', '厂-2', '接待车', '1', null, '0', '', '0', '交通出行');
INSERT INTO `physical_resource` VALUES ('22', '商务接待车', '厂-2', '接待车', '1', null, '0', '', '0', '交通出行');
INSERT INTO `physical_resource` VALUES ('23', '轨道车', '厂-2', '拍摄专用交通资源', '1', null, '0', '', '0', '交通出行');
INSERT INTO `physical_resource` VALUES ('24', '轨道车', '厂-2', '拍摄专用交通资源', '1', null, '0', '', '0', '交通出行');
INSERT INTO `physical_resource` VALUES ('25', '信号车', '厂-2', '直播信号车', '1', null, '1', '', '0', '交通出行');

-- ----------------------------
-- Table structure for `public`
-- ----------------------------
DROP TABLE IF EXISTS `public`;
CREATE TABLE `public` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `companyname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of public
-- ----------------------------
INSERT INTO `public` VALUES ('1', '杭州青奥传媒有限公司');

-- ----------------------------
-- Table structure for `space_resource`
-- ----------------------------
DROP TABLE IF EXISTS `space_resource`;
CREATE TABLE `space_resource` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `space_name` varchar(255) NOT NULL,
  `location` varchar(255) NOT NULL,
  `type` varchar(255) NOT NULL,
  `capacity` int NOT NULL,
  `public` int NOT NULL,
  `dep_id` varchar(255) DEFAULT NULL,
  `check` int NOT NULL,
  `note` text NOT NULL,
  `stage` int NOT NULL,
  `tag` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of space_resource
-- ----------------------------
INSERT INTO `space_resource` VALUES ('12', '会议室', '4号楼-112', '小型会议室', '11', '1', null, '0', '配备投影仪与白板', '-1', '公共辅助');
INSERT INTO `space_resource` VALUES ('13', '会议室', '4号楼-212', '小型会议室', '10', '1', null, '1', '配备投影仪与白板', '0', '公共辅助');
INSERT INTO `space_resource` VALUES ('14', '会议室', '4号楼-312', '小型会议室', '10', '1', null, '0', '配备投影仪与白板', '0', '公共辅助');
INSERT INTO `space_resource` VALUES ('15', '会议室', '5号楼-206', '大型会议室', '30', '1', null, '0', '配备电子屏与白板', '0', '公共辅助');
INSERT INTO `space_resource` VALUES ('16', '封闭剪辑室', '5号楼-105', '剪辑室', '2', '0', '16,20', '1', '2台台式电脑', '0', '核心业务');
INSERT INTO `space_resource` VALUES ('17', '封闭剪辑室', '5号楼-106', '剪辑室', '2', '0', '16,20', '1', '2台台式电脑', '0', '核心业务');
INSERT INTO `space_resource` VALUES ('18', '调音室', '5号楼-216', '调音室', '2', '0', '16', '1', '配备调音台与电子屏', '0', '核心业务');
INSERT INTO `space_resource` VALUES ('19', '开放剪辑室', '5号楼-210', '剪辑室', '4', '0', '16,20', '1', '4台台式电脑', '0', '核心业务');
INSERT INTO `space_resource` VALUES ('20', '开放剪辑室', '5号楼-211', '剪辑室', '4', '0', '16,20', '1', '4台台式电脑', '0', '核心业务');
INSERT INTO `space_resource` VALUES ('21', '审片室', '5号楼-213', '审片室', '5', '0', '16', '1', '', '0', '核心业务');
INSERT INTO `space_resource` VALUES ('27', '影棚', '厂-1', '摄影棚', '20', '1', null, '1', '', '0', '特殊功能');

-- ----------------------------
-- Table structure for `status`
-- ----------------------------
DROP TABLE IF EXISTS `status`;
CREATE TABLE `status` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sta_name` varchar(255) NOT NULL,
  `dep_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKbttbp11u0euxjpuj6e813v45x` (`dep_id`)
) ENGINE=InnoDB AUTO_INCREMENT=66 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of status
-- ----------------------------
INSERT INTO `status` VALUES ('33', '策划', '16');
INSERT INTO `status` VALUES ('34', '编导', '16');
INSERT INTO `status` VALUES ('35', '导演', '16');
INSERT INTO `status` VALUES ('36', '摄像师', '16');
INSERT INTO `status` VALUES ('37', '灯光师', '16');
INSERT INTO `status` VALUES ('38', '录音师', '16');
INSERT INTO `status` VALUES ('39', '剪辑师', '16');
INSERT INTO `status` VALUES ('40', '设计师', '16');
INSERT INTO `status` VALUES ('41', '场记', '16');
INSERT INTO `status` VALUES ('43', '市场策划', '17');
INSERT INTO `status` VALUES ('44', '客户经理', '17');
INSERT INTO `status` VALUES ('45', '品牌专员', '17');
INSERT INTO `status` VALUES ('46', '运营经理', '18');
INSERT INTO `status` VALUES ('47', '新媒体运营', '18');
INSERT INTO `status` VALUES ('48', '用户运营', '18');
INSERT INTO `status` VALUES ('49', '公关', '18');
INSERT INTO `status` VALUES ('50', '商务总监', '17');
INSERT INTO `status` VALUES ('51', '产品经理', '19');
INSERT INTO `status` VALUES ('52', '前端工程师', '19');
INSERT INTO `status` VALUES ('53', '后端工程师', '19');
INSERT INTO `status` VALUES ('54', '数据分析师', '19');
INSERT INTO `status` VALUES ('55', '运维工程师', '19');
INSERT INTO `status` VALUES ('56', '创意总监', '20');
INSERT INTO `status` VALUES ('57', '平面设计师', '20');
INSERT INTO `status` VALUES ('58', 'UI设计师', '20');
INSERT INTO `status` VALUES ('59', '动画特效师', '20');
INSERT INTO `status` VALUES ('60', '采购专员', '21');
INSERT INTO `status` VALUES ('61', '维护专员', '21');
INSERT INTO `status` VALUES ('62', '仓库管理员', '21');

-- ----------------------------
-- Table structure for `useraccount`
-- ----------------------------
DROP TABLE IF EXISTS `useraccount`;
CREATE TABLE `useraccount` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `emp_id` varchar(255) NOT NULL,
  `useracc` varchar(255) NOT NULL,
  `userpass` varchar(255) NOT NULL,
  `salt` char(8) NOT NULL,
  `isinitial` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=56 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of useraccount
-- ----------------------------
INSERT INTO `useraccount` VALUES ('12', 'EMP7612', 'LingChen', 'f511001a66d646e8f0b0cf20f4607f84abf55603beff7202709f85f4e2d8fadcaf89fb8185be90ffb8cca1e9e42ddb8dfe15a30731f21d7b3c89b7134a734638', 'RkDGQKRg', '1');
INSERT INTO `useraccount` VALUES ('13', 'EMP1698', 'WangYueRan', '219d563153b8dda8fb9d4ab0fc300346548e2df048b684973930f769f22ea2a47dda1fa66fcf95d5d6120186245e0162c6c2aaa48d289d518de6d39a941c3dcc', 'mGYbJjbB', '1');
INSERT INTO `useraccount` VALUES ('14', 'EMP4987', 'ZhangYu', 'f787197823525b6015b01d78b7c1df06b0b45c4ec061a2b7ec66caf665c001df76d5cbb07db2076aa5d6112340c03ff94b0540d354764b33c7463ec74603896e', 'ouNiIIvY', '1');
INSERT INTO `useraccount` VALUES ('15', 'EMP1269', 'LiuChangAn', '7bcbae6cba6fe295cebf33e6146e439595ce8e5224c0a0c33a982dffc06b63dc96607b9f933ead7b2ce51397e6f07751abf400ca260d8d32e8d0cde1d1fd9e2e', 'ksqEYuLJ', '1');
INSERT INTO `useraccount` VALUES ('16', 'EMP5133', 'ChenNuo', 'b5aa41e9df3915b62e580b08ac8b393514b0086ff8429a10891d3522092b210c2b0369e6a9c46745d3f779d40dd3da70c1763961e0581415c0c844d1d192b6eb', 'kvtskqie', '1');
INSERT INTO `useraccount` VALUES ('17', 'EMP4593', 'ZhaoShuHeng', '2e5e27c1eb9659fe03eefaa9d63f3457f14f17f0d56ccc045fa436ba5d0f5506da909153cf6b204f8fa28f30bdb5152341e2c7ddc5d13187e15c4578fa6809df', 'wYBVnbGq', '1');
INSERT INTO `useraccount` VALUES ('18', 'EMP9870', 'HuangMu', '31afc46cc818bbaa10fb407e7fa94a03b1cf779041e16552cf2b13b075a05eadaaaf953032c84d4d2b76bab60f73d334e7d3b2778bb7063037d24284ae3529ae', 'liXkESYn', '1');
INSERT INTO `useraccount` VALUES ('19', 'EMP1562', 'WuYouLe', '313850a7e89d7056650dda23169e0a1e1320c25039e8d4dd228793478fd18d7a0af4438244434e0727b6162e2f21aa4c2c419e2bf27b71e0e2ad97e07da78319', 'FSXdQLjN', '1');
INSERT INTO `useraccount` VALUES ('20', 'EMP2001', 'ZhouAn', '053b64f8b1c58c7b470ddd8abc6f3673041032768c018fc03165dc9651cd78a1b5aa597c6409596fb700f6a211e89c9e3c0459d0a678fac3f50a6d0760556e73', 'CnWqTwBx', '1');
INSERT INTO `useraccount` VALUES ('21', 'EMP3265', 'XuZhiXia', '0b846da9fc51d5587f0e28b45f1fdb2e6e3a66dc19f4745bfa54b335df309f4d67b4247f6ae6917b2f2a76b941ffa1484a30cee25ee626e06dc197f057527da8', 'pwdIySFE', '1');
INSERT INTO `useraccount` VALUES ('22', 'EMP5545', 'SunNian', '023e3c99af961e36c99db99bf086019828c2657037f316d8100eeca7615137fcd9e48dab3c066b8043aa9bd27c5cdd8694db8e9d11e94353a07b168731f92796', 'QyTXEKRs', '1');
INSERT INTO `useraccount` VALUES ('23', 'EMP6642', 'MaPingChuan', '8179c2d094a583ada41cd7d743a344c87c61e12e0c50ecf22ed64ec031dbf40c67250461a4fdba9e9d85e4e62e4bde1f69caedfc526182208c14b18b071c28f0', 'srKYbTBS', '1');
INSERT INTO `useraccount` VALUES ('24', 'EMP5499', 'ZhuHe', 'aacc4edaf7d67fac6e3547b9742bfab78103c39d0e708cacb0b6c3fdff8a913b2a7dc4c20cfe29562e2c94b5717af6d851a90dc1961b9699d23ab143d129a317', 'AWWWJUAr', '1');
INSERT INTO `useraccount` VALUES ('25', 'EMP4620', 'HuKeXin', '333b2cc928c829c01d6feecb6f381b39988cca52af8b5235bf6657ce2bcc775f53e06eae8ba52d81a715ed655455004f7939606a28ee32c7b6a761a3a41bf1fe', 'MbkkewxP', '1');
INSERT INTO `useraccount` VALUES ('26', 'EMP1613', 'GuoXiao', '2dbfe60a2577c2e9729081b4b2086489fb432b468b9fb8ba6d815650003e689a203a0c6512a247bfc1b3092bdcd2c5f3eb55fa207fa68fbecea1b97331faa3da', 'OoBtaDVD', '1');
INSERT INTO `useraccount` VALUES ('27', 'EMP1321', 'HeYiFan', '7653c1fdae511ac1b807e97936f87c6a915037fa2c4a676285606a03cdcb1c1789c247ca99d3b0a9f68b354203ba203e8bb140f6e88afdd919c7b5200ed5faa6', 'zAQptCdE', '1');
INSERT INTO `useraccount` VALUES ('28', 'EMP2231', 'GaoYue', 'b4d384a893458c036e2c266d77d68ca8c00770880e0f548331e5ac6e5096571a6a5c51e870a53c133e52860221e89d1059401be4fd41c97c6913db15b2e6b693', 'zouPSaWo', '1');
INSERT INTO `useraccount` VALUES ('29', 'EMP6521', 'LuoQiYue', '7aea490e23181313f872266d75619d5a1012e48c574ba3945e59621a1e34e403f04415da06380144eafe08ec067e14c747dc057be696f1d4cdcc6f3147462469', 'fPTmcTDV', '1');
INSERT INTO `useraccount` VALUES ('30', 'EMP3600', 'ZhengHang', '4f320079b098e108c8694cff6f441f4de856ead51f478970d64e6ce07ae974db4503cacb0c5750be75f84b0b44b1db16f6f2b34d4ec9d83b701a86f5d9870dcf', 'JQZjGOwJ', '1');
INSERT INTO `useraccount` VALUES ('31', 'EMP0231', 'LiangXiYue', 'c095b586e8f03dea346ed4e4794d4d02f7977c517a281e79f60ff3daa0d7b29a39cef246a8aa7e395fc0ce4c2e1b152cf30e032f030dc4a253fa36fc63a80e34', 'iwlomFmb', '1');
INSERT INTO `useraccount` VALUES ('32', 'EMP0031', 'XieLin', 'a347784eb99ba00357cdb335aa46cd81e5dca3140668b19d68e22dc8858d9334f761cb5ba03453cf7508134be68655a201d14d2f5aa70ae1f5bac10b3b03e1cb', 'jcyUmjMu', '1');
INSERT INTO `useraccount` VALUES ('33', 'EMP1030', 'SongNuanYang', '5fdc575247af4c57d3ebc3802d895a4c5b22e29f876aa65638e4fc79ba2ae12564f6e199921c0ecca500a2b9547f640145b9b60ab5d977ec68db6033e220886a', 'skWDXbSj', '1');
INSERT INTO `useraccount` VALUES ('34', 'EMP0218', 'TangZe', '9cf30af381acc7f9e98c7945cabd9ded67a4579338c6690fb8725e5d086acd0b1bd37770e5e7a4096ea4ce7b61c13c7d5c0abe8bb3584b16024e81dc2e460b95', 'DRNTwMlt', '1');
INSERT INTO `useraccount` VALUES ('35', 'EMP2212', 'HanXingYao', '2e74917a12bc20dbe96ca9374fefc41a11e0631df6c0ba1f88fa43b716bfab3c9bcab33019626f6133a5d209b05da81652fee30e843b8593a28d7a3ab739030e', 'tprNqJqn', '1');
INSERT INTO `useraccount` VALUES ('36', 'EMP1564', 'ZhangWei', '94fb8d73e0c5d1be4cfa39acdf709ba8611c7858ac3156ba8c0ff75fccb73d9b25ce131f697a27e57f7e73e5c132c56ab8fdc6a5ce65f7a8f729b703d3d1d999', 'qGgdbPpJ', '1');
INSERT INTO `useraccount` VALUES ('37', 'EMP0176', 'ZhouJian', 'a4999400df7572f1704e89a05aef8461e3e705b30288d7e95aa070eba7f91ed5cf1ca5f97ea387412ba1a710deb4ae95cb5f126b319289911fd9eaff3e8a1778', 'yBOpsgnd', '1');
INSERT INTO `useraccount` VALUES ('38', 'EMP1538', 'ChenXue', '24090dc8ac7da800a3461a9cfeb2f00355afdd8dd435c9a98a84fc1e7b964a04370a1552aa7ec9d04ca503684062a61b3fdd6e9fc03df9eee0032180915c2927', 'gGeQPPNc', '1');

-- ----------------------------
-- Table structure for `user_info`
-- ----------------------------
DROP TABLE IF EXISTS `user_info`;
CREATE TABLE `user_info` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `dep_id` bigint DEFAULT NULL,
  `sta_id` bigint DEFAULT NULL,
  `auth_id` bigint DEFAULT NULL,
  `emp_id` varchar(255) NOT NULL,
  `feishu_user_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=57 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of user_info
-- ----------------------------
INSERT INTO `user_info` VALUES ('1', '凯法哲', null, null, '99', 'ADM001', 'ou_34f2bce1d7c6ee53bdda690f4a64f193');
INSERT INTO `user_info` VALUES ('13', '林辰', '16', '33', '105', 'EMP7612', null);
INSERT INTO `user_info` VALUES ('14', '王悦然', '16', '34', '105', 'EMP1698', null);
INSERT INTO `user_info` VALUES ('15', '张宇', '16', '35', '105', 'EMP4987', null);
INSERT INTO `user_info` VALUES ('16', '刘畅安', '16', '36', '105', 'EMP1269', null);
INSERT INTO `user_info` VALUES ('17', '陈诺', '16', '37', '105', 'EMP5133', null);
INSERT INTO `user_info` VALUES ('18', '赵书恒', '16', '38', '105', 'EMP4593', null);
INSERT INTO `user_info` VALUES ('19', '黄沐', '16', '39', '105', 'EMP9870', null);
INSERT INTO `user_info` VALUES ('20', '吴悠乐', '16', '40', '105', 'EMP1562', null);
INSERT INTO `user_info` VALUES ('21', '周安', '16', '41', '105', 'EMP2001', null);
INSERT INTO `user_info` VALUES ('22', '徐知夏', '17', '43', '105', 'EMP3265', null);
INSERT INTO `user_info` VALUES ('23', '孙念', '17', '44', '105', 'EMP5545', null);
INSERT INTO `user_info` VALUES ('24', '马平川', '17', '45', '105', 'EMP6642', null);
INSERT INTO `user_info` VALUES ('25', '朱禾', '17', '50', '105', 'EMP5499', null);
INSERT INTO `user_info` VALUES ('26', '胡可昕', '18', '46', '105', 'EMP4620', null);
INSERT INTO `user_info` VALUES ('27', '郭晓', '18', '47', '105', 'EMP1613', null);
INSERT INTO `user_info` VALUES ('28', '何亦帆', '18', '48', '105', 'EMP1321', null);
INSERT INTO `user_info` VALUES ('29', '高越', '18', '49', '105', 'EMP2231', null);
INSERT INTO `user_info` VALUES ('30', '罗琦玥', '19', '51', '105', 'EMP6521', null);
INSERT INTO `user_info` VALUES ('31', '郑航', '19', '52', '105', 'EMP3600', null);
INSERT INTO `user_info` VALUES ('32', '梁溪月', '19', '53', '105', 'EMP0231', null);
INSERT INTO `user_info` VALUES ('33', '谢霖', '19', '54', '105', 'EMP0031', null);
INSERT INTO `user_info` VALUES ('34', '宋暖阳', '19', '55', '105', 'EMP1030', null);
INSERT INTO `user_info` VALUES ('35', '唐泽', '20', '56', '105', 'EMP0218', null);
INSERT INTO `user_info` VALUES ('36', '韩星遥', '20', '57', '105', 'EMP2212', null);
INSERT INTO `user_info` VALUES ('37', '张伟', '20', '58', '105', 'EMP1564', null);
INSERT INTO `user_info` VALUES ('38', '周健', '20', '59', '105', 'EMP0176', null);
INSERT INTO `user_info` VALUES ('39', '陈雪', '21', '60', '105', 'EMP1538', null);
