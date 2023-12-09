package my.impl.myservice;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.dto.grade.Grade;
import cn.edu.sustech.cs307.dto.grade.HundredMarkGrade;
import cn.edu.sustech.cs307.dto.grade.PassOrFailGrade;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.StudentService;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.*;
import java.sql.Date;
import java.time.DayOfWeek;
import java.util.*;

@ParametersAreNonnullByDefault
public class MyStudentService implements StudentService {

    @Override
    public void addStudent(int userId, int majorId, String firstName, String lastName, Date enrolledDate) {
        if (userId <= 0 || majorId <= 0) {
            throw new IntegrityViolationException();
        }
        try (Connection con = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = con.prepareStatement("insert into students (id, firstname, lastname, enrolleddate, major_id) values (?, ?, ?, ?, ?)")) {
            stmt.setInt(1, userId);
            stmt.setString(2, firstName);
            stmt.setString(3, lastName);
            stmt.setDate(4, enrolledDate);
            stmt.setInt(5, majorId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class CouToSec {
        Course cou;
        CourseSection sec;

        public CouToSec(Course cou, CourseSection sec) {
            this.cou = cou;
            this.sec = sec;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || this.getClass() != o.getClass()) return false;
            CouToSec ano = (CouToSec) o;
            return Objects.equals(cou, ano.cou) && Objects.equals(sec, ano.sec);
        }

        @Override
        public int hashCode() {
            return Objects.hash(cou, sec.id, sec.name);
        }
    }

    public static class searchProperty {
        boolean Full = false;
        boolean Conflict = false;
        boolean Passed = false;
        boolean MissingPrerequisites = false;
    }

    //通过searchCourseType来找找到全部符合条件的courseId
    public List<String> getCourseType(Connection con, int studentId, CourseType searchCourseType) {
        List<String> courseId = new ArrayList<>();
        if (searchCourseType.equals(CourseType.ALL)) {
            return courseId;
        }
        String sql;
        boolean che = false;
        if (searchCourseType.equals(CourseType.MAJOR_COMPULSORY)) {
            sql = "select course_id from major_course where type = 'C' and major_id = ?;";
        } else if (searchCourseType.equals(CourseType.MAJOR_ELECTIVE)) {
            sql = "select course_id from major_course where type = 'E' and major_id = ?;";
        } else if (searchCourseType.equals(CourseType.CROSS_MAJOR)) {
            sql = "select course_id from major_course where major_id!= ?;";
        } else {//!PUBLIC -> ALL MAJOR
            sql = "select distinct course_id from major_course;";
            che = true;
        }
        try (PreparedStatement stmt = con.prepareStatement(sql);
             PreparedStatement stmtMajor = con.prepareStatement("select major_id from students where id = ?")) {
            if (che) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    courseId.add(rs.getString(1));
                }
                rs.close();
                return courseId;
            }
            stmtMajor.setInt(1, studentId);
            ResultSet ma = stmtMajor.executeQuery();
            if (ma.next()) {
                int majorId = ma.getInt(1);
                stmt.setInt(1, majorId);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    courseId.add(rs.getString(1));
                }
                rs.close();
                ma.close();
                return courseId;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
        return courseId;
    }

    @Override
    public List<CourseSearchEntry> searchCourse(int studentId, int semesterId, @Nullable String searchCid,
                                                @Nullable String searchName, @Nullable String searchInstructor,
                                                @Nullable DayOfWeek searchDayOfWeek, @Nullable Short searchClassTime,
                                                @Nullable List<String> searchClassLocations, CourseType searchCourseType,
                                                boolean ignoreFull, boolean ignoreConflict, boolean ignorePassed,
                                                boolean ignoreMissingPrerequisites, int pageSize, int pageIndex) {
        try (Connection con = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = con.prepareStatement(
                     "select distinct final.courseid, final.coursename, final.credit, final.classhour, final.grade_type, " +
                             "                 final.sectionid, final.sectionname," +
                             "                 final.totalcapacity, final.cnt, final.classid, final.instructorid, final.firstname, final.lastname, " +
                             "                 final.dayofweek, " +
                             "                 final.weeklist, final.classbegin, final.classend, final.location " +
                             "from (select cou.id               as courseid, " +
                             "             cou.name             as coursename, " +
                             "             cou.credit, " +
                             "             cou.classhour, " +
                             "             cou.grade_type, " +
                             "             sec.id               as sectionid, " +
                             "             sec.name             as sectionname, " +
                             "             sec.totalcapacity, " +
                             "             count(sc.student_id) as cnt, " +
                             "             cla.id               as classid, " +
                             "             i.id                 as instructorid, " +
                             "             i.firstname          as firstname, " +
                             "             i.lastname           as lastname, " +
                             "             cla.dayofweek, " +
                             "             cla.weeklist, " +
                             "             cla.classbegin, " +
                             "             cla.classend, " +
                             "             cla.location " +
                             "      from courses cou " +
                             "               inner join sections sec on cou.id = sec.course_id " +
                             "               inner join classes cla on sec.id = cla.section_id " +
                             "               inner join class_instructor ci on cla.id = ci.class_id " +
                             "               inner join instructors i on i.id = ci.instructor_id " +
                             "               left outer join select_course sc on sc.section_id = sec.id " +
                             "      where (? is null or cou.id = ?) " +
                             "        and (? is null or cou.name || '[' || sec.name || ']' like '%' || ? || '%') " +
                             "        and (? is null or i.firstname || i.lastname like (? || '%') " +
                             "          or (i.firstname || ' ' || i.lastname like (? || '%')) " +
                             "          or i.firstname like (? || '%') or i.lastname like (? || '%')) " +
                             "        and (? is null or cla.dayofweek = ?) " +
                             "        and (? is null or ? between cla.classbegin and cla.classend) " +
                             "        and (sec.semester_id = ?) " +
                             "        and (? is null or cla.location like ('%' || ? || '%')) " +
                             "      group by cou.id, cou.name, cou.credit, cou.classhour, cou.grade_type, sec.id, sec.name, " +
                             "               sec.totalcapacity, cla.id, i.id, firstname, lastname, " +
                             "               cla.dayofweek, cla.classbegin, cla.classend, cla.location" +
                             "      order by cou.id, (cou.name || '[' || sec.name || ']')) as final ")) {

            //@Nullable String searchCid
            if (searchCid == null) {
                stmt.setNull(1, Types.VARCHAR);
                stmt.setNull(2, Types.VARCHAR);
            } else {
                stmt.setString(1, searchCid);
                stmt.setString(2, searchCid);
            }

            //@Nullable String searchName
            if (searchName == null) {
                stmt.setNull(3, Types.VARCHAR);
                stmt.setNull(4, Types.VARCHAR);
            } else {
                stmt.setString(3, searchName);
                stmt.setString(4, searchName);
            }

            //@Nullable String searchInstructor
            if (searchInstructor == null) {
                stmt.setNull(5, Types.VARCHAR);
                stmt.setNull(6, Types.VARCHAR);
                stmt.setNull(7, Types.VARCHAR);
                stmt.setNull(8, Types.VARCHAR);
                stmt.setNull(9, Types.VARCHAR);
            } else {
                stmt.setString(5, searchInstructor);
                stmt.setString(6, searchInstructor);
                stmt.setString(7, searchInstructor);
                stmt.setString(8, searchInstructor);
                stmt.setString(9, searchInstructor);
            }

            //@Nullable DayOfWeek searchDayOfWeek
            if (searchDayOfWeek == null) {
                stmt.setNull(10, Types.SMALLINT);
                stmt.setNull(11, Types.SMALLINT);
            } else {
                stmt.setInt(10, searchDayOfWeek.getValue());
                stmt.setInt(11, searchDayOfWeek.getValue());
            }

            // @Nullable Short searchClassTime
            if (searchClassTime == null) {
                stmt.setNull(12, Types.SMALLINT);
                stmt.setNull(13, Types.SMALLINT);
            } else {
                stmt.setInt(12, searchClassTime);
                stmt.setInt(13, searchClassTime);
            }

            stmt.setInt(14, semesterId);

            Map<CouToSec, Set<CourseSectionClass>> CouSecToCla = new HashMap<>();
            //@Nullable List<String> searchClassLocations
            if (searchClassLocations == null) {
                stmt.setNull(15, Types.VARCHAR);
                stmt.setNull(16, Types.VARCHAR);

                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    Course cou = new Course();
                    cou.id = rs.getString(1);
                    cou.name = rs.getString(2);
                    cou.credit = rs.getInt(3);
                    cou.classHour = rs.getInt(4);
                    cou.grading = (rs.getString(5).equals("P")) ?
                            Course.CourseGrading.PASS_OR_FAIL : Course.CourseGrading.HUNDRED_MARK_SCORE;

                    CourseSection sec = new CourseSection();
                    sec.id = rs.getInt(6);
                    sec.name = rs.getString(7);
                    sec.totalCapacity = rs.getInt(8);
                    sec.leftCapacity = sec.totalCapacity - rs.getInt(9);

                    CouToSec cts = new CouToSec(cou, sec);//结合Course和Section

                    CourseSectionClass cls = new CourseSectionClass();
                    cls.id = rs.getInt(10);

                    Instructor i = new Instructor();
                    i.id = rs.getInt(11);
                    String firstname = rs.getString(12);
                    String lastname = rs.getString(13);
                    if (firstname.matches("[a-zA-Z]") && lastname.matches("[a-zA-Z]")) {
                        i.fullName = String.format("%s %s", firstname, lastname);
                    } else {
                        i.fullName = firstname + lastname;
                    }

                    cls.instructor = i;
                    cls.dayOfWeek = DayOfWeek.values()[rs.getInt(14) - 1];

                    cls.weekList = Set.of((Short[]) rs.getArray(15).getArray());

                    cls.classBegin = rs.getShort(16);
                    cls.classEnd = rs.getShort(17);
                    cls.location = rs.getString(18);

                    //将相同的Course&Section的Class合起来
                    if (!CouSecToCla.containsKey(cts)) {
                        CouSecToCla.put(cts, new HashSet<>());
                    }
                    CouSecToCla.get(cts).add(cls);
                }

                stmt.close();
                rs.close();
            } else {
                for (String location : searchClassLocations) {
                    stmt.setString(15, location);
                    stmt.setString(16, location);

                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        Course cou = new Course();
                        cou.id = rs.getString(1);
                        cou.name = rs.getString(2);
                        cou.credit = rs.getInt(3);
                        cou.classHour = rs.getInt(4);
                        cou.grading = (rs.getString(5).equals("P")) ?
                                Course.CourseGrading.PASS_OR_FAIL : Course.CourseGrading.HUNDRED_MARK_SCORE;

                        CourseSection sec = new CourseSection();
                        sec.id = rs.getInt(6);
                        sec.name = rs.getString(7);
                        sec.totalCapacity = rs.getInt(8);
                        sec.leftCapacity = sec.totalCapacity - rs.getInt(9);

                        CouToSec cts = new CouToSec(cou, sec);//结合Course和Section

                        CourseSectionClass cls = new CourseSectionClass();
                        cls.id = rs.getInt(10);

                        Instructor i = new Instructor();
                        i.id = rs.getInt(11);
                        String firstname = rs.getString(12);
                        String lastname = rs.getString(13);
                        if (firstname.matches("[a-zA-Z]") && lastname.matches("[a-zA-Z]")) {
                            i.fullName = String.format("%s %s", firstname, lastname);
                        } else {
                            i.fullName = firstname + lastname;
                        }
                        cls.instructor = i;
                        cls.dayOfWeek = DayOfWeek.values()[rs.getInt(14) - 1];

                        cls.weekList = Set.of((Short[]) rs.getArray(15).getArray());

                        cls.classBegin = rs.getShort(16);
                        cls.classEnd = rs.getShort(17);
                        cls.location = rs.getString(18);

                        //将相同的Course&Section的Class合起来
                        if (!CouSecToCla.containsKey(cts)) {
                            CouSecToCla.put(cts, new HashSet<>());
                        }
                        CouSecToCla.get(cts).add(cls);
                    }
                    rs.close();
                }
                stmt.close();
            }

            //将全部数据拉出来，塞到CourseSearchEntry中
            List<CourseSearchEntry> tmp1 = new ArrayList<>();
            Iterator<Map.Entry<CouToSec, Set<CourseSectionClass>>> it = CouSecToCla.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<CouToSec, Set<CourseSectionClass>> entry = it.next();
                CourseSearchEntry cse = new CourseSearchEntry();
                cse.course = entry.getKey().cou;
                cse.section = entry.getKey().sec;
                cse.sectionClasses = entry.getValue();

                cse.conflictCourseNames = new ArrayList<>();//稍后处理

                tmp1.add(cse);
            }

