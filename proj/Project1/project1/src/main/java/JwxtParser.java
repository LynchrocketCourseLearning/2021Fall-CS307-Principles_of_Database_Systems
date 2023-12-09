import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;
import java.sql.*;
// TODO: import the json library of your choice

public class JwxtParser {
    private static final String pathToJSONFile = "src\\main\\java\\data\\course_info.json";
    private static final String pathToCSVFile = "src\\main\\java\\data\\select_course.csv";

    private static String url = "jdbc:postgresql://localhost:5432/Project1";
    private static String user = "postgres";
    private static String pwd = "u5398681234QWer";

    public static List<Course_first> courses;//预先放入courses

    static Map<String, Course> courseHashMap;//course_id到course的map，从courses去重
    static ArrayList<c_Class> classes;
    static HashSet<Teacher> teachers;
    static Map<String, Teacher> teacherHashMap;

    public static void main(String[] args) throws IOException {
        dataFromJSON();
        parseCourse();
        dataToDB();
    }

    public static void dataFromJSON() throws IOException {
        String content = Files.readString(Path.of(pathToJSONFile));
        content = content.replaceAll("）", ")");
        content = content.replaceAll("（", "(");
        content = content.replaceAll("或者", "or");
        content = content.replaceAll("并且", "and");
        // Gson is an example
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        courses = gson.fromJson(content, new TypeToken<List<Course_first>>() {
        }.getType());
    }

    public static void parseCourse() {
        classes = new ArrayList<>();
        teachers = new HashSet<>();
        courseHashMap = new HashMap<>();
        teacherHashMap = new HashMap<>();
        for (Course_first cou_f : courses) {
            if (!courseHashMap.containsKey(cou_f.courseId.trim())) {
                Course cou = new Course(cou_f);
                courseHashMap.put(cou.course_id, cou);
            }
            c_Class cla = new c_Class(cou_f.className.trim(), courseHashMap.get(cou_f.courseId.trim()));
            for (ClassList_first cl_f : cou_f.classList) {
                ClassList cl = new ClassList(cl_f);
                cla.classList.add(cl);
            }
            if (cou_f.teacher != null) {
                String[] tea = cou_f.teacher.split(",");
                for (String name : tea) {
                    Teacher teacher = new Teacher(name.trim());
                    teachers.add(teacher);
                    teacherHashMap.put(teacher.name, teacher);
                    cla.teachers.add(teacher);
                }
            }
            classes.add(cla);
        }

    }


    public static void dataToDB() {
        Connector connection = new Connector(url, user, pwd);

//        插入course
        long begin = System.currentTimeMillis();
        for (Course cou : courseHashMap.values()) {
            Connector.toDB(cou);
        }
        System.out.printf("course insertion: %d(ms)\n", System.currentTimeMillis() - begin);

        //插入teacher
        begin = System.currentTimeMillis();
        for (Teacher te : teachers) {
            Connector.toDB(te);
        }
        System.out.printf("teacher insertion: %d(ms)\n", System.currentTimeMillis() - begin);


        //插入class, course_class
        begin = System.currentTimeMillis();
        for (c_Class cla : classes) {
            Connector.toDB(cla);
            Connector.toDB(cla.course, cla);
            for (ClassList cl : cla.classList) {
                Connector.toDB(cl, cla);
            }
            Connector.toDB(cla, 0);
        }
        System.out.printf("course insertion: %d(ms)\n", System.currentTimeMillis() - begin);

        //插入student
        Connector.toDB(pathToCSVFile);
        //插入course_learned
        Connector.toDB(pathToCSVFile, 0);


        Connector.closeDB();
    }

}

//Course和ClassList都要处理
class Course_first {
    public int totalCapacity;
    public String courseId;
    public String prerequisite;
    public String teacher;
    public ClassList_first[] classList;
    public int courseHour;
    public float courseCredit;
    public String courseName;
    public String courseDept;
    public String className;
}

