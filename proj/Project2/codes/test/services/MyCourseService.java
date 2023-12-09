package test.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.dto.prerequisite.AndPrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.CoursePrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.OrPrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.Prerequisite;
import cn.edu.sustech.cs307.service.CourseService;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import test.Util.prerequisiteTool;


import javax.annotation.Nullable;
import java.sql.*;
import java.time.DayOfWeek;
import java.util.*;

public class MyCourseService implements CourseService {

    @Override
    public void addCourse(String courseId, String courseName, int credit, int classHour, Course.CourseGrading grading, @Nullable Prerequisite prerequisite)  {
        //TODO  将Java对象的参数导入数据库
        Prerequisite advanced = null;
        if (prerequisite !=null) {
            advanced = prerequisiteTool.advancePrerequisite(prerequisite);
        }

        try (Connection con = SQLDataSource.getInstance().getSQLConnection();
        PreparedStatement stmt = con.prepareStatement("insert into courses(id,name,credit,classHour,grade_type,max_group) values(?,?,?,?,?,?)")){
            stmt.setString(1,courseId);
            stmt.setString(2,courseName);
            stmt.setInt(3,credit);
            stmt.setInt(4,classHour);
            if (grading== Course.CourseGrading.HUNDRED_MARK_SCORE){
                stmt.setString(5,"H");
            }else if (grading == Course.CourseGrading.PASS_OR_FAIL){
                stmt.setString(5,"P");
            }
            stmt.setInt(6,0);
            stmt.executeUpdate();

            if (advanced !=null) {
                int max_group = insertAdvancedPrerequisite(courseId, advanced);
                PreparedStatement ps = con.prepareStatement("update courses set max_group = ? where max_group=0;");
                ps.setInt(1,max_group);
                ps.executeUpdate();
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        //prerequisite需要处理



    }

    private synchronized int insertAdvancedPrerequisite(String course_id,Prerequisite advanced){
        if (advanced == null){
            return 0;
        }

        int groupId=1;

        Connection con = null;

        try {
            con = SQLDataSource.getInstance().getSQLConnection();
            String sql = "insert into prerequisite" +
                         "(course_id,prerequisite_id,group_id)"+
                         "values(?,?,?);";

            PreparedStatement ps = con.prepareStatement(sql);
            con.setAutoCommit(false);

            if(advanced instanceof CoursePrerequisite){
                CoursePrerequisite course=(CoursePrerequisite)advanced;

                ps.setString(1,course_id);
                ps.setString(2,course.courseID);
                ps.setInt(3,groupId);

                ps.executeUpdate();
                groupId++;
            }else if(advanced instanceof AndPrerequisite){
                AndPrerequisite and=(AndPrerequisite) advanced;

                for (Prerequisite child: and.terms){
                    if(child instanceof CoursePrerequisite){
                        CoursePrerequisite course=(CoursePrerequisite) child;
                        ps.setString(1,course_id);
                        ps.setString(2,course.courseID);
                        ps.setInt(3,groupId);
                        ps.executeUpdate();
                    }
                }

                groupId++;
            }else if(advanced instanceof OrPrerequisite){
                OrPrerequisite or=(OrPrerequisite) advanced;

                for (Prerequisite child :or.terms){
                    if(child instanceof AndPrerequisite){
                        AndPrerequisite and=(AndPrerequisite) child;
                        for (Prerequisite courseChild:and.terms){
                            if(courseChild instanceof CoursePrerequisite){
                                try{
                                    CoursePrerequisite course=(CoursePrerequisite) courseChild;

                                    ps.setString(1,course_id);
                                    ps.setString(2,course.courseID);
                                    ps.setInt(3,groupId);
                                    ps.executeUpdate();
                                }catch (SQLException e){
                                    e.printStackTrace();
                                    groupId--;
                                    break;
                                }
                            }
                        }
                        groupId++;
                    }
                    else if(child instanceof CoursePrerequisite){
                        CoursePrerequisite course=(CoursePrerequisite) child;
                        ps.setString(1,course_id);
                        ps.setString(2,course.courseID);
                        ps.setInt(3,groupId);
                        ps.executeUpdate();
                        groupId++;
                    }
                }
            }


            //ps.executeBatch();

            con.commit();
            con.close();

            return groupId-1;
        }catch (Exception e){
            try{
                if(con!=null){
                    con.rollback();
                    con.close();
                }
            }catch (SQLException ex){
                ex.printStackTrace();
            }

            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    /*protected long group1_id=1;
    protected long group2_id=1;
    protected long group3_id=1;
    protected long group4_id=1;
    protected long group5_id=1;//不确定sql中是否会识别long类型数据

    private void insertPrerequisite(Prerequisite p,String courseId){
        //todo 分类插入，三层结构
        Connection con = null;

        try {
            con = SQLDataSource.getInstance().getSQLConnection();
            con.setAutoCommit(false);

            String sql1 = "insert into type1(courseId,type,course_id,group_id) values("+courseId+",?,?,?)";
            String sql2 = "insert into type2(courseId,type,course_id,group_id) values("+courseId+",?,?,?)";
            String sql3 = "insert into type3(courseId,type,subtype,subgroup_id,group_id) values("+courseId+",?,?,?,?)";
            String sql4 = "insert into type4(courseId,type,course_id,group_id) values("+courseId+"?,?,?)";
            String sql5 = "insert into type5(courseId,type,subtype,subgroup_id,group_id) values("+courseId+",?,?,?,?)";

            PreparedStatement ps=null;

            ArrayList<Integer> level1_type = new ArrayList<>();
            ArrayList<Long> level1_groupId = new ArrayList<>();

            ArrayList<Integer> level2_type = new ArrayList<>();
            ArrayList<Long> level2_groupId = new ArrayList<>();



            if (p instanceof AndPrerequisite){
                AndPrerequisite and = (AndPrerequisite) p;

                boolean isAllCourses = true;
                for (Prerequisite t:and.terms){
                    if (!(t instanceof CoursePrerequisite)){
                        isAllCourses = false;
                    }
                }

                if (isAllCourses){        //and1第2类pre
                    ps = con.prepareStatement(sql2);

                    for (Prerequisite t:and.terms){
                        CoursePrerequisite course = (CoursePrerequisite) t;
                        ps.setInt(1,2);
                        ps.setString(2,course.courseID);
                        ps.setLong(3,group2_id);

                        ps.addBatch();
                    }

                    group2_id++;
                }else {                   //and1第3类pre

                    //给level1填充sub_type、sub_group_id
                    for (Prerequisite child1:and.terms){
                        if (child1 instanceof CoursePrerequisite){
                            CoursePrerequisite course = (CoursePrerequisite) child1;

                            ps = con.prepareStatement(sql1);
                            ps.setInt(1,1);
                            ps.setString(2,course.courseID);
                            ps.setLong(3,group4_id);
                            ps.addBatch();

                            level1_type.add(4);
                            level1_groupId.add(group4_id++);
                        }else if (child1 instanceof OrPrerequisite){
                            OrPrerequisite or = (OrPrerequisite) child1;

                            isAllCourses = true;
                            for (Prerequisite t:or.terms){
                                if (!(t instanceof CoursePrerequisite)){
                                    isAllCourses = false;
                                }
                            }

                            if (isAllCourses){ //p(and)的某个child 是第4类pre
                                ps = con.prepareStatement(sql4);
                                for (Prerequisite t:or.terms){
                                    CoursePrerequisite course = (CoursePrerequisite) t;

                                    ps.setInt(1,4);
                                    ps.setString(2,course.courseID);
                                    ps.setLong(3,group4_id);

                                    ps.addBatch();
                                }

                                level1_type.add(4);
                                level1_groupId.add(group4_id++);

                            }else {            //p(and)的某个child是第5类pre
                                //构建level2_type、level2_groupId
                                for (Prerequisite child2:((OrPrerequisite) child1).terms){
                                    if (child2 instanceof CoursePrerequisite){
                                        CoursePrerequisite course = (CoursePrerequisite) child2;

                                        ps = con.prepareStatement(sql2);
                                        ps.setInt(1,2);
                                        ps.setString(2,course.courseID);
                                        ps.setLong(3,group2_id);

                                        ps.addBatch();

                                        level2_type.add(2);
                                        level2_groupId.add(group2_id++);

                                    }else if (child2 instanceof AndPrerequisite){
                                        //此处不考虑child2有嵌套的情况，单纯视作type2
                                        for (Prerequisite child3:((AndPrerequisite) child2).terms){
                                            CoursePrerequisite course = (CoursePrerequisite)child3;
                                            ps = con.prepareStatement(sql2);
                                            ps.setInt(1,2);
                                            ps.setString(2,course.courseID);
                                            ps.setLong(3,group2_id);

                                            ps.addBatch();
                                        }
                                        level2_type.add(2);
                                        level2_groupId.add(group2_id++);
                                    }
                                }
                                //向batch中添加语句，同步遍历level2_type、level2_groupId
                                ps = con.prepareStatement(sql5);

                                int type;
                                long gid;
                                for (int c=0;c<level2_type.size();c++){
                                    type = level2_type.get(c);
                                    gid = level2_groupId.get(c);

                                    ps.setInt(1,5);
                                    ps.setInt(2,type);
                                    ps.setLong(3,gid);
                                    ps.setLong(4,group5_id);

                                    ps.addBatch();
                                }

                                group5_id++;

                                level2_type.clear();
                                level2_groupId.clear();
                            }
                        }

                    }

                    //向batch中添加语句，同步遍历level1_type level1_groupId
                    ps = con.prepareStatement(sql3);

                    int type;
                    long gid;
                    for (int c=0;c<level1_groupId.size();c++){
                        type = level1_type.get(c);
                        gid = level1_groupId.get(c);

                        ps.setInt(1,3);
                        ps.setInt(2,type);
                        ps.setLong(3,gid);
                        ps.setLong(4,group3_id);

                        ps.addBatch();
                    }

                    group3_id++;

                    level1_groupId.clear();
                    level1_type.clear();
                }


            }else if (p instanceof OrPrerequisite){
                OrPrerequisite or=(OrPrerequisite) p;

                boolean isAllCourses = true;
                for (Prerequisite t:or.terms){
                    if (!(t instanceof CoursePrerequisite)){
                        isAllCourses = false;
                    }
                }

                if (isAllCourses){   //p(or)是第4类pre，内部全为coursePrerequisite
                    ps = con.prepareStatement(sql4);

                    for (Prerequisite t:or.terms){
                        CoursePrerequisite course = (CoursePrerequisite) t;
                        ps.setInt(1,4);
                        ps.setString(2,course.courseID);
                        ps.setLong(3,group4_id);

                        ps.addBatch();
                    }
                    group4_id++;

                }else {              //p(or)是第5类pre，内部又有嵌套
                    //给level1填充sub_type、sub_group_id
                    for (Prerequisite child1 : or.terms) {

                        if (child1 instanceof AndPrerequisite) {
                            AndPrerequisite and = (AndPrerequisite) child1;

                            isAllCourses = true;
                            for (Prerequisite t:and.terms){
                                if (!(t instanceof CoursePrerequisite)){
                                    isAllCourses = false;
                                }
                            }

                            if (isAllCourses){ //p(or)的某个child 是第2类pre
                                ps = con.prepareStatement(sql2);
                                for (Prerequisite t:or.terms){
                                    CoursePrerequisite course = (CoursePrerequisite) t;

                                    ps.setInt(1,2);
                                    ps.setString(2,course.courseID);
                                    ps.setLong(3,group2_id);

                                    ps.addBatch();
                                }

                                level1_type.add(2);
                                level1_groupId.add(group2_id++);

                            }else {            //p(or)的某个child(and)是第5类pre
                                //构建level2_type、level2_groupId
                                for (Prerequisite child2:((AndPrerequisite) child1).terms){
                                    if (child2 instanceof CoursePrerequisite){
                                        CoursePrerequisite course = (CoursePrerequisite) child2;

                                        ps = con.prepareStatement(sql4);
                                        ps.setInt(1,4);
                                        ps.setString(2,course.courseID);
                                        ps.setLong(3,group4_id);

                                        ps.addBatch();

                                        level2_type.add(4);
                                        level2_groupId.add(group4_id++);

                                    }else if (child2 instanceof OrPrerequisite){
                                        //此处不考虑child2中还有嵌套的情况，单纯视作type4
                                        for (Prerequisite child3:((OrPrerequisite) child2).terms){
                                            CoursePrerequisite course = (CoursePrerequisite)child3;
                                            ps = con.prepareStatement(sql4);
                                            ps.setInt(1,4);
                                            ps.setString(2,course.courseID);
                                            ps.setLong(3,group4_id);

                                            ps.addBatch();

                                        }
                                        level2_type.add(4);
                                        level2_groupId.add(group4_id++);
                                    }
                                }
                                //向batch中添加语句，同步遍历level2_type、level2_groupId
                                ps = con.prepareStatement(sql3);

                                int type;
                                long gid;
                                for (int c=0;c<level2_type.size();c++){
                                    type = level2_type.get(c);
                                    gid = level2_groupId.get(c);

                                    ps.setInt(1,3);
                                    ps.setInt(2,type);
                                    ps.setLong(3,gid);
                                    ps.setLong(4,group3_id);

                                    ps.addBatch();
                                }

                                group3_id++;

                                level2_type.clear();
                                level2_groupId.clear();
                            }

                        } else if (child1 instanceof CoursePrerequisite) {
                            CoursePrerequisite course = (CoursePrerequisite) child1;

                            ps = con.prepareStatement(sql2);
                            ps.setInt(1,2);
                            ps.setString(2,course.courseID);
                            ps.setLong(3,group2_id);

                            ps.addBatch();

                            level2_type.add(2);
                            level2_groupId.add(group2_id++);
                        }

                    }
                    //向batch中添加语句，同步遍历level1_type level1_groupId
                    ps = con.prepareStatement(sql5);

                    int type;
                    long gid;
                    for (int c=0;c<level1_groupId.size();c++){
                        type = level1_type.get(c);
                        gid = level1_groupId.get(c);

                        ps.setInt(1,5);
                        ps.setInt(2,type);
                        ps.setLong(3,gid);
                        ps.setLong(4,group5_id);

                        ps.addBatch();
                    }

                    group5_id++;

                    level1_type.clear();
                    level1_groupId.clear();
                }
            }

            ps.executeBatch();

            con.commit();
            con.close();

        }catch (Exception e){
            try{
                if(con!=null){
                    con.rollback();
                    con.close();
                }
            }catch (SQLException ex){
                ex.printStackTrace();
            }

            e.printStackTrace();
            throw new IntegrityViolationException();
        }

    }*/
    /*private synchronized void insertPrerequisite(String courseId, @Nullable Prerequisite simplified){
        //System.out.println(simplified);
        if(simplified==null){
            return;
        }

        Connection conn=null;

        try{
            conn=SQLDataSource.getInstance().getSQLConnection();
            String sql="insert into prerequisite" +
                    "(course_id, prerequisitecourseid, group_id)" +
                    " values(?,?,?); ";
            PreparedStatement preparedStatement=conn.prepareStatement(sql);
            conn.setAutoCommit(false);


            int groupId=1;
            if(simplified instanceof CoursePrerequisite){
                CoursePrerequisite course=(CoursePrerequisite)simplified;

                preparedStatement.setString(1,courseId);
                preparedStatement.setString(2,course.courseID);
                preparedStatement.setInt(3,groupId);

                preparedStatement.addBatch();

            }else if(simplified instanceof AndPrerequisite){
                AndPrerequisite and=(AndPrerequisite) simplified;

                for (Prerequisite child: and.terms){
                    if(child instanceof CoursePrerequisite){
                        CoursePrerequisite course=(CoursePrerequisite) child;
                        preparedStatement.setString(1,courseId);
                        preparedStatement.setString(2,course.courseID);
                        preparedStatement.setInt(3,groupId);
                        preparedStatement.addBatch();
                    }
                }

            }else if(simplified instanceof OrPrerequisite){
                OrPrerequisite or=(OrPrerequisite) simplified;

                for (Prerequisite child :or.terms){
                    if(child instanceof AndPrerequisite){
                        AndPrerequisite and=(AndPrerequisite) child;
                        for (Prerequisite courseChild:and.terms){
                            if(courseChild instanceof CoursePrerequisite){
                                try{
                                    CoursePrerequisite course=(CoursePrerequisite) courseChild;

                                    preparedStatement.setString(1,courseId);
                                    preparedStatement.setString(2,course.courseID);
                                    preparedStatement.setInt(3,groupId);
                                    preparedStatement.addBatch();
                                }catch (SQLException e){
                                    e.printStackTrace();
                                    groupId--;
                                    break;
                                }
                            }
                        }
                        groupId++;
                    }
                    else if(child instanceof CoursePrerequisite){
                        CoursePrerequisite course=(CoursePrerequisite) child;
                        preparedStatement.setString(1,courseId);
                        preparedStatement.setString(2,course.courseID);
                        preparedStatement.setInt(3,groupId);
                        preparedStatement.addBatch();
                        groupId++;
                    }
                }
            }
            preparedStatement.executeBatch();


            conn.commit();
            conn.close();
        }catch (SQLException e){
            try{
                if(conn!=null){
                    conn.rollback();
                    conn.close();
                }
            }catch (SQLException ex){
                ex.printStackTrace();
            }

            e.printStackTrace();
            throw new IntegrityViolationException();
        }

    }*/
    @Override
    public int addCourseSection(String courseId, int semesterId, String sectionName, int totalCapacity) {
        Connection conn = null;
        try{
            conn =SQLDataSource.getInstance().getSQLConnection();
            conn.setAutoCommit(false);

            String sql
                    ="insert into sections" +
                    "(course_id,semester_id,name,totalcapacity)" +
                    "values(?,?,?,?);";

            PreparedStatement preparedStatement= conn.prepareStatement(sql,PreparedStatement.RETURN_GENERATED_KEYS);

            preparedStatement.setString(1,courseId);
            preparedStatement.setInt(2,semesterId);
            preparedStatement.setString(3,sectionName);
            preparedStatement.setInt(4,totalCapacity);

            preparedStatement.executeUpdate();

            ResultSet rs= preparedStatement.getGeneratedKeys();

            if(rs.next()){

                int result=rs.getInt(3);
                conn.commit();
                conn.close();

                return result;
            }else {
                conn.commit();
                conn.close();

                throw new IntegrityViolationException();
            }
        }catch (SQLException e){
            try{
                if(conn !=null){
                    conn.rollback();
                    conn.close();
                }
            }catch (SQLException ex){
                ex.printStackTrace();
            }

            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public int addCourseSectionClass(int sectionId, int instructorId, DayOfWeek dayOfWeek, Set<Short> weekList, short classStart, short classEnd, String location) {
        //System.out.print("***");
        checkOtherExistent("sections",sectionId);
        try(Connection con=SQLDataSource.getInstance().getSQLConnection()){
            //System.out.println("sectionId = " + sectionId);
            String sql = "insert into classes"+
                         "(dayOfWeek,weekList,classBegin,classEnd,location,section_id)"+
                         "values(?,?,?,?,?,?);";

            PreparedStatement ps = con.prepareStatement(sql,PreparedStatement.RETURN_GENERATED_KEYS);

            String[] weeks = new String[weekList.size()];
            int counter = 0;
            for (short t:weekList){
                weeks[counter++] = Short.toString(t);
            }
            String array = "{";
            StringBuilder sb = new StringBuilder(array);
            for (int c=0;c<counter-1;c++){
                sb.append(weeks[c]);
                sb.append(",");
            }
            sb.append(weeks[weeks.length-1]);
            sb.append("}");

            int day=1;
            switch (dayOfWeek){
                case MONDAY:day=1;
                case TUESDAY:day=2;
                case WEDNESDAY:day=3;
                case THURSDAY:day=4;
                case FRIDAY:day=5;
                case SATURDAY:day=6;
                case SUNDAY:day=7;
            }

            int l = weekList.size();
            Short[] arr = new Short[l];

            arr = weekList.toArray(new Short[0]);

            /*for (Short t:arr){
                System.out.print(t+" ");
            }
            System.out.println();*/

            Array array1 = con.createArrayOf("integer",arr);

            ps.setInt(1,day);
            ps.setArray(2,array1);
            ps.setInt(3,classStart);
            ps.setInt(4,classEnd);
            ps.setString(5,location);
            ps.setInt(6,sectionId);

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            int class_id=-1;
            if (rs.next()){
                 class_id = rs.getInt(1);
            }else {
                throw new EntityNotFoundException();
            }

            if (class_id!=-1) {
                String sql1 = "insert into class_instructor"+
                        "(class_id,instructor_id)"+
                        "values(?,?);";
                PreparedStatement ps1 = con.prepareStatement(sql1);

                ps1.setInt(1,class_id);
                ps1.setInt(2,instructorId);

                ps1.executeUpdate();
            }else {
                throw new IntegrityViolationException();
            }
            return class_id;
        }catch (SQLException e){
            e.printStackTrace();
            throw new IntegrityViolationException();
        }

    }

    private synchronized void insertClasses_instructor(int class_id,int instructor_id){
        try (Connection con = SQLDataSource.getInstance().getSQLConnection()){
            String sql = "insert into class_instructor"+
                         "(class_id,instructor_id)"+
                         "values(?,?);";
            PreparedStatement ps1 = con.prepareStatement(sql);

            ps1.setInt(1,class_id);
            ps1.setInt(2,instructor_id);

            ps1.executeUpdate();

        }catch (Exception e){
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public void removeCourse(String courseId) {
        try (Connection con = SQLDataSource.getInstance().getSQLConnection()){
            String sql="delete from courses where id=?";
            PreparedStatement p=con.prepareStatement(sql);

            p.setString(1,courseId);

            p.executeUpdate();
        }catch (Exception e){
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public void removeCourseSection(int sectionId) {
        try (Connection con = SQLDataSource.getInstance().getSQLConnection()){
            String sql="delete from sections where id=?";
            PreparedStatement p=con.prepareStatement(sql);

            p.setInt(1,sectionId);

            p.executeUpdate();
        }catch (Exception e){
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public void removeCourseSectionClass(int classId) {
        try(Connection conn=SQLDataSource.getInstance().getSQLConnection()){
            String sql="delete from classes where id=?";
            PreparedStatement p=conn.prepareStatement(sql);

            p.setInt(1,classId);

            p.executeUpdate();
        }catch (SQLException e){
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public List<Course> getAllCourses() {
        ArrayList<Course> courses = new ArrayList<>();

        try (Connection con = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = con.prepareStatement("select * from courses")){
            ResultSet rs = stmt.executeQuery();
            while (rs.next()){
                String id = rs.getString(1);
                String name = rs.getString(2);
                int credit = rs.getInt(3);
                int classHour = rs.getInt(4);
                String grade = rs.getString(5);

                Course.CourseGrading grading;
                if (grade == "P"){
                    grading = Course.CourseGrading.PASS_OR_FAIL;
                }else {
                    grading = Course.CourseGrading.HUNDRED_MARK_SCORE;
                }

                Course temp = new Course();
                temp.id = id;
                temp.name = name;
                temp.credit = credit;
                temp.classHour = classHour;
                temp.grading = grading;

                courses.add(temp);
            }
            return courses;
        }catch (SQLException e){
            System.err.println(e);
            return courses;
        }

    }

    /////////// here begin
    @Override    // tested
    public List<CourseSection> getCourseSectionsInSemester(String courseId, int semesterId) {
        checkCourseExistent(courseId);
        checkOtherExistent("semesters",semesterId);
        List<CourseSection> allSectionInSemester = new ArrayList<>();
        try (Connection con = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement p = con.prepareStatement("with s as (select id, name, totalcapacity from sections sc where course_id = ? and semester_id = ?)  " +
                     "select id,name,totalcapacity,totalcapacity-count(*) as leftcapacity " +
                     "from select_course sc " +
                     "join s on sc.section_id = s.id " +
                     "group by s.id,s.name,s.totalcapacity;");) {
            p.setString(1, courseId);
            p.setInt(2, semesterId);
            ResultSet resultSet = p.executeQuery();

            while (resultSet.next()) {
                CourseSection cs = new CourseSection();
                cs.id = resultSet.getInt("id");
                cs.name = resultSet.getString("name");
                cs.totalCapacity = resultSet.getInt("totalcapacity");
                cs.leftCapacity = resultSet.getInt("leftcapacity");
                allSectionInSemester.add(cs);
            }
//            if (allSectionInSemester.isEmpty()) {
//                System.out.printf("No section with courseId %s in semester %d found! %n", courseId, semesterId);
//                throw new EntityNotFoundException();
//            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new EntityNotFoundException();
        }
        return allSectionInSemester;
    }

    @Override    // tested
    public Course getCourseBySection(int sectionId) {
        checkOtherExistent("sections",sectionId);
        Course c = new Course();
        try (Connection con = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement p = con.prepareStatement("select id, name, credit, classhour, grade_type from courses where id in (select course_id from sections where semester_id = ?);");) {
            p.setInt(1, sectionId);
            ResultSet resultSet = p.executeQuery();

            if (resultSet.next()) {
                c.id = resultSet.getString("id");
                c.name = resultSet.getString("name");
                c.credit = resultSet.getInt("credit");
                c.classHour = resultSet.getInt("classhour");
                String grade_type = resultSet.getString("grade_type");
                c.grading = Objects.equals(grade_type, "P") ? Course.CourseGrading.PASS_OR_FAIL : Course.CourseGrading.HUNDRED_MARK_SCORE;

            } else {
                System.out.printf("Can not find the course by sectionId %s ", sectionId);
                throw new EntityNotFoundException();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
        return c;
    }

    @Override    // tested
    public List<CourseSectionClass> getCourseSectionClasses(int sectionId) {
        checkOtherExistent("sections",sectionId);
        List<CourseSectionClass> allClasses = new ArrayList<>();
        try (Connection con = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement p = con.prepareStatement("with c as (select id, dayofweek, weeklist, classbegin, classend, location from classes where section_id = ?)" +
                     "select c.id as c_id, dayofweek, weeklist, classbegin, classend, location,i.id as i_id, getfullname(firstname, lastname) as fullname from c join class_instructor ci on c.id = ci.class_id join instructors i on ci.instructor_id = i.id;")) {
            p.setInt(1, sectionId);
            ResultSet resultSet = p.executeQuery();

            while (resultSet.next()) {
                CourseSectionClass csc = new CourseSectionClass();
                csc.id = resultSet.getInt("c_id");
                csc.classBegin = resultSet.getShort("classbegin");
                csc.classEnd = resultSet.getShort("classend");
                csc.location = resultSet.getString("location");

                // dayOfWeek
                int dow = resultSet.getInt("dayofweek");
                DayOfWeek d;
                switch (dow) {
                    case 1:
                        d = DayOfWeek.MONDAY;
                        break;
                    case 2:
                        d = DayOfWeek.TUESDAY;
                        break;
                    case 3:
                        d = DayOfWeek.WEDNESDAY;
                        break;
                    case 4:
                        d = DayOfWeek.THURSDAY;
                        break;
                    case 5:
                        d = DayOfWeek.FRIDAY;
                        break;
                    case 6:
                        d = DayOfWeek.SATURDAY;
                        break;
                    case 7:
                        d = DayOfWeek.SUNDAY;
                        break;
                    default:
                        throw new EntityNotFoundException();
                }
                csc.dayOfWeek = d;

                // weekList
                Array array = resultSet.getArray("weeklist");
                Short[] shorts = (Short[]) array.getArray();
                Set<Short> set = new HashSet(Arrays.asList(shorts));
                csc.weekList = set;

                // instructor
                Instructor i = new Instructor();
                i.id = resultSet.getInt("i_id");
                i.fullName = resultSet.getString("fullname");
                csc.instructor = i;

                allClasses.add(csc);
            }
//            if (allClasses.isEmpty()) {
//                System.out.printf("No class with given sectionId %s", sectionId);
//                throw new EntityNotFoundException();
//            }


        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
        return allClasses;
    }
    /////////// here ends

    @Override
    public CourseSection getCourseSectionByClass(int classId) {
        checkOtherExistent("classes",classId);
        //先从table classes里取出id == classId的那一行
        Connection con = null;
        try {
            con = SQLDataSource.getInstance().getSQLConnection();
            con.setAutoCommit(false);
            String sql = "select cs.section_id, c.name,c.totalcapacity,count(s.student_id) " +
                    "from classes as cs " +
                    "inner join sections c on c.id = cs.section_id " +
                    "inner join select_course s on c.id = s.section_id where cs.id=? " +
                    "group by cs.section_id,c.name,c.totalcapacity;";
            PreparedStatement ps = con.prepareStatement(sql);

            ps.setInt(1,classId);
            con.commit();

            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                CourseSection cs=new CourseSection();
                cs.id=rs.getInt(1);
                cs.name=rs.getString(2);
                cs.totalCapacity=rs.getInt(3);
                cs.leftCapacity=cs.totalCapacity-rs.getInt(4);

                return cs;
            }else {
                throw new EntityNotFoundException();
            }
        }catch (SQLException e){
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public List<Student> getEnrolledStudentsInSemester(String courseId, int semesterId) {
        //需要根据学期id和课程id得到某一section下选课学生的列表

        Connection con = null;

        /*try {
            con = SQLDataSource.getInstance().getSQLConnection();
            String sql = "select distinct id from sections where course_id = ? and semester_id = ?;";
            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1,courseId);
            ps.setInt(2,semesterId);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                section_id = rs.getInt(1);
            }else {
                throw new EntityNotFoundException();
            }
        }catch (Exception e){
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
        //第二步，通过courseId获取这门course的grade_type
        try {
            con = SQLDataSource.getInstance().getSQLConnection();
            String sql = "select distinct grade_type from courses where id = ?;";
            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1,courseId);

            ResultSet rs = ps.executeQuery();
            char gradeType;
            if (rs.next()){
                gradeType = rs.getObject(1,Character.class);
            }else {
                throw new EntityNotFoundException();
            }
            String sql1 = "select student_id from student_grades_pf where section_id = ?";
            String sql2 = "select student_id from student_grades_hundred where section_id = ?";

            PreparedStatement ps1;
            if (gradeType == 'P'){
                ps1 = con.prepareStatement(sql1);
            }else if (gradeType == 'H'){
                ps1 = con.prepareStatement(sql2);
            }else {
                con.close();
                throw new EntityNotFoundException();
            }

            if (section_id==-1){
                con.close();
                throw new EntityNotFoundException();
            }else {
                ps1.setInt(1,section_id);
            }

            ResultSet rs1 = ps1.executeQuery();

            while (rs1.next()){
                studentId.add(rs1.getInt(1));
            }
        }catch (Exception e){
            e.printStackTrace();
            throw new IntegrityViolationException();
        }*/
        try {
            List<Student> students = new ArrayList<>();

            con = SQLDataSource.getInstance().getSQLConnection();
            con.setAutoCommit(false);
            String sql = "select s2.id,getfullname(s2.firstname,s2.lastname) as fullName,s2.enrolleddate,m.id,m.name,d.id,d.name" +
                    "from sections s" +
                    "     inner join select_course sc on s.id = sc.section_id" +
                    "     inner join students s2 on s2.id = sc.student_id" +
                    "     inner join majors m on m.id = s2.major_id" +
                    "     inner join departments d on d.id = m.dept_id"+
                    "where course_id = ? and semester_id = ?;";
            PreparedStatement ps = con.prepareStatement(sql);
            //第一步，用courseId和semesterId定下section的id

            //第二步，将sections表与select_course联立，得到该section下的选课学生id、fullname、enrolledDate、Major

            //第三步，用学生id获取student.class在放入List<Student>

            ResultSet rs = ps.executeQuery();
            con.commit();

            while (rs.next()){
                Student student = new Student();
                student.id = rs.getInt(1);
                student.fullName = rs.getString(2);
                student.enrolledDate = rs.getDate(3);

                Major major = new Major();
                major.id = rs.getInt(4);
                major.name = rs.getString(5);

                Department d = new Department();
                d.id = rs.getInt(6);
                d.name = rs.getString(7);
                major.department = d;

                student.major = major;

                students.add(student);
            }

            con.close();

            if (students.isEmpty()){
                throw new EntityNotFoundException();
            }else {
                return students;
            }
        }catch (Exception e){
            try{
                if(con!=null){
                    con.rollback();
                    con.close();
                }
            }catch (SQLException ex){
                ex.printStackTrace();
            }
            throw  new IntegrityViolationException();
        }
    }

    private void checkCourseExistent(String courseId){

        try (Connection con = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement p = con.prepareStatement("select from courses where id = ?;");) {
            p.setString(1, courseId);
            ResultSet resultSet = p.executeQuery();
            if (!resultSet.next()) {
                System.out.printf("key %s is non-existent %n", courseId);
                throw new EntityNotFoundException();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    private final Set<String> tableNames =  new HashSet<String>() {{add("semesters");add("sections");add("classes");}};

    // check key is non-existent : semesterId,sectionId,classId
    private void checkOtherExistent(String tableName,int id){
        if(!tableNames.contains(tableName)){
            System.out.println("Invalid TableName");
            return;
        }
        try (Connection con = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement p = con.prepareStatement("select from "+tableName+" where id = ?;");) {
//            p.setString(1, tableName);
            p.setInt(1,id);
            ResultSet resultSet = p.executeQuery();
            if (!resultSet.next()) {         /* WA */
                System.out.printf("key %d is non-existent %n ", id);
                //throw new EntityNotFoundException();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }
}
