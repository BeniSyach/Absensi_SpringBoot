/*
 Navicat Premium Data Transfer

 Source Server         : DB LOCAL POSTGRE
 Source Server Type    : PostgreSQL
 Source Server Version : 160004 (160004)
 Source Host           : localhost:5432
 Source Catalog        : absensi_db
 Source Schema         : public

 Target Server Type    : PostgreSQL
 Target Server Version : 160004 (160004)
 File Encoding         : 65001

 Date: 19/06/2026 16:45:30
*/


-- ----------------------------
-- Sequence structure for absen_masuk_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."absen_masuk_id_seq";
CREATE SEQUENCE "public"."absen_masuk_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for absen_pulang_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."absen_pulang_id_seq";
CREATE SEQUENCE "public"."absen_pulang_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for opd_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."opd_id_seq";
CREATE SEQUENCE "public"."opd_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for shift_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."shift_id_seq";
CREATE SEQUENCE "public"."shift_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for users_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."users_id_seq";
CREATE SEQUENCE "public"."users_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for waktu_kerja_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."waktu_kerja_id_seq";
CREATE SEQUENCE "public"."waktu_kerja_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Table structure for absen_masuk
-- ----------------------------
DROP TABLE IF EXISTS "public"."absen_masuk";
CREATE TABLE "public"."absen_masuk" (
  "id" int8 NOT NULL DEFAULT nextval('absen_masuk_id_seq'::regclass),
  "akurasi_gps" float4,
  "catatan" varchar(500) COLLATE "pg_catalog"."default",
  "created_at" timestamp(6),
  "device_info" varchar(200) COLLATE "pg_catalog"."default",
  "foto_absen" varchar(255) COLLATE "pg_catalog"."default",
  "ip_address" varchar(50) COLLATE "pg_catalog"."default",
  "jarak_dari_kantor" float8,
  "kecepatan_perpindahan" float8,
  "latitude" float8 NOT NULL,
  "location_provider" varchar(50) COLLATE "pg_catalog"."default",
  "lokasi_valid" bool NOT NULL,
  "longitude" float8 NOT NULL,
  "mock_location_detected" bool,
  "status" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "tanggal" date NOT NULL,
  "waktu_masuk" timestamp(6) NOT NULL,
  "opd_id" int8 NOT NULL,
  "shift_id" int8,
  "user_id" int8 NOT NULL
)
;

-- ----------------------------
-- Records of absen_masuk
-- ----------------------------
INSERT INTO "public"."absen_masuk" VALUES (3, 5, NULL, '2026-06-17 18:56:07.123402', 'Android 11 sdk_gphone_x86', '2026/06/17/masuk_1_e4ede531.jpg', '172.100.20.4', 13812064.605328713, 0, 37.4219983, 'fused', 'f', -122.084, 'f', 'HADIR', '2026-06-17', '2026-06-17 18:56:07.122402', 1, NULL, 1);

-- ----------------------------
-- Table structure for absen_pulang
-- ----------------------------
DROP TABLE IF EXISTS "public"."absen_pulang";
CREATE TABLE "public"."absen_pulang" (
  "id" int8 NOT NULL DEFAULT nextval('absen_pulang_id_seq'::regclass),
  "akurasi_gps" float4,
  "catatan" varchar(500) COLLATE "pg_catalog"."default",
  "created_at" timestamp(6),
  "device_info" varchar(200) COLLATE "pg_catalog"."default",
  "durasi_kerja_menit" int4,
  "foto_absen" varchar(255) COLLATE "pg_catalog"."default",
  "ip_address" varchar(50) COLLATE "pg_catalog"."default",
  "jarak_dari_kantor" float8,
  "kecepatan_perpindahan" float8,
  "latitude" float8 NOT NULL,
  "location_provider" varchar(50) COLLATE "pg_catalog"."default",
  "lokasi_valid" bool NOT NULL,
  "longitude" float8 NOT NULL,
  "mock_location_detected" bool,
  "status" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "tanggal" date NOT NULL,
  "waktu_pulang" timestamp(6) NOT NULL,
  "absen_masuk_id" int8,
  "opd_id" int8 NOT NULL,
  "shift_id" int8,
  "user_id" int8 NOT NULL
)
;

-- ----------------------------
-- Records of absen_pulang
-- ----------------------------

-- ----------------------------
-- Table structure for opd
-- ----------------------------
DROP TABLE IF EXISTS "public"."opd";
CREATE TABLE "public"."opd" (
  "id" int8 NOT NULL DEFAULT nextval('opd_id_seq'::regclass),
  "aktif" bool,
  "alamat" varchar(500) COLLATE "pg_catalog"."default",
  "created_at" timestamp(6),
  "kode" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "latitude_kantor" float8 NOT NULL,
  "longitude_kantor" float8 NOT NULL,
  "nama" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "radius_absen" int4 NOT NULL,
  "updated_at" timestamp(6)
)
;

