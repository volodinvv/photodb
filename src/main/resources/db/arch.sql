--- Check md5 and size
select md5 FROM photos_arch p group by md5 having max(size)<>min(size)

-- zero size!!!
select folder, count(*) from photos_arch p where "size"=0 GROUP by folder

------------------------------ Not empty --------------------------------

-- comments
select comment, count(*) FROM photos_arch  p GROUP by comment


---- !!!!!-------
select * from photos_arch pa left join photos p using(md5) where p."path" is null;
select * from photos_arch pa left join photos p using(md5) where p.name <> pa.name;

SELECT ext, created is not null, count(*) from photos_arch pa GROUP by ext, created is not null