            //处理ignoreFull, ignoreConflict, ignorePassed, ignoreMissingPrerequisites
            Map<CourseSearchEntry, searchProperty> propertyMap = new HashMap<>();
            for (CourseSearchEntry cse : tmp1) {
                searchProperty sp = new searchProperty();

                sp.Full = isClassFull(con, cse.section.id);
                sp.Conflict = isCourseConflict(con, studentId, cse.section.id);
                sp.Passed = isCoursePassed(con, studentId, cse.section.id);
                sp.MissingPrerequisites = !passedPrerequisitesForCourse(con, studentId, cse.course.id);

                propertyMap.put(cse, sp);
            }

            List<CourseSearchEntry> tmp2 = new ArrayList<>();

            List<String> courseIdList = getCourseType(con, studentId, searchCourseType);

            for (Map.Entry<CourseSearchEntry, searchProperty> entry : propertyMap.entrySet()) {
                CourseSearchEntry cse = entry.getKey();
                searchProperty sp = entry.getValue();

//                if (!((sp.Full && ignoreFull) || (!sp.Passed && ignorePassed)
//                        || (sp.Conflict && ignoreConflict) || (sp.MissingPrerequisites && ignoreMissingPrerequisites))) {
//                    //检查CourseType
//                    if (searchCourseType == CourseType.ALL || sp.courseType == searchCourseType) tmp2.add(cse);
//                }
                if (!((!ignoreFull & sp.Full) | (!ignorePassed & !sp.Passed)
                        | (!ignoreConflict & sp.Conflict) | (!ignoreMissingPrerequisites & sp.MissingPrerequisites))) {
                    if (courseIdList.contains(cse.course.id)) tmp2.add(cse);
                }
            }

//            tmp2.sort((r1, r2) -> {
//                if (r1.course.id.compareTo(r2.course.id) != 0) {//先按courseId比
//                    return r1.course.id.compareToIgnoreCase(r2.course.id);
//                } else {//再按course.name[section.name]比
//                    return (r1.course.name + "[" + r1.section.name + "]").compareToIgnoreCase(r2.course.name + "[" + r2.section.name + "]");
//                }
//            });