class ClassList_first {
    public int[] weekList;
    public String location;
    public String classTime;
    public int weekday;
}

class Course {
    // TODO:
    String course_id;
    int capacity;
    String course_name;
    int course_hour;
    String course_dept;
    float credit;
    String prerequisite;

    List<Teacher> teachers;

    List<c_Class> classes;

    public Course(String course_id, int capacity, String course_name,
                  int course_hour, float credit, String prerequisite,
                  String course_dept) {
        this.course_id = course_id.trim();
        this.capacity = capacity;
        this.course_name = course_name.trim();
        this.course_hour = course_hour;
        this.course_dept = course_dept.trim();
        this.credit = credit;
        this.prerequisite = prerequisite;
        this.classes = new ArrayList<>();
    }

    public Course(Course_first c) {
        this.course_id = c.courseId.trim();
        this.capacity = c.totalCapacity;
        this.course_name = c.courseName.trim();
        this.course_hour = c.courseHour;
        this.credit = c.courseCredit;
        this.course_dept = c.courseDept;
        this.classes = new ArrayList<>();
        this.prerequisite = c.prerequisite;
    }
}

//Class被用了。故用c_Class代指Class
class c_Class {
    String class_id;
    String class_name;

    List<ClassList> classList;
    Course course;
    List<Teacher> teachers;

    public c_Class(String class_name, Course course) {
        this.class_id = course.course_id + class_name;
        this.class_name = class_name;
        this.course = course;
        this.classList = new ArrayList<>();
        this.teachers = new ArrayList<>();
        this.course.classes.add(this);
    }
}

//Class的一个属性
class ClassList {
    // TODO: define data-class as the json structure
    String week_list;
    String location;
    int start_time;
    int end_time;
    int weekday;

    public ClassList(int[] week_list, String location, String classTime, int weekday) {
        this.location = location;
        this.weekday = weekday;
        String[] class_time = classTime.split("-");
        this.start_time = Integer.parseInt(class_time[0]);
        this.end_time = Integer.parseInt(class_time[1]);
        StringBuffer sb = new StringBuffer();
        int len = week_list.length;
        for (int i = 0; i < len; i++) {
            sb.append(week_list[i]).append(" ");
        }
        this.week_list = sb.toString();
    }

    public ClassList(ClassList_first c) {
        this.location = c.location;
        this.weekday = c.weekday;
        String[] ct = c.classTime.split("-");
        this.start_time = Integer.parseInt(ct[0]);
        this.end_time = Integer.parseInt(ct[1]);
        StringBuffer sb = new StringBuffer();
        int len = c.weekList.length;
        for (int i = 0; i < len; i++) {
            sb.append(c.weekList[i]).append(" ");
        }
        this.week_list = sb.toString();
    }
}

class Teacher {
    int teacher_id;//数据库自动生成
    String name;

    public Teacher(int teacher_id, String name) {
        this.teacher_id = teacher_id;
        this.name = name;
    }

    public Teacher(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        Teacher t = null;
        try {
            t = (Teacher) obj;
            if (t.teacher_id != this.teacher_id) return false;
            else return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}

class Student {
    String student_id;
    String name;
    String gender;
    String college;
    List<String> course_learned;

    public Student(String student_id, String name, String gender, String college) {
        this.student_id = student_id;
        this.name = name;
        this.gender = gender;
        this.college = college;
        this.course_learned = new ArrayList<>();
    }

    public Student(String[] message) {
//按csv文件顺序
        this.name = message[0].trim();
        this.gender = message[1].trim();
        this.college = message[2].trim();
        this.student_id = message[3].trim();
        this.course_learned = new ArrayList<>();
        int len = message.length;
        for (int i = 4; i < len; i++) {
            this.course_learned.add(message[i].trim());
        }
    }

    public boolean equals(Object obj) {
        if (obj == null) return false;
        Student t = null;
        try {
            t = (Student) obj;
            if (!t.student_id.equals(this.student_id)) return false;
            else return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}


