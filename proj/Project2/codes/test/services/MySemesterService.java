package test.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Semester;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.SemesterService;

import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class MySemesterService implements SemesterService {
    @Override
    public int addSemester(String name, Date begin, Date end) {
        if (end.after(begin)) {
            try (Connection con = SQLDataSource.getInstance().getSQLConnection();
                 PreparedStatement p = con.prepareStatement("insert into semesters (name, begin_date, end_date) values (?,?,?);", PreparedStatement.RETURN_GENERATED_KEYS);
            ) {
                p.setString(1, name);
                p.setDate(2, begin);
                p.setDate(3, end);
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
        } else {
            throw new IntegrityViolationException();
        }


    }

    @Override
    public void removeSemester(int semesterId) {
        try (Connection con = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement p = con.prepareStatement("delete from semesters where id = ?;");
        ) {
            p.setInt(1, semesterId);
            int i = p.executeUpdate();
//            System.out.println(i);
            if (i == 0) {
                System.out.println("Can not find the semester with id " + semesterId);
                throw new EntityNotFoundException();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }

    }

    @Override
    public List<Semester> getAllSemesters() {
        // List & ArrayList : https://www.cnblogs.com/zcscnn/p/7743507.html
        List<Semester> allSemesters = new ArrayList<>();
        try (Connection con = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement p = con.prepareStatement("select id, name, begin_date, end_date from semesters;");) {
            ResultSet resultSet = p.executeQuery();

            while (resultSet.next()) {
                Semester s = new Semester();
                s.id = resultSet.getInt("id");
                s.name = resultSet.getString("name");
                s.begin = resultSet.getDate("begin_date");
                s.end = resultSet.getDate("end_date");
                allSemesters.add(s);
            }
//            if (allSemesters.isEmpty()) {
//                System.out.println("No semester found!");
//                throw new EntityNotFoundException();
//            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
        return allSemesters;
    }

    @Override
    public Semester getSemester(int semesterId) {
        Semester s = new Semester();
        try (Connection con = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement p = con.prepareStatement("select id, name, begin_date, end_date from semesters where id = ?;");) {
            p.setInt(1, semesterId);
            ResultSet resultSet = p.executeQuery();

            if (resultSet.next()) {
                s.id = resultSet.getInt("id");
                s.name = resultSet.getString("name");
                s.begin = resultSet.getDate("begin_date");
                s.end = resultSet.getDate("end_date");
            } else {
                System.out.println("Can not find the semester with id " + semesterId);
                throw new EntityNotFoundException();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
        return s;
    }

}
