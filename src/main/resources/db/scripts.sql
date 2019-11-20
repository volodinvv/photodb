
alter table photos add column comment text

CREATE TABLE "equipments"(equipment TEXT,alias TEXT);

INSERT INTO equipments (equipment, alias) VALUES('6303 classic', 'Nokia6303');
INSERT INTO equipments (equipment, alias) VALUES('Canon EOS 350D DIGITAL', 'camera');
INSERT INTO equipments ( equipment, alias) VALUES('Canon PowerShot A400', 'camera');
INSERT INTO equipments (equipment, alias) VALUES('Canon PowerShot A620', 'CanonA620');
INSERT INTO equipments (equipment, alias) VALUES('Canon PowerShot A630', 'camera');
INSERT INTO equipments (equipment, alias) VALUES('Canon PowerShot SX10 IS', 'CanonSX10');
INSERT INTO equipments (equipment, alias) VALUES('Canon PowerShot SX200 IS', 'camera');
INSERT INTO equipments (equipment, alias) VALUES('Canon PowerShot SX230 HS', 'CanonSX230');
INSERT INTO equipments (equipment, alias) VALUES('DiMAGE S414', 'camera');
INSERT INTO equipments (equipment, alias) VALUES('E-400           ', 'camera');
INSERT INTO equipments (equipment, alias) VALUES('E-M10MarkII     ', 'OlimpusM10');
INSERT INTO equipments (equipment, alias) VALUES('HP PhotoSmart C945 (V01.60) ', 'camera');
INSERT INTO equipments (equipment, alias) VALUES('HTC One S', 'HTCOnesS');
INSERT INTO equipments (equipment, alias) VALUES('KODAK EASYSHARE C743 ZOOM DIGITAL CAMERA', 'camera');
INSERT INTO equipments (equipment, alias) VALUES('NIKON D3', 'camera');
INSERT INTO equipments (equipment, alias) VALUES('NIKON D70s', 'camera');
INSERT INTO equipments (equipment, alias) VALUES('SLT-A33', 'camera');
INSERT INTO equipments (equipment, alias) VALUES('SM-A520F', 'SamsungA5');
INSERT INTO equipments (equipment, alias) VALUES('unknown', 'unknown');

select * from photos
