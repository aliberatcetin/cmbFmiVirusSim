package core;

import movement.StudentScheduler;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.*;

public class Course implements Comparable<Course> {
    private int maxSeats;
    private String roomName;
    private final Date timeStart;
    private final Date timeEnd;

    private Set<Coord> takenSeats = new HashSet<>();
    private Set<Coord> emptySeats = new HashSet<>();

    private long timeStartSimulation;
    private long timeEndSimulation;

    private int roomIndex;

    private String courseName;
    private List<Student> students = new ArrayList<>();

    public Course(JSONObject jo) throws ParseException {
        DateFormat formatter = new SimpleDateFormat(jo.getString("dateFormat"));
        timeStart = formatter.parse(jo.getString("timeStart"));
        timeEnd = formatter.parse(jo.getString("timeEnd"));
        roomIndex = jo.getInt("roomIndex");
        courseName = jo.getString("courseName");
        roomName = jo.getString("roomName");
        maxSeats = jo.getInt("maxSeats");
        timeStartSimulation = (timeStart.getTime() - StudentScheduler.simulationStartTime.getTime()) / 1000;
        timeEndSimulation = (timeEnd.getTime() - StudentScheduler.simulationStartTime.getTime()) / 1000;
    }

    public long getTimeStartSimulation() {
        return timeStartSimulation;
    }

    public long getTimeEndSimulation() {
        return timeEndSimulation;
    }

    public int getMaxSeats() {
        return maxSeats;
    }

    public void addStudent(Student student) {
        if (hasMoreSeats())
            students.add(student);
    }

    public boolean hasMoreSeats() {
        return students.size() < maxSeats;
    }

    @Override
    public int compareTo(Course o) {
        return (int) (o.timeStartSimulation - timeStartSimulation);
    }

    public int getRoomIndex() {
        return roomIndex;
    }

    public void addSeat(Coord coord) {
        emptySeats.add(coord);
    }

    public Coord takeRandomSeat() {
        Coord seat = emptySeats.stream().skip(new Random().nextInt(emptySeats.size())).findFirst().orElse(null);
        takenSeats.add(seat);
        return seat;
    }

    public void emptySeat(Coord coord) {
        emptySeats.add(coord);
        takenSeats.remove(coord);
    }
}

