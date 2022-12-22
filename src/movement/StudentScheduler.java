package movement;

import core.Coord;
import core.Course;
import core.Settings;
import core.Student;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class StudentScheduler {

    private List<Student> students = new ArrayList<>();
    private List<Course> courses = new ArrayList<>();

    private static Random rng;
    private String STUDENT_SCHEDULER_NS = "StudentScheduler";
    private String COURSES_JSON_FILE = "jsonScheduleFile";

    private String coursesJsonFile;

    public static Date simulationStartTime;

    public List<Student> getStudents() {
        return students;
    }

    public StudentScheduler() {
        Settings settings = new Settings(STUDENT_SCHEDULER_NS);
        this.coursesJsonFile = settings.getSetting(COURSES_JSON_FILE);
        rng = new Random(0);
        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd-HH:mm");
        try {
            simulationStartTime = formatter.parse("2022/12/02-08:00");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        readJson();
    }

    public void constructCoursesFromJsonArray(JSONArray ja) {
        for (int i = 0; i < ja.length(); i++) {
            JSONObject jo = ja.getJSONObject(i);
            try {
                courses.add(new Course(jo));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void addStudentAndCourseToEachOther(Student student, Course course) {
        course.addStudent(student);
        student.addCourse(course);
        students.add(student);
    }

    public void createSeatMap(Course course) {
        for (int i = 0; i < Math.sqrt(course.getMaxSeats()); i++) {
            for (int j = 0; j < Math.sqrt(course.getMaxSeats()); j++) {
                course.addSeat(new Coord(i * 20, j * 20));
            }
        }
    }

    public void createWorld() {
        for (Course course : courses) {
            createSeatMap(course);
            while (course.hasMoreSeats()) {
                Student student = new Student();
                addStudentAndCourseToEachOther(student, course);
            }

            for (int i = 0; i < students.size(); i++) {
                Student student = students.get(i);
                if (student.didReachMaxCourses())
                    continue;
                int random = rng.nextInt(100);
                if (random < 2) {
                    Course randomCourse = courses.get(rng.nextInt(courses.size()));
                    if (randomCourse.hasMoreSeats() && !student.hasAlreadyTakenTheCourse(randomCourse))
                        addStudentAndCourseToEachOther(student, randomCourse);
                }
            }
        }


        int studentsWithoutCourse = rng.nextInt(1, 2);
        for (int i = 0; i < 100; i++) {
            students.add(new Student());
        }
        System.out.println(students.size());
    }

    public void readJson() {
        File file = new File(coursesJsonFile);
        InputStream is = null;
        String json;
        try {
            is = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        try {
            json = IOUtils.toString(is, "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //JsonElement json = J.parseReader( new InputStreamReader(new FileInputStream("/someDir/someFile.json"), "UTF-8") );

        JSONArray ja = new JSONArray(json);
        constructCoursesFromJsonArray(ja);
        createWorld();
        System.out.println("");
    }
}
