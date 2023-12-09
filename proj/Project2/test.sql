
insert into courses (id, name, credit, classhour, grade_type) VALUES ('MA333', 'dashuju', 2, 12, 'P');
insert into semesters (id, name, begin_date, end_date) VALUES (1, 'haru', cast('1999-12-10' as date), cast('2001-12-10' as date));
insert into sections (name, totalcapacity, course_id, semester_id) VALUES ('nihao', 90, 'MA333', 1);
insert into departments(id, name) VALUES (1,'cs');
insert into majors (id, name, dept_id) VALUES (1,'MA',1);
insert into students (id, firstname, lastname, enrolleddate, major_id) VALUES (12011327,'liu','leqi',cast('1999-12-1' as date), 1);
insert into students (id, firstname, lastname, enrolleddate, major_id) VALUES (12011328,'l','lqi',cast('1999-12-1' as date), 1);
insert into instructors(id, firstname, lastname) VALUES (1,'s','a');
insert into classes(id, dayofweek, weeklist, classbegin, classend, location, section_id) VALUES (1,1,'{1,2}',8,10,'a',3);
insert into class_instructor (class_id, instructor_id) values (1,1);
insert into select_course (student_id, section_id) VALUES (12011327, 3);
insert into select_course (student_id, section_id) VALUES (12011328, 3);
