# project 2





## data

### coursePrerequisites :

| 内容                    | ex                   |
| ----------------------- | -------------------- |
| courseId                | "MSE305"             |
| prerequisite            |                      |
| - And / Or Prerequisite | 表示先修课之间的关系 |
| - terms                 |                      |

- courseId : 对应的课程编号
- prerequisite : 
  -  And / Or Prerequisite : 表示先修课之间的关系
  - CoursePrerequisite - CourseId : 对应的先修课的编号

![image-20211202204736169](https://github.com/Cateatsthatfish/Database-Project2/tree/main/Stage1_getStarted/pic/image-20211202204736169.png) 



### courses

|           |                                              |
| --------- | -------------------------------------------- |
| @type     | 不知道除了Course还有没有其他的               |
| id        | 课程编号                                     |
| name      | 中文名（不知道会不会需要处理一些奇怪的符号） |
| credit    | 不知道是不是都是整数的                       |
| classHour | 总课时                                       |
| grading   | 百分制度和PF制度 enum                        |

![image-20211202204805057](https://github.com/Cateatsthatfish/Database-Project2/tree/main/Stage1_getStarted/pic/image-20211202204805057.png) 



> The relationship between {@code CourseSectionClass} with {@code CourseSection} is: One CourseSection usually has two CourseSectionClass the one is theory class, the other is lab class
>
> -  CourseSectionClasses in same courseSection may have different week list.



### courseSection

- courseId : 课程编号
  - 1/2/3 : 现在怀疑是semester的编号？
    - @type : CourseSection  （有没有其他的？）
    - id ：不知道是什么的id （应该是class的编号）
    - name ： 和courses里面的name不一样，是class的名字
    - totalCapacity : 总课程
    - leftCapacity : 剩余课程（可能后面做选课的时候能用到吧）

![image-20211202205015703](https://github.com/Cateatsthatfish/Database-Project2/tree/main/Stage1_getStarted/pic/image-20211202205015703.png) 

![image-20211202212901171](https://github.com/Cateatsthatfish/Database-Project2/tree/main/Stage1_getStarted/pic/image-20211202212901171.png) 





### courseSectionClasses

- 172 : courseSectionId ? 
- 170 : courseSectionClassId ? 
  - instructor : 老师
  - dayOfWeek 
  - weekList
  - classBegin : 开始的节次
  - classEnd : 结束的节次
  - location : 地点

![image-20211202204914036](https://github.com/Cateatsthatfish/Database-Project2/tree/main/Stage1_getStarted/pic/image-20211202204914036.png)  



### departments

- id : 开课部门的id （也可以说是系）
- name ： 开课部门的名称

![image-20211202205035297](https://github.com/Cateatsthatfish/Database-Project2/tree/main/Stage1_getStarted/pic/image-20211202205035297.png) 



### majorCompulsoryCourses

- majorId : 前面的数字
- compulsory 对应的必修课程编号

![image-20211202205114757](https://github.com/Cateatsthatfish/Database-Project2/tree/main/Stage1_getStarted/pic/image-20211202205114757.png) 

### majorElectiveCourses

- majorId : 前面的数字
- elective 对应的选修课程

![image-20211202205139668](https://github.com/Cateatsthatfish/Database-Project2/tree/main/Stage1_getStarted/pic/image-20211202205139668.png) 

### majors

- @type ： Major具体的专业
- id：应该是专业的id
- name：专业的id
- department : 专业在哪个系下面

![image-20211202205219590](https://github.com/Cateatsthatfish/Database-Project2/tree/main/Stage1_getStarted/pic/image-20211202205219590.png) 

### semesters

- id：只有三个学期 时间似乎是 2-1-3
- Name ： 学期的名称
- begin / end : 应该是时间的换算？？？？

![image-20211202205248623](https://github.com/Cateatsthatfish/Database-Project2/tree/main/Stage1_getStarted/pic/image-20211202205248623.png) 

### users

- @type : 似乎只有Student和Instructor两类

- id : 学号/教工号， 不知道会不会有重复的？

- fullName：全名（在user里面似乎讲了分开的方法），但是学生的全部是RANDOM的样子

- enrolledDate : 就读时间？？？有点奇怪？比如下面那个是18-19-3的endDate的样子，可能是用来判断学生能否选课？比如在如果enrolledDate的时间比开课的那个学期晚，那么就不不能选课？

- major : 应该是这个学生的专业。扫了一眼几乎几乎全部只有Id，没有name和department(重复信息)

  

![image-20211202205336727](https://github.com/Cateatsthatfish/Database-Project2/tree/main/Stage1_getStarted/pic/image-20211202205336727.png) 

![image-20211202205423773](https://github.com/Cateatsthatfish/Database-Project2/tree/main/Stage1_getStarted/pic/image-20211202205423773.png) 





### studentCourses.json

![image-20211202205542946](https://github.com/Cateatsthatfish/Database-Project2/tree/main/Stage1_getStarted/pic/image-20211202205542946.png)  





## 前人的智慧

> 没有read me 简直杀我

[开心](https://github.com/vilsmeier/CS307_SUSTech/blob/main/Requirements.md) ： 有一个数据库的设计表

https://github.com/vilsmeier/CS307_SUSTech/blob/main/project2%E8%AE%BE%E8%AE%A1.pdf 

python ： [Zhouyicheng](https://github.com/Zhou-Yicheng/CS307-Project2) 

java : https://github.com/YG-yyds/CS307_project2



 
