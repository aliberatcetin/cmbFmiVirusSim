package core;

import java.util.*;

public class Student {

    PriorityQueue<Course> coursesHeap = new PriorityQueue<>();

    private Set<Course> courseSet = new HashSet<>();

    private int maximumWaitBeforeLectureStarts = 180;

    public DTNHostStudent getHost() {
        return host;
    }

    public void setHost(DTNHostStudent host) {
        this.host = host;
    }

    private DTNHostStudent host;

    private final int maxClasses = 5;

    public Student() {

    }

    public boolean hasAlreadyTakenTheCourse(Course course) {
        return courseSet.contains(course);
    }

    public int getMaximumWaitBeforeLectureStarts() {
        return maximumWaitBeforeLectureStarts;
    }

    public void addCourse(Course course) {
        coursesHeap.add(course);
        courseSet.add(course);
    }

    public void removeCurrentLecture(){
        coursesHeap.poll();
    }

    public boolean hasUpcomingLecture(int simTime){
        return hasLectureToday() && coursesHeap.peek().getTimeStartSimulation() < simTime + maximumWaitBeforeLectureStarts;
    }

    public Course getUpcomingLecture(){
        return coursesHeap.peek();
    }

    public boolean didReachMaxCourses() {
        return coursesHeap.size() >= maxClasses;
    }

    public PriorityQueue<Course> getCoursesHeap() {
        return coursesHeap;
    }

    public void removeEarliestClass(){
        coursesHeap.poll();
    }

    public boolean hasLectureToday(){
        return coursesHeap.size()>0;
    }
}
