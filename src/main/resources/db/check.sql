--- Check md5 and size
select md5 FROM photos p group by md5 having max(size)<>min(size)

-- same md5 but different names
select * from photos p where md5 in(
select md5 FROM photos p where "size"<>0 group by md5 having max(name)<>min(name))

-- zero size!!!
select folder, count(*) from photos p where "size"=0 GROUP by folder

--- created
select folder, count(*) from photos p where created is null or created < '2004-01-01 00:00:00' GROUP by folder

select folder, count(*) from photos p where created is null GROUP by folder

select * from photos p where equipment is null 


-- comments
select comment, count(*) FROM photos p GROUP by comment

--update photos set comment=null where comment in ('Фото','фото т1','т2', 'Новая папка', 'Новая папка (2)','февраль-апрель', 'май','декабрь_январь','июнь','декабрь','январь','копия','сентябрь2010-2')

--update photos set comment='1 Год!' WHERE comment='Год!'

select * from photos p where folder='2008\2008_03_02'