-- 1、初始化数据库：创建nacos 外置数据库 flashcode_nacos_dev 和脚手架业务数据库 flashcode_dev
-- 2、创建用户，用户名：zmbdpdev 密码：Hf@173503494
-- 3、授予zmbdpdev用户特定权限
create database if not exists `flashcode_nacos_dev` default character set utf8mb4 collate utf8mb4_general_ci;
create database if not exists `flashcode_dev` default character set utf8mb4 collate utf8mb4_general_ci;
create database if not exists `flashcode_xxljob_dev` default character set utf8mb4 collate utf8mb4_general_ci;

create user 'zmbdpdev'@'%' identified BY 'Hf@173503494';

grant replication slave, replication client on *.* to 'zmbdpdev'@'%';
grant all privileges on flashcode_nacos_dev.* to 'zmbdpdev'@'%';
grant all privileges on flashcode_dev.* to 'zmbdpdev'@'%';
grant all privileges on flashcode_xxljob_dev.* to 'zmbdpdev'@'%';
FLUSH PRIVILEGES;

