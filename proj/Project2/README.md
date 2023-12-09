# Fall 2021 CS307 Database Project-2

[TOC]

## 1. Title

Educational Administration System --ADVANCE

## 2. Group members and contribution

|  name  |   SID    |                       work                        | percentage |
| :----: | :------: | :-----------------------------------------------: | :--------: |
| 谢子晟 | 12011919 |       database design, prerequisite design        |   33.3%    |
| 简欣瑶 | 11911838 |      database design, most `service` design       |   33.3%    |
| 刘乐奇 | 12011327 | database design, user, student, instructor design |   33.3%    |

## 3. Database design 

### 3.1 The structure of whole database

![db_stru.png](https://github.com/Cateatsthatfish/Database-Project2/blob/main/pic/db_stru.png)

### 3.2 Description of database

​	The specific codes can be seen in the attachments. All the foreign keys are ` on delete cascade on update cascade `.

#### 3.2.1 tables

##### courses

| column's name |  data type  |          description          |
| :-----------: | :---------: | :---------------------------: |
|      id       | varchar(45) |          primary key          |
|     name      |    text     |           not null            |
|    credit     |     int     |     not null, check >= 0      |
|   classhour   |     int     |      not null, check >=0      |
|  grade_type   |   char(1)   | not null, check in ('P', 'H') |

##### semesters

| column's name |  data type  | description |
| :-----------: | :---------: | :---------: |
|      id       |   serial    | primary key |
|     name      | varchar(45) |  not null   |
|  begin_date   |    date     |  not null   |
|   end_date    |    date     |  not null   |

##### sections

| column's name  |  data type  |              description              |
| :------------: | :---------: | :-----------------------------------: |
|       id       |   serial    |              primary key              |
|      name      |   varchar   |                                       |
| total_capacity |     int     |                                       |
|   course_id    | varchar(45) |  foreign key, reference courses (id)  |
|  semester_id   |     int     | foreign key, reference semesters (id) |

##### classes

| column's name | data type |             description              |
| :-----------: | :-------: | :----------------------------------: |
|      id       |  serial   |             primary key              |
|   dayofweek   | smallint  |        check between 1 and 7         |
|   weeklist    | integer[] |                                      |
|  classbegin   | smallint  |        check between 1 and 12        |
|   classend    | smallint  |   check between classbegin and 12    |
|   location    |  varchar  |                                      |
|  section_id   |    int    | foreign key, reference sections (id) |

##### prerequisite

|  column's name  |  data type  |             description             |
| :-------------: | :---------: | :---------------------------------: |
|       id        |   serial    |             primary key             |
|    course_id    | varchar(45) | foreign key, reference courses (id) |
| prerequisite_id |     int     | foreign key, reference courses (id) |
|    group_id     |     int     |              not null               |

##### departments

| column's name | data type | description |
| :-----------: | :-------: | :---------: |
|      id       |  serial   | primary key |
|     name      |  varchar  |   unique    |

##### majors

| column's name | data type |               description               |
| :-----------: | :-------: | :-------------------------------------: |
|      id       |  serial   |               primary key               |
|     name      |  varchar  |                                         |
|    dept_id    |    int    | foreign key, reference departments (id) |

##### instructors

| column's name | data type | description |
| :-----------: | :-------: | :---------: |
|      id       |    int    | primary key |
|   firstname   |  varchar  |             |
|   lastname    |  varchar  |             |

##### students

| column's name | data type |            description             |
| :-----------: | :-------: | :--------------------------------: |
|      id       |    int    |            primary key             |
|   firstname   |  varchar  |                                    |
|   lastname    |  varchar  |                                    |
| enrolleddate  |   date    |                                    |
|   major_id    |    int    | foreign key, reference majors (id) |

##### select_course

| column's name | data type |                    description                    |
| :-----------: | :-------: | :-----------------------------------------------: |
|  student_id   |    int    | primary key, foreign key, reference students (id) |
|  section_id   |    int    | primary key, foreign key, reference sections (id) |

##### class_instructor

| column's name | data type |                     description                      |
| :-----------: | :-------: | :--------------------------------------------------: |
|   class_id    |    int    |   primary key, foreign key, reference classes (id)   |
| instructor_id |    int    | primary key, foreign key, reference instructors (id) |

##### student_grades_pf

| column's name | data type |                    description                    |
| :-----------: | :-------: | :-----------------------------------------------: |
|  student_id   |    int    | primary key, foreign key, reference students (id) |
|  section_id   |    int    | primary key, foreign key, reference sections (id) |
|     grade     |  char(4)  |             check in ('PASS', 'FAIL')             |

##### student_grades_hundred

| column's name | data type |                    description                    |
| :-----------: | :-------: | :-----------------------------------------------: |
|  student_id   |    int    | primary key, foreign key, reference students (id) |
|  section_id   |    int    | primary key, foreign key, reference sections (id) |
|     grade     | smallint  |              check between 0 and 100              |

##### major_course

| column's name |  data type  |                   description                    |
| :-----------: | :---------: | :----------------------------------------------: |
|   major_id    |     int     | primary key, foreign key, reference majors (id)  |
|   course_id   | varchar(45) | primary key, foreign key, reference courses (id) |
|     type      |   char(1)   |      default 'A', check in ('A', 'C', 'E')       |

#### 3.2.2 functions

##### getfullname

![func.png](https://github.com/Cateatsthatfish/Database-Project2/blob/main/pic/func.png)
