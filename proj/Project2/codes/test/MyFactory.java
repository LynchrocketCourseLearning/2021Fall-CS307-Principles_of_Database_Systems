package test;

import cn.edu.sustech.cs307.factory.ServiceFactory;
import cn.edu.sustech.cs307.service.*;
import test.services.*;

import java.util.List;

public class MyFactory extends ServiceFactory {
    public MyFactory(){
        super();
        registerService(CourseService.class,new MyCourseService());
        registerService(DepartmentService.class,new MyDepartmentService());
        registerService(InstructorService.class,new MyInstructorService());
        registerService(MajorService.class,new MyMajorService());
        registerService(SemesterService.class,new MySemesterService());
        registerService(StudentService.class,new MyStudentService());
        registerService(UserService.class, new MyUserService());
    }
    @Override
    public List<String> getUIDs() {
        return List.of("12011919", "11911838", "12011327");
    }
}