-- ----------------------------
-- Records of opd
-- ----------------------------
INSERT INTO "public"."opd" VALUES (1, 't', 'Jl. Kapten Maulana Lubis No.2, Medan', '2026-06-12 12:05:03.277098', 'SEKRETARIAT', 3.5952, 98.6722, 'Sekretariat Daerah', 100, '2026-06-12 12:05:03.277098');
INSERT INTO "public"."opd" VALUES (2, 't', 'Jl. Pintu Air IV, Medan', '2026-06-12 12:05:03.292098', 'DISHUB', 3.587, 98.68, 'Dinas Perhubungan', 150, '2026-06-12 12:05:03.292098');

-- ----------------------------
-- Table structure for shift
-- ----------------------------
DROP TABLE IF EXISTS "public"."shift";
CREATE TABLE "public"."shift" (
  "id" int8 NOT NULL DEFAULT nextval('shift_id_seq'::regclass),
  "aktif" bool,
  "created_at" timestamp(6),
  "jam_masuk" time(6) NOT NULL,
  "jam_pulang" time(6) NOT NULL,
  "nama" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "toleransi_pulang_awal" int4,
  "toleransi_terlambat" int4,
  "updated_at" timestamp(6),
  "opd_id" int8 NOT NULL
)
;

-- ----------------------------
-- Records of shift
-- ----------------------------
INSERT INTO "public"."shift" VALUES (1, 't', '2026-06-12 12:05:03.296098', '07:30:00', '16:00:00', 'Shift Pagi', 10, 15, '2026-06-12 12:05:03.296098', 1);
INSERT INTO "public"."shift" VALUES (2, 't', '2026-06-12 12:05:03.300098', '12:00:00', '20:00:00', 'Shift Siang', 10, 15, '2026-06-12 12:05:03.300098', 1);

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS "public"."users";
CREATE TABLE "public"."users" (
  "id" int8 NOT NULL DEFAULT nextval('users_id_seq'::regclass),
  "aktif" bool,
  "created_at" timestamp(6),
  "device_id" varchar(100) COLLATE "pg_catalog"."default",
  "email" varchar(200) COLLATE "pg_catalog"."default",
  "foto_profil" varchar(255) COLLATE "pg_catalog"."default",
  "nama_lengkap" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "nip" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "password" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "role" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "telepon" varchar(20) COLLATE "pg_catalog"."default",
  "updated_at" timestamp(6),
  "username" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "opd_id" int8 NOT NULL
)
;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO "public"."users" VALUES (3, 't', '2026-06-12 12:05:04.128555', NULL, 'budi.santoso@absensi.go.id', NULL, 'Budi Santoso', '199001012020121001', '$2a$12$Pu359gwSaKmiZ2HJjNh1WeyHke0UHez22qOzJZ9qUdhDxNEHhiLea', 'ROLE_USER', '0833333333', '2026-06-12 12:05:04.128555', 'budi.santoso', 1);
INSERT INTO "public"."users" VALUES (1, 't', '2026-06-12 12:05:03.581624', 'android-sdk_gphone_x86-80713d5e', 'admin@absensi.go.id', NULL, 'Administrator Sistem', '000000000000000001', '$2a$12$j.wdABmirhD9NvXyJPa3ouoDVuWbU/RTnsAINb94RpoipbWMJwR5K', 'ROLE_ADMIN', '0811111111', '2026-06-17 18:57:41.381046', 'admin', 1);
INSERT INTO "public"."users" VALUES (2, 't', '2026-06-12 12:05:03.854628', NULL, 'pimpinan@absensi.go.id', NULL, 'Kepala Dinas', '198501012010011001', '$2a$12$JG1mbjrdjTThTaRko4KYN.zw0QCHGWimYYDFfLCS90AZGJHBr1jrG', 'ROLE_PIMPINAN', '0822222222', '2026-06-19 15:26:51.791239', 'pimpinan', 1);

-- ----------------------------
-- Table structure for waktu_kerja
-- ----------------------------
DROP TABLE IF EXISTS "public"."waktu_kerja";
CREATE TABLE "public"."waktu_kerja" (
  "id" int8 NOT NULL DEFAULT nextval('waktu_kerja_id_seq'::regclass),
  "aktif" bool,
  "created_at" timestamp(6),
  "tanggal_mulai" date NOT NULL,
  "tanggal_selesai" date,
  "updated_at" timestamp(6),
  "shift_id" int8 NOT NULL,
  "user_id" int8 NOT NULL
)
;

