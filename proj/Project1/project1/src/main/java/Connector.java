import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.sql.*;
import java.net.URL;

public class Connector {
    private static final int BATCH_SIZE = 1000;

    private static Connection con = null;
    private static PreparedStatement stmt = null;

//    String url = "jdbc:postgresql://localhost:5432/Project1";
//    String user = "postgres";
//    String pwd = "u5398681234QWer";

    public Connector(String url, String user, String pwd) {
        try {
            //
            Class.forName("org.postgresql.Driver");
        } catch (Exception e) {
            System.err.println("Cannot find the Postgres driver. Check CLASSPATH.");
            System.exit(1);
        }

        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", pwd);

        try {
            con = DriverManager.getConnection(url, props);
            System.out.println("Connection succeeded");
            String sql = "set search_path to \"public\"";
            stmt = con.prepareStatement(sql);
            stmt.execute();
            con.setAutoCommit(false);
        } catch (SQLException e) {
            System.err.println("Connection failed");
            e.printStackTrace();
            System.exit(1);
        }

    }

    public static void closeDB() {
        if (con != null) {
            try {
                con.commit();
                if (stmt != null) {
                    stmt.close();
                    System.out.println("Disconnection succeeded");
                }
                con.close();
                con = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //main tables
    public static void toDB(Map<String, Course> courses) {
        long begin = System.currentTimeMillis();
        if (con != null) {
            String sql = "insert into course (course_id, capacity, course_name, course_hour, course_dept, course_credit, prerequisite) " +
                    "values (?,?,?,?,?,?,?) on conflict do nothing";
            try {
                long cnt = 0;
                for (Course course : courses.values()) {
                    cnt++;
                    stmt = con.prepareStatement(sql);

                    stmt.setString(1, course.course_id);
                    stmt.setInt(2, course.capacity);
                    stmt.setString(3, course.course_name);
                    stmt.setInt(4, course.course_hour);
                    stmt.setString(5, course.course_dept);
                    stmt.setFloat(6, course.credit);
                    stmt.setString(7, course.prerequisite);
                    if (cnt % BATCH_SIZE == 0) {
                        stmt.executeBatch();
                        con.commit();
                        stmt.clearBatch();
                    }
                }
                if (cnt % BATCH_SIZE != 0) {
                    stmt.executeBatch();
                    con.commit();
                    stmt.clearBatch();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                System.out.printf("course insertion: %d(ms)\n", System.currentTimeMillis() - begin);
            }
        }
    }

    public static void toDB(Course course) {
        if (con != null) {
            String sql = "insert into course (course_id, capacity, course_name, course_hour, course_dept, course_credit, prerequisite) " +
                    "values (?,?,?,?,?,?,?) on conflict do nothing";
            try {
                stmt = con.prepareStatement(sql);

                stmt.setString(1, course.course_id);
                stmt.setInt(2, course.capacity);
                stmt.setString(3, course.course_name);
                stmt.setInt(4, course.course_hour);
                stmt.setString(5, course.course_dept);
                stmt.setFloat(6, course.credit);
                stmt.setString(7, course.prerequisite);

                stmt.execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void toDB(c_Class c_class) {
        if (con != null) {
            String sql = "insert into class (class_id, class_name) values (?,?) on conflict do nothing";
            try {
                stmt = con.prepareStatement(sql);
                stmt.setString(1, c_class.class_id);
                stmt.setString(2, c_class.class_name);

                stmt.execute();

                con.commit();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void toDB(ClassList classList, c_Class c_class) {
        if (con != null) {
            String sql = "insert into class_list (class_id, week_list, location, start_time, end_time, weekday) " +
                    "values (?,?,?,?,?,?) on conflict do nothing";
            try {
                stmt = con.prepareStatement(sql);

                stmt.setString(1, c_class.class_id);
                stmt.setString(2, classList.week_list);
                stmt.setString(3, classList.location);
                stmt.setInt(4, classList.start_time);
                stmt.setInt(5, classList.end_time);
                stmt.setInt(6, classList.weekday);

                stmt.execute();

                con.commit();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void toDB(String pathToCSVFile) {
        BufferedReader re = null;
        long begin = System.currentTimeMillis();
        long cnt = 0;
        if (con != null) {
            String sql = "insert into student (student_id, student_name, gender, college) values (?,?,?,?) on conflict do nothing";
            try {
                re = new BufferedReader(new FileReader(pathToCSVFile));
                String line;
                String[] message;
                stmt = con.prepareStatement(sql);
                while ((line = re.readLine()) != null) {
                    message = line.split(",");
                    if (message.length > 1) {
                        stmt.setString(1, message[3].trim());
                        stmt.setString(2, message[0].trim());
                        stmt.setString(3, message[1].trim());
                        stmt.setString(4, message[2].trim());
                        stmt.addBatch();
                        cnt++;
                        if (cnt % BATCH_SIZE == 0) {
                            stmt.executeBatch();
                            stmt.clearBatch();
                        }
                    }
                }
                if (cnt % BATCH_SIZE != 0) {
                    stmt.executeBatch();
                    stmt.clearBatch();
                }
                con.commit();
                stmt.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (re != null) re.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                long time = System.currentTimeMillis() - begin;
                long speed = 1000 * (cnt / time);
                System.out.printf("student insertion: %d(ms)\n Loading speed: %d(records/s)\n", time, speed);
            }
        }
    }

    public static void toDB(String pathToCSVFile, int mode) {
        BufferedReader re = null;
        long begin = System.currentTimeMillis();
        long cnt = 0;
        if (con != null) {
            String sql = "insert into course_learned (student_id, course_id) values (?,?) on conflict do nothing";
            try {
                re = new BufferedReader(new FileReader(pathToCSVFile));
                String line;
                String[] message;
                stmt = con.prepareStatement(sql);
                while ((line = re.readLine()) != null) {
                    message = line.split(",");
                    if (message.length > 1) {
                        for (int i = 4; i < message.length; i++) {
                            cnt++;
                            stmt.setString(1, message[3].trim());
                            stmt.setString(2, message[i].trim());
                            stmt.addBatch();
                        }
                        if (cnt % BATCH_SIZE == 0) {
                            stmt.executeBatch();
                            stmt.clearBatch();
                        }
                    }
                }
                if (cnt % BATCH_SIZE != 0) {
                    stmt.executeBatch();
                    stmt.clearBatch();
                }
                con.commit();
                stmt.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (re != null) re.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                long time = System.currentTimeMillis() - begin;
                long speed = 1000 * (cnt / time);
                System.out.printf("student insertion: %d(ms)\n Loading speed: %d(records/s)\n", time, speed);
            }
        }
    }

    public static void toDB(ArrayList<Student> students) {
        long begin = System.currentTimeMillis();
        long cnt = 0;
        if (con != null) {
            String sql = "insert into student (student_id, student_name, gender, college) values (?,?,?,?) on conflict do nothing";
            try {
                for (Student student : students) {
                    cnt++;
                    stmt = con.prepareStatement(sql);

                    stmt.setString(1, student.student_id);
                    stmt.setString(2, student.name);
                    stmt.setString(3, student.gender);
                    stmt.setString(4, student.college);
                    stmt.addBatch();
                    if (cnt % BATCH_SIZE == 0) {
                        stmt.executeBatch();
                        stmt.clearBatch();
                    }
                }
                if (cnt % BATCH_SIZE != 0) {
                    stmt.executeBatch();
                    stmt.clearBatch();
                }
                con.commit();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                long time = System.currentTimeMillis() - begin;
                long speed = 1000 * (cnt / time);
                System.out.printf("course_learned insertion: %d(ms)\n Loading speed: %d(records/s)\n", time, speed);
            }
        }
    }

    public static void toDB(ArrayList<Student> students, int mode) {
        long begin = System.currentTimeMillis();
        if (con != null) {
            String sql = "insert into course_learned (student_id, course_id) values (?,?) on conflict do nothing";
            try {
                long cnt = 0;
                for (Student student : students) {
                    stmt = con.prepareStatement(sql);
                    for (String course_id : student.course_learned) {
                        cnt++;
                        stmt.setString(1, student.student_id);
                        stmt.setString(2, course_id);
                        stmt.addBatch();
                    }
                    if (cnt % BATCH_SIZE == 0) {
                        stmt.executeBatch();
                        con.commit();
                        stmt.clearBatch();
                    }
                }
                if (cnt % BATCH_SIZE != 0) {
                    stmt.executeBatch();
                    con.commit();
                    stmt.clearBatch();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                System.out.printf("course_learned insertion: %d(ms)\n", System.currentTimeMillis() - begin);
            }
        }
    }

    public static void toDB(Set<Teacher> teachers) {
        long begin = System.currentTimeMillis();
        if (con != null) {
            String sql = "insert into teacher (teacher_name) values (?) on conflict do nothing";
            try {
                long cnt = 0;
                for (Teacher teacher : teachers) {
                    cnt++;
                    stmt = con.prepareStatement(sql);
                    stmt.setString(1, teacher.name);
                    stmt.addBatch();
                    if (cnt % BATCH_SIZE == 0) {
                        stmt.executeBatch();
                        con.commit();
                        stmt.clearBatch();
                    }
                }
                if (cnt % BATCH_SIZE != 0) {
                    stmt.executeBatch();
                    con.commit();
                    stmt.clearBatch();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                System.out.printf("teacher insertion: %d(ms)\n", System.currentTimeMillis() - begin);
            }
        }
    }

    public static void toDB(Teacher teacher) {
        if (con != null) {
            String sql = "insert into teacher (teacher_name) values (?) on conflict do nothing";
            try {
                stmt = con.prepareStatement(sql);

                stmt.setString(1, teacher.name);

                stmt.execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static HashMap<String, Integer> teacherID;

    static void getTeacherID() {
        teacherID = new HashMap<>();
        String sql = "select teacher_id, teacher_name from teacher";
        try {
            Statement st = con.createStatement();
            ResultSet resultSet = st.executeQuery(sql);
            while (resultSet.next()) {
                int teacher_id = resultSet.getInt(1);
                String teacher_name = resultSet.getString(2);
                teacherID.put(teacher_name, teacher_id);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //link tables
    public static void toDB(c_Class c_class, Student student) {
        if (con != null) {
            String sql = "insert into class_student (class_id, student_id) values (?,?) on conflict do nothing";
            try {
                stmt = con.prepareStatement(sql);

                stmt.setString(1, c_class.class_id);
                stmt.setString(2, student.student_id);

                stmt.execute();

                con.commit();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void toDB(c_Class c_class, int n) {
        getTeacherID();
        List<Teacher> teachers = c_class.teachers;
        if (teachers.size() == 0) return;
        if (con != null) {
            String sql = "insert into class_teacher (class_id, teacher_id) values (?,?) on conflict do nothing";
            for (Teacher t : teachers) {
                int teacher_id = teacherID.get(t.name);
                try {
                    stmt = con.prepareStatement(sql);
                    stmt.setString(1, c_class.class_id);
                    stmt.setInt(2, teacher_id);

                    stmt.execute();

                    con.commit();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void toDB(Course course, c_Class c_class) {
        if (con != null) {
            String sql = "insert into course_class (course_id, class_id) values (?,?) on conflict do nothing";
            try {
                stmt = con.prepareStatement(sql);

                stmt.setString(1, course.course_id);
                stmt.setString(2, c_class.class_id);

                stmt.execute();

                con.commit();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}

