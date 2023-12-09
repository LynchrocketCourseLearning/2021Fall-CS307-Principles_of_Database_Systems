-- create tables --
create table if not exists courses
(
    id         varchar(45) primary key,
    name       text    not null,
    credit     int     not null check ( credit >= 0 ),
    classHour  int     not null check ( classHour >= 0),
    grade_type char(1) not null check ( grade_type in ('P', 'H')),
    max_group  int     not null check (max_group >= 0)
);

create table if not exists semesters
(
    id         serial primary key,
    name       varchar(45) not null,
    begin_date date        not null,
    end_date   date        not null
);

-- truncate table  semesters RESTART IDENTITY;

create table if not exists sections
(
    id            serial primary key,
    name          varchar,
    totalCapacity int,
--     leftCapacity  int,
    course_id     varchar(45) references courses (id) on delete cascade on update cascade,
    semester_id   int references semesters (id) on delete cascade on update cascade
);

create table if not exists classes
(
    id         serial primary key,
    dayOfWeek  smallint check (dayOfWeek between 1 and 7),
    weekList   integer[],
    classBegin smallint check (classBegin between 1 and 12),
    classEnd   smallint check (classEnd between classBegin and 12),
    location   varchar,
    section_id int references sections (id) on delete cascade on update cascade
);

create table if not exists prerequisite
(
    id              serial primary key,
    course_id       varchar(45) references courses on delete cascade on update cascade,
    prerequisite_id varchar(45) references courses on delete cascade on update cascade,
    group_id        int not null
);

create table if not exists departments
(
    id   serial primary key,
    name varchar unique
);

create table if not exists majors
(
    id      serial primary key,
    name    varchar,
    dept_id int references departments (id) on delete cascade on update cascade
);

create table if not exists instructors
(
    id        int primary key,
    firstname varchar,
    lastname  varchar
);

create table if not exists students
(
    id           int primary key,
    firstname    varchar,
    lastname     varchar,
    enrolledDate date,
    major_id     int references majors (id) on delete cascade on update cascade
);

-- links --
create table if not exists select_course
(
    student_id int references students (id) on delete cascade on update cascade,
    section_id int references sections (id) on delete cascade on update cascade,
    constraint pk_sc primary key (student_id, section_id)
);

create table if not exists class_instructor
(
    class_id      int references classes (id) on delete cascade on update cascade,
    instructor_id int references instructors (id) on delete cascade on update cascade,
    constraint pk_ci primary key (class_id, instructor_id)
);

create table if not exists student_grades_PF
(
    student_id int references students (id) on delete cascade on update cascade,
    section_id int references sections (id) on delete cascade on update cascade,
    grade      char(4) check ( grade in ('PASS', 'FAIL') ),
    constraint uq_sgp unique (student_id, section_id)
);

create table if not exists student_grades_hundred
(
    student_id int references students (id) on delete cascade on update cascade,
    section_id int references sections (id) on delete cascade on update cascade,
    grade      smallint check ( grade between 0 and 100 ),
    constraint uq_sgh unique (student_id, section_id)
);

create table if not exists major_course
(
    major_id  int references majors (id) on delete cascade on update cascade,
    course_id varchar(45) references courses (id) on delete cascade on update cascade,
    type      char(1) default 'A' check (type in ('A', 'C', 'E') ),
    constraint pk_mc primary key (major_id, course_id, type)
);

create or replace function getfullname(firstname varchar, lastname varchar) returns varchar
as
$$
begin
    if (firstname ~ '[a-zA-z]' and lastName ~ '[a-zA-z]') then
        return firstname || ' ' || lastname;
    else
        return firstname || lastname;
    end if;
end
$$ language plpgsql;