-- ----------------------------
-- Records of waktu_kerja
-- ----------------------------
INSERT INTO "public"."waktu_kerja" VALUES (1, 't', '2026-06-12 12:05:04.136556', '2026-01-01', NULL, '2026-06-12 12:05:04.136556', 1, 3);

-- ----------------------------
-- Table structure for waktu_kerja_hari
-- ----------------------------
DROP TABLE IF EXISTS "public"."waktu_kerja_hari";
CREATE TABLE "public"."waktu_kerja_hari" (
  "waktu_kerja_id" int8 NOT NULL,
  "hari" varchar(255) COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Records of waktu_kerja_hari
-- ----------------------------
INSERT INTO "public"."waktu_kerja_hari" VALUES (1, 'FRIDAY');
INSERT INTO "public"."waktu_kerja_hari" VALUES (1, 'TUESDAY');
INSERT INTO "public"."waktu_kerja_hari" VALUES (1, 'WEDNESDAY');
INSERT INTO "public"."waktu_kerja_hari" VALUES (1, 'MONDAY');
INSERT INTO "public"."waktu_kerja_hari" VALUES (1, 'THURSDAY');

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."absen_masuk_id_seq"
OWNED BY "public"."absen_masuk"."id";
SELECT setval('"public"."absen_masuk_id_seq"', 3, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."absen_pulang_id_seq"
OWNED BY "public"."absen_pulang"."id";
SELECT setval('"public"."absen_pulang_id_seq"', 2, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."opd_id_seq"
OWNED BY "public"."opd"."id";
SELECT setval('"public"."opd_id_seq"', 2, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."shift_id_seq"
OWNED BY "public"."shift"."id";
SELECT setval('"public"."shift_id_seq"', 2, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."users_id_seq"
OWNED BY "public"."users"."id";
SELECT setval('"public"."users_id_seq"', 3, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."waktu_kerja_id_seq"
OWNED BY "public"."waktu_kerja"."id";
SELECT setval('"public"."waktu_kerja_id_seq"', 1, true);

-- ----------------------------
-- Indexes structure for table absen_masuk
-- ----------------------------
CREATE INDEX "idx_absen_masuk_opd" ON "public"."absen_masuk" USING btree (
  "opd_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_absen_masuk_tanggal" ON "public"."absen_masuk" USING btree (
  "tanggal" "pg_catalog"."date_ops" ASC NULLS LAST
);
CREATE INDEX "idx_absen_masuk_user_tanggal" ON "public"."absen_masuk" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
  "tanggal" "pg_catalog"."date_ops" ASC NULLS LAST
);

-- ----------------------------
-- Checks structure for table absen_masuk
-- ----------------------------
ALTER TABLE "public"."absen_masuk" ADD CONSTRAINT "absen_masuk_status_check" CHECK (status::text = ANY (ARRAY['HADIR'::character varying, 'TERLAMBAT'::character varying, 'PULANG_AWAL'::character varying, 'IZIN'::character varying, 'SAKIT'::character varying, 'ALPA'::character varying]::text[]));

-- ----------------------------
-- Primary Key structure for table absen_masuk
-- ----------------------------
ALTER TABLE "public"."absen_masuk" ADD CONSTRAINT "absen_masuk_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table absen_pulang
-- ----------------------------
CREATE INDEX "idx_absen_pulang_absen_masuk" ON "public"."absen_pulang" USING btree (
  "absen_masuk_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_absen_pulang_tanggal" ON "public"."absen_pulang" USING btree (
  "tanggal" "pg_catalog"."date_ops" ASC NULLS LAST
);
CREATE INDEX "idx_absen_pulang_user_tanggal" ON "public"."absen_pulang" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
  "tanggal" "pg_catalog"."date_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table absen_pulang
-- ----------------------------
ALTER TABLE "public"."absen_pulang" ADD CONSTRAINT "uk_nb9sl6n0e3iab5fujr3ti9yqh" UNIQUE ("absen_masuk_id");

-- ----------------------------
-- Checks structure for table absen_pulang
-- ----------------------------
ALTER TABLE "public"."absen_pulang" ADD CONSTRAINT "absen_pulang_status_check" CHECK (status::text = ANY (ARRAY['HADIR'::character varying, 'TERLAMBAT'::character varying, 'PULANG_AWAL'::character varying, 'IZIN'::character varying, 'SAKIT'::character varying, 'ALPA'::character varying]::text[]));

-- ----------------------------
-- Primary Key structure for table absen_pulang
-- ----------------------------
ALTER TABLE "public"."absen_pulang" ADD CONSTRAINT "absen_pulang_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table opd
-- ----------------------------
ALTER TABLE "public"."opd" ADD CONSTRAINT "uk_s6lan2af3tepg89b1hj8fcoyn" UNIQUE ("kode");

-- ----------------------------
-- Primary Key structure for table opd
-- ----------------------------
ALTER TABLE "public"."opd" ADD CONSTRAINT "opd_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table shift
-- ----------------------------
ALTER TABLE "public"."shift" ADD CONSTRAINT "shift_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table users
-- ----------------------------
CREATE INDEX "idx_user_nip" ON "public"."users" USING btree (
  "nip" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_user_opd" ON "public"."users" USING btree (
  "opd_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_user_username" ON "public"."users" USING btree (
  "username" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table users
-- ----------------------------
ALTER TABLE "public"."users" ADD CONSTRAINT "uk_e72fwutcg2xou2qg41w9bn5ed" UNIQUE ("nip");
ALTER TABLE "public"."users" ADD CONSTRAINT "uk_r43af9ap4edm43mmtq01oddj6" UNIQUE ("username");

-- ----------------------------
-- Checks structure for table users
-- ----------------------------
ALTER TABLE "public"."users" ADD CONSTRAINT "users_role_check" CHECK (role::text = ANY (ARRAY['ROLE_ADMIN'::character varying, 'ROLE_USER'::character varying, 'ROLE_PIMPINAN'::character varying]::text[]));

-- ----------------------------
-- Primary Key structure for table users
-- ----------------------------
ALTER TABLE "public"."users" ADD CONSTRAINT "users_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table waktu_kerja
-- ----------------------------
ALTER TABLE "public"."waktu_kerja" ADD CONSTRAINT "waktu_kerja_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Checks structure for table waktu_kerja_hari
-- ----------------------------
ALTER TABLE "public"."waktu_kerja_hari" ADD CONSTRAINT "waktu_kerja_hari_hari_check" CHECK (hari::text = ANY (ARRAY['MONDAY'::character varying, 'TUESDAY'::character varying, 'WEDNESDAY'::character varying, 'THURSDAY'::character varying, 'FRIDAY'::character varying, 'SATURDAY'::character varying, 'SUNDAY'::character varying]::text[]));

-- ----------------------------
-- Foreign Keys structure for table absen_masuk
-- ----------------------------
ALTER TABLE "public"."absen_masuk" ADD CONSTRAINT "fk5r2bkl6mus43vbphtbo7l2sdd" FOREIGN KEY ("user_id") REFERENCES "public"."users" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."absen_masuk" ADD CONSTRAINT "fkdjveb1deiflt4b5pphic67r8v" FOREIGN KEY ("shift_id") REFERENCES "public"."shift" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."absen_masuk" ADD CONSTRAINT "fkgmvg1753lah1i67i3eapuw1ey" FOREIGN KEY ("opd_id") REFERENCES "public"."opd" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table absen_pulang
-- ----------------------------
ALTER TABLE "public"."absen_pulang" ADD CONSTRAINT "fk22c0ovfx3t8ldlih42b9wfho6" FOREIGN KEY ("user_id") REFERENCES "public"."users" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."absen_pulang" ADD CONSTRAINT "fk29lej85por6tks35uj5xonwtf" FOREIGN KEY ("opd_id") REFERENCES "public"."opd" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."absen_pulang" ADD CONSTRAINT "fk5j9s92kx6af8vanx0vsj6qo6j" FOREIGN KEY ("shift_id") REFERENCES "public"."shift" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."absen_pulang" ADD CONSTRAINT "fkprw93lfqv8s3kxvyyf9anb17h" FOREIGN KEY ("absen_masuk_id") REFERENCES "public"."absen_masuk" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table shift
-- ----------------------------
ALTER TABLE "public"."shift" ADD CONSTRAINT "fks2pv3c7ub766oofnpxfwnwht6" FOREIGN KEY ("opd_id") REFERENCES "public"."opd" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table users
-- ----------------------------
ALTER TABLE "public"."users" ADD CONSTRAINT "fkt16e1vf4dfypg20vp7g0k39m5" FOREIGN KEY ("opd_id") REFERENCES "public"."opd" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table waktu_kerja
-- ----------------------------
ALTER TABLE "public"."waktu_kerja" ADD CONSTRAINT "fkm6w0n30i0frtx10kk2nk9cnj4" FOREIGN KEY ("user_id") REFERENCES "public"."users" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."waktu_kerja" ADD CONSTRAINT "fknqr1b9333jtx0m3qo1edvjq4w" FOREIGN KEY ("shift_id") REFERENCES "public"."shift" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table waktu_kerja_hari
-- ----------------------------
ALTER TABLE "public"."waktu_kerja_hari" ADD CONSTRAINT "fkte6hu2mlr9wtms50y097boyvp" FOREIGN KEY ("waktu_kerja_id") REFERENCES "public"."waktu_kerja" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;
