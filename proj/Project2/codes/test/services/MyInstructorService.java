package my.impl.myservice;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.CourseSection;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.InstructorService;

import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class MyInstructorService implements InstructorService {
    @Override
    public void addInstructor(int userId, String firstName, String lastName) {
        try (Connection con = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = con.prepareStatement("insert into instructors (id, firstname, lastname) values (?, ?, ?)")) {
            stmt.setInt(1, userId);
            stmt.setString(2, firstName);
            stmt.setString(3, lastName);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public List<CourseSection> getInstructedCourseSections(int instructorId, int semesterId) {
        try (Connection con = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = con.prepareStatement(
                     "select id, name, totalcapacity, (totalcapacity - cnt) leftcapacity " +
                             " from (select distinct sec.id, sec.name, sec.totalcapacity, count(*) over (partition by sec.id) cnt " +
                             "      from (select id, name, totalcapacity from sections where semester_id = ?) sec " +
                             "               join (select id, section_id " +
                             "                     from classes " +
                             "                              join (select * from class_instructor where instructor_id = ?) ci " +
                             "                                   on ci.class_id = id) i_cla " +
                             "                    on sec.id = i_cla.section_id " +
                             "               join select_course sc on i_cla.section_id = sc.section_id) t ")) {
            stmt.setInt(1, semesterId);
            stmt.setInt(2, instructorId);
            stmt.executeUpdate();
            ResultSet rs = stmt.executeQuery();

            List<CourseSection> res = new ArrayList<>();
            while (rs.next()) {
                CourseSection sec = new CourseSection();
                sec.id = rs.getInt(1);
                sec.name = rs.getString(2);
                sec.totalCapacity = rs.getInt(3);
                sec.leftCapacity = rs.getInt(4);
                res.add(sec);
            }
            rs.close();
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }
}
