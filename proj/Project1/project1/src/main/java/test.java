import java.io.*;
import java.util.ArrayList;

public class test {
    static ArrayList<Student> students;

    public static void main(String[] args) throws IOException {
        parseStudent();
        writeFile(students);
    }

    public static void parseStudent() throws IOException {
        students = new ArrayList<>();
        File student_info = new File("src/main/java/data/select_course.csv");
        BufferedReader reader = null;
        try {
            String one_student = null;
            reader = new BufferedReader(new FileReader(student_info));
            while ((one_student = reader.readLine()) != null) {
                String[] s_info = one_student.split(",");
                Student student = new Student(s_info);
                students.add(student);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            reader.close();
        }
    }

    public static void writeFile(ArrayList<Student> students) throws IOException {
        BufferedWriter bw = null;
        String sql = "insert into students (student_id, student_name, gender, college) values (\'%s\',\'%s\',\'%s\',\'%s\')\n";
        try {
            bw = new BufferedWriter(new FileWriter("src/main/java/data/students.sql"));
            for (Student s : students) {
                bw.append(String.format(sql, s.student_id, s.name, s.gender, s.college));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                assert bw != null;
                bw.close();
            }catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("write finished");
        }

    }
}
