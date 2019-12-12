--- Check md5 and size
select md5 FROM photos p group by md5 having max(size)<>min(size)

-- zero size!!!
select folder, count(*) from photos p where "size"=0 GROUP by folder

------------------------------ Not empty --------------------------------

-- comments
select comment, count(*) FROM photos p GROUP by comment


------------------------------ Not OK -----------------------------------

-- same md5 but different names
select * from photos p where md5 in(select md5 FROM photos p where "size"<>0 group by md5 having max(name)<>min(name))

--- created is wrong or NULL
select folder, count(*), max(created), MIN(created) from photos p where created < '2004-01-01 00:00:00' GROUP by folder

select folder, count(*) from photos p where created is null GROUP by folder

-- equal name and date - different md5
select name, substr(created, 1, 10), equipment from photos p group by name, substr(created, 1, 10), equipment HAVING max(md5)<>Min(md5)

select * from photos p WHERE name='IMG_0373.JPG'

---- equipmnets

select equipment, count(*), sum(size) from photos p where 1=1 group by equipment order by 2

update photos set comment='xxx' where equipment='Canon PowerShot A630' and comment is null
select * from photos p where equipment='NIKON D3' ---?????
select * from photos p where equipment='Canon PowerShot SX200 IS' --?????


-------------------------------- In progress -------------------------------


select folder FROM photos p where comment is null GROUP by folder

2011\12 New year\170CANON
2011\12 New year\171CANON
2011\12 New year\172CANON
2014\Kinezis stars 2014
Фото\2005\20050413 DR Marini restoran
Фото\2014\Kinezis stars 2014
Фото\2017\100OLYMP-berlin
Фото\2019\A5\YiCamera\481910


UPDATE photos set comment='Berlin' where folder like'%berlin%'

select * from photos where md5 in(
select md5 from photos p GROUP BY md5 HAVING max (comment)<>min(comment))


