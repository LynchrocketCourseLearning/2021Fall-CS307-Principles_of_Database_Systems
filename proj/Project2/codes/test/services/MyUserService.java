package my.impl.myservice;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Instructor;
import cn.edu.sustech.cs307.dto.Student;
import cn.edu.sustech.cs307.dto.User;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.UserService;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class MyUserService implements UserService {
    @Override
    public void removeUser(int userId) {
        try (Connection con = SQLDataSource.getInstance().getSQLConnection()) {
            PreparedStatement stmt = con.prepareStatement("delete from instructors where id = ?");
            stmt.setInt(1, userId);
            int cnt = stmt.executeUpdate();

            if (cnt <= 0) {
                stmt = con.prepareStatement("delete from students where id = ?");
                stmt.setInt(1, userId);
                stmt.executeUpdate();
            }
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<User> getAllUsers() {
        List<User> userList = new ArrayList<>();
        try (Connection con = SQLDataSource.getInstance().getSQLConnection()) {
            PreparedStatement stmt = con.prepareStatement("select * from instructors");
            ResultSet resultSet1 = stmt.executeQuery();
            while (resultSet1.next()) {
                int id = resultSet1.getInt(1);
                String firstname = resultSet1.getString(2);
                String lastname = resultSet1.getString(3);

                User usr = new Instructor();
                usr.id = id;
                if (firstname.matches("[a-zA-Z]") && lastname.matches("[a-zA-Z]")) {
                    usr.fullName = String.format("%s %s", firstname, lastname);
                } else {
                    usr.fullName = firstname + lastname;
                }
                userList.add(usr);
            }

            stmt = con.prepareStatement("select * from students");
            ResultSet resultSet2 = stmt.executeQuery();
            while (resultSet2.next()) {
                int id = resultSet2.getInt(1);
                String firstname = resultSet2.getString(2);
                String lastname = resultSet2.getString(3);
//                Date enrolledDate = resultSet2.getDate(4);
//                int majorId = resultSet2.getInt(5);

                User usr = new Student();
                usr.id = id;
                if (firstname.matches("[a-zA-Z]") && lastname.matches("[a-zA-Z]")) {
                    usr.fullName = String.format("%s %s", firstname, lastname);
                } else {
                    usr.fullName = firstname + lastname;
                }
                userList.add(usr);
            }
            stmt.close();
            return userList;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public User getUser(int userId) {
        try (Connection con = SQLDataSource.getInstance().getSQLConnection()) {
            PreparedStatement stmt = con.prepareStatement("select * from instructors where id = ?");
            stmt.setInt(1, userId);
            ResultSet resultSet1 = stmt.executeQuery();
            User usr;
            if (resultSet1.next()) {
                usr = new Instructor();
                usr.id = resultSet1.getInt(1);
                String firstname = resultSet1.getString(2);
                String lastname = resultSet1.getString(3);

                if (firstname.matches("[a-zA-Z]") && lastname.matches("[a-zA-Z]")) {
                    usr.fullName = String.format("%s %s", firstname, lastname);
                } else {
                    usr.fullName = firstname + lastname;
                }
            } else {
                stmt = con.prepareStatement("select * from students where id = ?");
                stmt.setInt(1, userId);
                ResultSet resultSet2 = stmt.executeQuery();

                usr = new Student();
                usr.id = resultSet2.getInt(1);
                String firstname = resultSet2.getString(2);
                String lastname = resultSet2.getString(3);

                if (firstname.matches("[a-zA-Z]") && lastname.matches("[a-zA-Z]")) {
                    usr.fullName = String.format("%s %s", firstname, lastname);
                } else {
                    usr.fullName = firstname + lastname;
                }
            }
            stmt.close();
            return usr;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }
}
