package test.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Department;
import cn.edu.sustech.cs307.dto.Major;
import cn.edu.sustech.cs307.dto.Semester;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.MajorService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MyMajorService implements MajorService {
    @Override // tested
    public int addMajor(String name, int departmentId) {
        try (Connection con = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement p = con.prepareStatement("insert into majors (name, dept_id) values (?,?);", PreparedStatement.RETURN_GENERATED_KEYS);
        ) {
            p.setString(1, name);
            p.setInt(2, departmentId);
            p.executeUpdate();

            ResultSet resultSet = p.getGeneratedKeys();
            if (resultSet.next()) {
//                    System.out.println(resultSet.getInt(1));
                return resultSet.getInt(1);
            } else {
                throw new EntityNotFoundException();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override // tested
    public void removeMajor(int majorId) {
        try (Connection con = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement p = con.prepareStatement("delete from majors where id = ?;");
        ) {
            p.setInt(1, majorId);
            int i = p.executeUpdate();
//            System.out.println(i);
            if (i == 0) {
                System.out.println("Can not find the major with id " + majorId);
                throw new EntityNotFoundException();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }

    }

    @Override // tested
    public List<Major> getAllMajors() {
        List<Major> allMajors = new ArrayList<>();
        try (Connection con = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement p = con.prepareStatement("select id, name, dept_id from majors;");) {
            ResultSet resultSet = p.executeQuery();


            while (resultSet.next()) {
                Major m = new Major();
                m.id = resultSet.getInt("id");
                m.name = resultSet.getString("name");

                int dept_id = resultSet.getInt("dept_id");
                MyDepartmentService departmentService = new MyDepartmentService();
                m.department = departmentService.getDepartment(dept_id);

                allMajors.add(m);
            }
//            if (allMajors.isEmpty()) {
//                System.out.println("No semester found!");
//                throw new EntityNotFoundException();
//            }


        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
        return allMajors;
    }

    @Override
    public Major getMajor(int majorId) {
        Major m = new Major();
        try (Connection con = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement p = con.prepareStatement("select id, name, dept_id from majors where id = ?;");) {
            p.setInt(1, majorId);
            ResultSet resultSet = p.executeQuery();

            if (resultSet.next()) {
                m.id = resultSet.getInt("id");
                m.name = resultSet.getString("name");
                MyDepartmentService departmentService = new MyDepartmentService();
                m.department = departmentService.getDepartment(resultSet.getInt("dept_id"));

            } else {
                System.out.println("Can not find the major with id " + majorId);
                throw new EntityNotFoundException();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
        return m;
    }


    @Override
    public void addMajorCompulsoryCourse(int majorId, String courseId) {
        try (Connection con = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement p = con.prepareStatement("insert into major_course (major_id, course_id, type) values (?,?,?);");
        ) {
            p.setInt(1, majorId);
            p.setString(2, courseId);
            p.setString(3, "C");
            p.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }


    }

    @Override
    public void addMajorElectiveCourse(int majorId, String courseId) {
        try (Connection con = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement p = con.prepareStatement("insert into major_course (major_id, course_id, type) values (?,?,?);");
        ) {
            p.setInt(1, majorId);
            p.setString(2, courseId);
            p.setString(3, "E");
            p.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }

    }
}