            //按页输出
            List<CourseSearchEntry> result = new ArrayList<>();
            int page = (pageIndex + 1) * pageSize;
            // we changed here!
            for (int i = pageIndex * pageSize + 1; i < page + 1 && i < tmp1.size(); i++) {
                result.add(tmp1.get(i));
            }
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    public EnrollResult firstTwoResult(int studentId, int sectionId) {
        try (Connection con = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = con.prepareStatement(
                     "(select 'COURSE_NOT_FOUND' where not exists(select null from sections where id = ?)) " +
                             " union all " +
                             "(select 'ALREADY_ENROLLED'" +
                             " where exists(select null from select_course where student_id = ? and section_id = ?)) ")) {
            stmt.setInt(1, sectionId);
            stmt.setInt(2, studentId);
            stmt.setInt(3, sectionId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String res = rs.getString(1);
                rs.close();
                return EnrollResult.valueOf(res);
            } else {
                rs.close();
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    public boolean isCoursePassed(Connection con, int studentId, int sectionId) {
        try (
                PreparedStatement stmt = con.prepareStatement(
                        "select exists(select null from student_grades_hundred where student_id = ? and section_id = ? and grade >= 60) " +
                                " or exists(select null from student_grades_pf where student_id = ? and section_id = ? and grade = 'PASS');")) {
            stmt.setInt(1, studentId);
            stmt.setInt(2, sectionId);
            stmt.setInt(3, studentId);
            stmt.setInt(4, sectionId);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            boolean res = rs.getBoolean(1);
            rs.close();
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    public List<String> getEnrolledCourse(Connection con, int studentId, int sectionId){
        try{
            PreparedStatement stmt = con.prepareStatement("select ");
        }catch (Exception e){
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    //与已选课程时间冲突，或之前已经选了该课程
    public boolean isCourseConflict(Connection con, int studentId, int sectionId) {
        try {
            PreparedStatement stmt1 = con.prepareStatement(
                    "select * " +
                            "from (select allclasses.*, sec.name  as sectionname, cou.name as coursename " +
                            "      from (select cla.*, sec.semester_id " +
                            "            from sections as sec " +
                            "                     inner join classes as cla on sec.id = cla.section_id " +
                            "            where sec.id = ?) as curclasses " +
                            "               inner join classes as allclasses " +
                            "                          on ((allclasses.classbegin <= curclasses.classbegin " +
                            "                              and allclasses.classend >= curclasses.classend) " +
                            "                              or (allclasses.classbegin >= curclasses.classbegin " +
                            "                                  and allclasses.classend <= curclasses.classend) " +
                            "                              or (allclasses.classbegin >= curclasses.classbegin " +
                            "                                  and allclasses.classbegin <= curclasses.classend) " +
                            "                              or (allclasses.classend >= curclasses.classbegin " +
                            "                                  and allclasses.classend <= curclasses.classend)) " +
                            "                              and allclasses.dayofweek = curclasses.dayofweek " +
                            "                              and allclasses.weeklist = curclasses.weeklist " +
                            "               inner join sections as sec " +
                            "                          on sec.id = allclasses.section_id " +
                            "                              and sec.semester_id = curclasses.semester_id " +
                            "               inner join courses as cou on sec.course_id = cou.id) as conflictCourse " +
                            "         inner join (select cs1.student_id, section_id from select_course as cs1 " +
                            "                     union all " +
                            "                     select cs2.student_id, section_id from student_grades_hundred as cs2 " +
                            "                     union all " +
                            "                     select cs3.student_id, section_id from student_grades_pf as cs3) as selected " +
                            "                    on selected.section_id = conflictCourse.section_id " +
                            " where selected.student_id = ?;");
            stmt1.setInt(1, sectionId);
            stmt1.setInt(2, studentId);
            ResultSet rs1 = stmt1.executeQuery();
            boolean res1 = rs1.next();
            stmt1.close();
            rs1.close();

            PreparedStatement stmt2 = con.prepareStatement(
                    "select * from (select cou.id          as course_id, " +
                            "             sec.semester_id as semester_id " +
                            "      from sections as sec " +
                            "               inner join select_course sc on sec.id = sc.section_id " +
                            "               inner join courses cou on cou.id = sec.course_id " +
                            "      where sc.student_id = ?) t " +
                            "         inner join courses as cou on cou.id = t.course_id " +
                            "         inner join sections as sec on cou.id = sec.course_id and sec.semester_id = t.semester_id " +
                            " where sec.id = ? ");
            stmt2.setInt(1, studentId);
            stmt2.setInt(2, sectionId);
            ResultSet rs2 = stmt2.executeQuery();
            boolean res2 = rs2.next();
            stmt2.close();
            rs2.close();

            return (res1 || res2);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    public boolean isClassFull(Connection con, int sectionId) {
        try (PreparedStatement stmt = con.prepareStatement(
                "select count(*), sec.totalcapacity " +
                        " from (select * from select_course where section_id = ?) sc " +
                        "         join sections sec on sc.section_id = sec.id " +
                        " group by sec.id ")) {
            stmt.setInt(1, sectionId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int sele = rs.getInt(1);
                int total = rs.getInt(2);
                rs.close();
                return (sele >= total);
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public EnrollResult enrollCourse(int studentId, int sectionId) {
        try (Connection con = SQLDataSource.getInstance().getSQLConnection()) {
            //COURSE_NOT_FOUND
            //ALREADY_ENROLLED
            EnrollResult en = firstTwoResult(studentId, sectionId);
            if (en != null) {
                return en;
            }
            //ALREADY_PASSED
            if (isCoursePassed(con, studentId, sectionId)) {
                return EnrollResult.ALREADY_PASSED;
            }
            //PREREQUISITES_NOT_FULFILLED
            PreparedStatement stmt1 = con.prepareStatement("select course_id from sections where id = ?");
            stmt1.setInt(1, sectionId);
            ResultSet rs1 = stmt1.executeQuery();
            rs1.next();
            String courseId = rs1.getString(1);
            if (!passedPrerequisitesForCourse(studentId, courseId)) {
                return EnrollResult.PREREQUISITES_NOT_FULFILLED;
            }

            stmt1.close();
            rs1.close();

            //COURSE_CONFLICT_FOUND
            if (isCourseConflict(con, studentId, sectionId)) {
                return EnrollResult.COURSE_CONFLICT_FOUND;
            }
            //COURSE_IS_FULL
            if (isClassFull(con, sectionId)) {
                return EnrollResult.COURSE_IS_FULL;
            }
            //SUCCESS UNKNOWN_ERROR
            PreparedStatement stmt2 = con.prepareStatement("insert into select_course(student_id,section_id) values (?,?)");
            stmt2.setInt(1, studentId);
            stmt2.setInt(2, sectionId);
            int cnt = stmt2.executeUpdate();
            stmt2.close();
            if (cnt == 1) {
                return EnrollResult.SUCCESS;
            } else {
                return EnrollResult.UNKNOWN_ERROR;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public void dropCourse(int studentId, int sectionId) throws IllegalStateException {
        try (Connection con = SQLDataSource.getInstance().getSQLConnection()) {
            PreparedStatement stmt1 = con.prepareStatement(
                    "select grade from (select grade::varchar from student_grades_pf where student_id = ? and section_id = ? " +
                            "      union all " +
                            "      select grade::varchar from student_grades_hundred where student_id = ? and section_id = ?) sg; ");
            stmt1.setInt(1, studentId);
            stmt1.setInt(2, sectionId);
            stmt1.setInt(3, studentId);
            stmt1.setInt(4, sectionId);

            ResultSet rs = stmt1.executeQuery();
            if (rs.next() && rs.getString(1) != null) {
                throw new IllegalStateException();
            }
            rs.close();

            PreparedStatement stmt2 = con.prepareStatement("delete from select_course where student_id = ? and section_id = ?");
            stmt2.setInt(1, studentId);
            stmt2.setInt(2, sectionId);
            stmt2.executeUpdate();
            stmt2.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public void addEnrolledCourseWithGrade(int studentId, int sectionId, @Nullable Grade grade) {
        try (Connection con = SQLDataSource.getInstance().getSQLConnection()) {
            PreparedStatement stmt1 = con.prepareStatement("select grade_type from (select course_id from sections where id = ?) sec join courses on sec.course_id = courses.id");
            stmt1.setInt(1, sectionId);

            ResultSet rs = stmt1.executeQuery();
            String grade_type;
            if (!rs.next()) {
                throw new IntegrityViolationException();
            } else {
                grade_type = rs.getString(1);
                stmt1.close();
                rs.close();
            }

            if (grade == null) {
                if (grade_type.equals("P")) {
                    PreparedStatement stmt2 = con.prepareStatement("insert into student_grades_pf (student_id, section_id, grade) values (?, ?, ?)");
                    stmt2.setInt(1, studentId);
                    stmt2.setInt(2, sectionId);
                    stmt2.setNull(3, Types.CHAR);
                    stmt2.executeUpdate();
                    stmt2.close();
                } else {
                    PreparedStatement stmt2 = con.prepareStatement("insert into student_grades_hundred (student_id, section_id, grade) values (?, ?, ?)");
                    stmt2.setInt(1, studentId);
                    stmt2.setInt(2, sectionId);
                    stmt2.setNull(3, Types.SMALLINT);
                    stmt2.executeUpdate();
                    stmt2.close();
                }
            } else {
                if (grade instanceof PassOrFailGrade) {
                    PreparedStatement stmt3 = con.prepareStatement("insert into student_grades_pf (student_id, section_id, grade) values (?, ?, ?)");
                    stmt3.setInt(1, studentId);
                    stmt3.setInt(2, sectionId);
                    stmt3.setString(3, ((PassOrFailGrade) grade).name());
                    stmt3.executeUpdate();
                    stmt3.close();
                } else if (grade instanceof HundredMarkGrade) {
                    PreparedStatement stmt3 = con.prepareStatement("insert into student_grades_hundred (student_id, section_id, grade) values (?, ?, ?)");
                    stmt3.setInt(1, studentId);
                    stmt3.setInt(2, sectionId);
                    stmt3.setShort(3, ((HundredMarkGrade) grade).mark);
                    stmt3.executeUpdate();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public void setEnrolledCourseGrade(int studentId, int sectionId, Grade grade) {
        try (Connection con = SQLDataSource.getInstance().getSQLConnection()) {
            if (grade instanceof PassOrFailGrade) {
                PreparedStatement stmt = con.prepareStatement("insert into student_grades_pf (student_id, section_id, grade) values (?, ?, ?)");
                stmt.setInt(1, studentId);
                stmt.setInt(2, sectionId);
                stmt.setString(3, ((PassOrFailGrade) grade).name());
                stmt.executeUpdate();
                stmt.close();
            } else if (grade instanceof HundredMarkGrade) {
                PreparedStatement stmt = con.prepareStatement("insert into student_grades_hundred (student_id, section_id, grade) values (?, ?, ?)");
                stmt.setInt(1, studentId);
                stmt.setInt(2, sectionId);
                stmt.setInt(3, ((HundredMarkGrade) grade).mark);
                stmt.executeUpdate();
                stmt.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public Map<Course, Grade> getEnrolledCoursesAndGrades(int studentId, @Nullable Integer semesterId) {
        try (Connection con = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = con.prepareStatement(
                     "select cou.id,  cou.name,  cou.credit, cou.classhour, cou.grade_type, allCou.grade " +
                             " from (select student_id, section_id, grade::varchar from student_grades_hundred sgh where student_id = ? " +
                             "      union all" +
                             "      select student_id, section_id, grade::varchar from student_grades_pf sgp where student_id = ?) as allCou " +
                             "         inner join sections sec on sec.id = allCou.section_id " +
                             "         inner join courses cou on cou.id = allCou.section_id " +
                             " where ? is null sec.semester_id = ? ")) {
            Map<Course, Grade> res = new HashMap<>();
            stmt.setInt(1, studentId);
            stmt.setInt(2, studentId);
            if (semesterId != null) {
                stmt.setInt(3, semesterId);
                stmt.setInt(4, semesterId);
            } else {
                stmt.setNull(3, Types.INTEGER);
                stmt.setNull(4, Types.INTEGER);
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Course course = new Course();
                course.id = rs.getString(1);
                course.name = rs.getString(2);
                course.credit = rs.getInt(3);
                course.classHour = rs.getInt(4);
                course.grading = (rs.getString(5).equals("P")) ? Course.CourseGrading.PASS_OR_FAIL : Course.CourseGrading.HUNDRED_MARK_SCORE;

                String g = rs.getString(6);
                Grade grade;
                if (g == null)
                    grade = null;
                else if (course.grading == Course.CourseGrading.PASS_OR_FAIL)
                    grade = g.equals("PASS") ? PassOrFailGrade.PASS : PassOrFailGrade.FAIL;
                else
                    grade = new HundredMarkGrade(Short.parseShort(g));

                res.put(course, grade);
            }
            rs.close();

            if (res.isEmpty()) {
                throw new EntityNotFoundException();
            } else {
                return res;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public CourseTable getCourseTable(int studentId, Date date) {
        try (Connection con = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = con.prepareStatement(
                     "select sm.id                                as smid, " +
                             "       floor((? - sm.begin_date) / 7.0)::integer + 1          as smweek, " +
                             "       cou.name                             as coursename, " +
                             "       i.id                                 as iid, " +
                             "       getfullname(i.firstname, i.lastname) as iname, " +
                             "       cla.classbegin, " +
                             "       cla.classend, " +
                             "       cla.location, " +
                             "       cla.dayofweek, " +
                             "       sec.name                             as sectionname " +
                             " from semesters as sm " +
                             "         inner join sections sec on sec.semester_id = sm.id " +
                             "         inner join classes cla on sec.id = cla.section_id and cla.dayofweek = floor((? - sm.begin_date) / 7.0)::integer + 1 " +
                             "         inner join courses cou on cou.id = sec.course_id " +
                             "         inner join class_instructor ci on ci.class_id = cla.id " +
                             "         inner join instructors i on i.id = ci.instructor_id " +
                             "         inner join " +
                             "     (select sc.section_id from select_course sc where sc.student_id = ? " +
                             "      union all " +
                             "      select sgp.section_id from student_grades_pf sgp where sgp.student_id = ? " +
                             "      union all " +
                             "      select sgh.section_id from student_grades_hundred sgh where sgh.student_id = ?) scs " +
                             "     on scs.section_id = sec.id " +
                             " where ? between sm.begin_date and sm.end_date ")) {

            stmt.setDate(1, date);
            stmt.setDate(2, date);
            stmt.setInt(3, studentId);
            stmt.setInt(4, studentId);
            stmt.setInt(5, studentId);
            stmt.setDate(6, date);

            ResultSet rs = stmt.executeQuery();

            CourseTable courseTable = new CourseTable();
            courseTable.table = new HashMap<>();
            for (DayOfWeek d : DayOfWeek.values()) {
                courseTable.table.put(d, new HashSet<>());
            }

            while (rs.next()) {
                CourseTable.CourseTableEntry entry = new CourseTable.CourseTableEntry();
                entry.courseFullName = String.format("%s[%s]", rs.getString(3), rs.getString(10));

                Instructor instructor = new Instructor();
                instructor.id = rs.getInt(4);
                instructor.fullName = rs.getString(5);
                entry.instructor = instructor;

                entry.classBegin = rs.getShort(6);
                entry.classEnd = rs.getShort(7);
                entry.location = rs.getString(8);

                DayOfWeek day = DayOfWeek.of(rs.getInt(9));
                courseTable.table.get(day).add(entry);
            }
            rs.close();
            return courseTable;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    public boolean passedPrerequisitesForCourse(Connection con, int studentId, String courseId) {
        try {
            //得到courseId的先修课列表
            PreparedStatement stmt = con.prepareStatement(
                    "select prerequisite_id,group_id " +
                            " from (select * from prerequisite " +
                            "       order by (course_id,group_id) ) a " +
                            " where course_id = ?; ");
            stmt.setString(1, courseId);
            ResultSet rs = stmt.executeQuery();

            int group_id = 1;
            String prerequisite;
            int temp;
            ArrayList<ArrayList<String>> prerequisites = new ArrayList<>();
            ArrayList<String> group = new ArrayList<>();
            if (rs.next()) {
                prerequisite = rs.getString(1);
                temp = rs.getInt(2);
                if (group_id != temp) {
                    prerequisites.add(group);
                    group = new ArrayList<>();
                    group.add(prerequisite);
                    group_id++;
                } else {
                    group.add(prerequisite);
                }
                while (rs.next()) {
                    prerequisite = rs.getString(1);
                    temp = rs.getInt(2);
                    if (group_id != temp) {
                        prerequisites.add(group);
                        group = new ArrayList<>();
                        group.add(prerequisite);
                        group_id++;
                    } else {
                        group.add(prerequisite);
                    }
                }
            } else {
                return true;
            }
            stmt.close();
            rs.close();

            //得到学生的选课清单
            PreparedStatement ps = con.prepareStatement(
                    "select course_id from students s " +
                            "     inner join student_grades_hundred sgh on s.id = sgh.student_id " +
                            "        inner join sections s2 on s2.id = sgh.section_id " +
                            " where s.id = ? " +
                            " union " +
                            " select course_id " +
                            " from students s " +
                            "     inner join student_grades_pf sgp on s.id = sgp.student_id " +
                            "        inner join sections s2 on s2.id = sgp.section_id " +
                            " where s.id = ?;");
            ps.setInt(1, studentId);
            ps.setInt(2, studentId);
            ResultSet rs2 = ps.executeQuery();

            ArrayList<String> selected = new ArrayList<>();

            String courseTemp;
            if (rs2.next()) {
                courseTemp = rs2.getString(1);
                selected.add(courseTemp);
                while (rs2.next()) {
                    courseTemp = rs2.getString(1);
                    selected.add(courseTemp);
                }
            } else {
                ps.close();
                rs2.close();
                return false;
            }

            ps.close();
            rs2.close();

            //挨个比对prerequisites各个组与selected的内容
            //只要某个组里面的所有元素都在selected中有出现，就返回true，如果所有组都没有满足的，返回false
            boolean isSatisfied = false;
            boolean isSatisfiedPartly;
            for (ArrayList<String> t : prerequisites) {
                isSatisfiedPartly = true;
                for (String s : t) {
                    if (!selected.contains(s)) {
                        isSatisfiedPartly = false;
                    }
                }
                if (isSatisfiedPartly) {
                    isSatisfied = true;
                    break;
                }
            }
            return isSatisfied;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public boolean passedPrerequisitesForCourse(int studentId, String courseId) {
        try (Connection con = SQLDataSource.getInstance().getSQLConnection()) {
            //得到courseId的先修课列表
            PreparedStatement stmt = con.prepareStatement(
                    "select prerequisite_id,group_id " +
                            " from (select * from prerequisite " +
                            "       order by (course_id,group_id) ) a " +
                            " where course_id = ?; ");
            stmt.setString(1, courseId);
            ResultSet rs = stmt.executeQuery();

            int group_id = 1;
            String prerequisite;
            int temp;
            ArrayList<ArrayList<String>> prerequisites = new ArrayList<>();
            ArrayList<String> group = new ArrayList<>();
            if (rs.next()) {
                prerequisite = rs.getString(1);
                temp = rs.getInt(2);
                if (group_id != temp) {
                    prerequisites.add(group);
                    group = new ArrayList<>();
                    group.add(prerequisite);
                    group_id++;
                } else {
                    group.add(prerequisite);
                }
                while (rs.next()) {
                    prerequisite = rs.getString(1);
                    temp = rs.getInt(2);
                    if (group_id != temp) {
                        prerequisites.add(group);
                        group = new ArrayList<>();
                        group.add(prerequisite);
                        group_id++;
                    } else {
                        group.add(prerequisite);
                    }
                }
            } else {
                return true;
            }
            stmt.close();
            rs.close();

            //得到学生的选课清单
            PreparedStatement ps = con.prepareStatement(
                    "select course_id from students s " +
                            "     inner join student_grades_hundred sgh on s.id = sgh.student_id " +
                            "        inner join sections s2 on s2.id = sgh.section_id " +
                            " where s.id = ? " +
                            " union " +
                            " select course_id " +
                            " from students s " +
                            "     inner join student_grades_pf sgp on s.id = sgp.student_id " +
                            "        inner join sections s2 on s2.id = sgp.section_id " +
                            " where s.id = ?;");
            ps.setInt(1, studentId);
            ps.setInt(2, studentId);
            ResultSet rs2 = ps.executeQuery();

            ArrayList<String> selected = new ArrayList<>();

            String courseTemp;
            if (rs2.next()) {
                courseTemp = rs2.getString(1);
                selected.add(courseTemp);
                while (rs2.next()) {
                    courseTemp = rs2.getString(1);
                    selected.add(courseTemp);
                }
            } else {
                ps.close();
                rs2.close();
                return false;
            }

            ps.close();
            rs2.close();

            //挨个比对prerequisites各个组与selected的内容
            //只要某个组里面的所有元素都在selected中有出现，就返回true，如果所有组都没有满足的，返回false
            boolean isSatisfied = false;
            boolean isSatisfiedPartly;
            for (ArrayList<String> t : prerequisites) {
                isSatisfiedPartly = true;
                for (String s : t) {
                    if (!selected.contains(s)) {
                        isSatisfiedPartly = false;
                    }
                }
                if (isSatisfiedPartly) {
                    isSatisfied = true;
                    break;
                }
            }
            return isSatisfied;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public Major getStudentMajor(int studentId) {
        try (Connection con = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = con.prepareStatement(
                     "select mj.id, mj.name, dept.id, dept.name from (select major_id from students where id = ?) s " +
                             " join majors as mj on s.major_id = mj.id join department as dept on mj.dept_id = dept.id ")) {

            stmt.setInt(1, studentId);
            stmt.executeUpdate();
            ResultSet rs = stmt.executeQuery();

            Major major = new Major();
            Department dept = new Department();

            if (rs.next()) {
                major.id = rs.getInt(1);
                major.name = rs.getString(2);
                dept.id = rs.getInt(3);
                dept.name = rs.getString(4);
                major.department = dept;
            } else {
                rs.close();
                throw new EntityNotFoundException();
            }
            rs.close();

            return major;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }
}
