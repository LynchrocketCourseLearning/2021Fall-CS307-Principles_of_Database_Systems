package test.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Department;
import cn.edu.sustech.cs307.dto.Semester;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.DepartmentService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MyDepartmentService implements DepartmentService {
    @Override
    public int addDepartment(String name) {
        try(Connection con = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement p = con.prepareStatement("insert into departments (name) values (?);",PreparedStatement.RETURN_GENERATED_KEYS);
        ){
            p.setString(1,name);
            p.executeUpdate();

            ResultSet resultSet = p.getGeneratedKeys();
            if(resultSet.next()){
//                    System.out.println(resultSet.getInt(1));
                return resultSet.getInt(1);
            }else{
                throw new EntityNotFoundException();
            }

        }catch (SQLException e){
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public void removeDepartment(int departmentId) {
        try(Connection con = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement p = con.prepareStatement("delete from departments where id = ?;");
        ){
            p.setInt(1,departmentId);
            int i = p.executeUpdate();
//            System.out.println(i);
            if(i==0){
                System.out.println( "Can not find the department with id " + departmentId);
                throw new EntityNotFoundException();
            }
        }catch (SQLException e){
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public List<Department> getAllDepartments() {
        List<Department> allDepartments = new ArrayList<>();
        try(Connection con = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement p = con.prepareStatement("select id, name from departments;")){
            ResultSet resultSet = p.executeQuery();

//            if(!resultSet.next()){
//                System.out.println("No department found!");
//                throw new EntityNotFoundException();
//            }else{
                while (resultSet.next()){
                    Department d = new Department();
                    d.id = resultSet.getInt("id");
                    d.name = resultSet.getString("name");
                    allDepartments.add(d);
                }
//            }
        }catch (SQLException e){
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
        return allDepartments;
    }

    @Override
    public Department getDepartment(int departmentId) {
        Department d = new Department();
        try(Connection con = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement p = con.prepareStatement("select id, name from departments where id = ?;")){
            p.setInt(1,departmentId);
            ResultSet resultSet = p.executeQuery();

            if(resultSet.next()){
                d.id = resultSet.getInt("id");
                d.name = resultSet.getString("name");
            }else {
                System.out.println( "Can not find the department with id " + departmentId);
                throw new EntityNotFoundException();
            }

        }catch (SQLException e){
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
        return d;
    }
}
