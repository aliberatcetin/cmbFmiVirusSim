package movement;

import core.*;
import input.WKTMapReader;
import movement.map.DijkstraPathFinder;
import movement.map.MapNode;
import movement.map.SimMap;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;


public class ScheduledStudentMovement extends MapBasedMovement {

    public enum StudentState {
        INITIAL, IDLE, CLASS, NON_LECTURE_POINT, DONE, MOVING
    }

    private int nrofMapFilesRead = 0;

    private static SimMap cachedMap = null;
    /**
     * names of the previously cached map's files (for hit comparison)
     */
    private static List<String> cachedMapFiles = null;

    private Coord destination;
    private long waitTime = rng.nextLong(0,100);
    private SimMap map = null;
    private DijkstraPathFinder pathFinder;

    private StudentState state = StudentState.INITIAL;

    private StudentState prevState = null;

    private Coord offset;

    private StudentState nextState = null;
    List<Coord> fmiGatePoints;
    List<Coord> nonLecturePoints;
    List<Coord> lectureHalls;

    public static final String SCHULED_STUDENT_MOVEMENT_NS = "ScheduledStudentMovement";


    public ScheduledStudentMovement(final Settings settings) {
        super(settings);
        map = readMap();
        maxPathLength = 100;
        minPathLength = 10;
        backAllowed = false;
        pathFinder = new DijkstraPathFinder(null);
        Settings scheduledMovementSettings = new Settings(SCHULED_STUDENT_MOVEMENT_NS);
        fmiGatePoints = readPointFile(scheduledMovementSettings.getSetting("gatePoints"));
        lectureHalls = readPointFile(scheduledMovementSettings.getSetting("lectureHalls"));
        nonLecturePoints = readPointFile(scheduledMovementSettings.getSetting("nonLecturePoints"));
    }

    private SimMap checkCache(Settings settings) {
        int nrofMapFiles = settings.getInt(NROF_FILES_S);

        if (nrofMapFiles != cachedMapFiles.size() || cachedMap == null) {
            return null; // wrong number of files
        }

        for (int i = 1; i <= nrofMapFiles; i++) {
            String pathFile = settings.getSetting(FILE_S + i);
            if (!pathFile.equals(cachedMapFiles.get(i - 1))) {
                return null;    // found wrong file name
            }
        }

        // all files matched -> return cached map
        return cachedMap;
    }


    public SimMap readMap() {
        SimMap simMap;
        Settings settings = new Settings(MAP_BASE_MOVEMENT_NS);
        WKTMapReader r = new WKTMapReader(true);

        if (cachedMap == null) {
            cachedMapFiles = new ArrayList<String>(); // no cache present
        } else { // something in cache
            // check out if previously asked map was asked again
            SimMap cached = checkCache(settings);
            if (cached != null) {
                nrofMapFilesRead = cachedMapFiles.size();
                return cached; // we had right map cached -> return it
            } else { // no hit -> reset cache
                cachedMapFiles = new ArrayList<String>();
                cachedMap = null;
            }
        }

        try {
            int nrofMapFiles = settings.getInt(NROF_FILES_S);

            for (int i = 1; i <= nrofMapFiles; i++) {
                String pathFile = settings.getSetting(FILE_S + i);
                cachedMapFiles.add(pathFile);
                r.addPaths(new File(pathFile), i);
            }

            nrofMapFilesRead = nrofMapFiles;
        } catch (IOException e) {
            throw new SimError(e.toString(), e);
        }

        simMap = r.getMap();
        // mirrors the map (y' = -y) and moves its upper left corner to origo
        simMap.mirror();
        offset = simMap.getMinBound().clone();
        simMap.translate(-offset.getX(), -offset.getY());

        cachedMap = simMap;
        return simMap;
    }

    protected List<Coord> readPointFile(String pointFile) {
        WKTMapReader reader = new WKTMapReader(true);
        List<Coord> coords = new ArrayList<>();
        try {
            coords = reader.readPoints(new File(pointFile));
        } catch (Exception e) {
            throw new SimError(e.toString(), e);
        }
        for(int i=0;i<coords.size();i++){
            Coord coord = coords.get(i);
            coord.setLocation(coord.getX(), -coord.getY());
            coord.translate(-offset.getX(), -offset.getY());
        }

        return coords;
    }

    public Path startMoving(DTNHostStudent dtnHostStudent, MapNode destinationNode) {
        if (destinationNode != null && destinationNode.getLocation().compareTo(dtnHostStudent.getLocation()) != 0) {
            List<MapNode> nodes = pathFinder.getShortestPath(lastMapNode,
                    destinationNode);
            Path path = new Path(generateSpeed());
            for (MapNode node : nodes) {
                path.addWaypoint(node.getLocation());
            }
            destination = destinationNode.getLocation().clone();
            return path;
        }
        return null;
    }

    public Path handleInitialState(DTNHostStudent dtnHostStudent, Student student, int simTime) {

        if (simTime < 0 || waitTime>simTime) {
            return null;
        }
        MapNode destinationNode = null;
        if (!student.hasLectureToday()) {
            prevState = state;
            state = StudentState.MOVING;
            Coord nextLocation = nonLecturePoints.get(rng.nextInt(nonLecturePoints.size()));
            destinationNode = getClosestMapNode(nextLocation, this.getMap().getNodes());
            nextState = StudentState.NON_LECTURE_POINT;
            return startMoving(dtnHostStudent, destinationNode);
        }

        if (student.hasUpcomingLecture(simTime)) {
            prevState = state;
            state = StudentState.MOVING;
            Course nextCourse = student.getUpcomingLecture();
            Coord nextLocation = lectureHalls.get(nextCourse.getRoomIndex());
            destinationNode = getClosestMapNode(nextLocation, this.getMap().getNodes());
            nextState = StudentState.CLASS;
            return startMoving(dtnHostStudent, destinationNode);
        }else{
            prevState = state;
            state = StudentState.MOVING;
            Coord nextLocation = nonLecturePoints.get(rng.nextInt(nonLecturePoints.size()));
            destinationNode = getClosestMapNode(nextLocation, this.getMap().getNodes());
            nextState = StudentState.NON_LECTURE_POINT;
            return startMoving(dtnHostStudent, destinationNode);
        }
    }


    public Path handleClassState(DTNHostStudent dtnHostStudent, Student student, int simTime) {
        if (simTime < student.getUpcomingLecture().getTimeEndSimulation()) {
            return null;
        }

        state = StudentState.NON_LECTURE_POINT;
        student.getUpcomingLecture().emptySeat(dtnHostStudent.getDummyCoord());
        student.removeCurrentLecture();
        dtnHostStudent.setDummyCoord(null);
        prevState = StudentState.CLASS;
        nextState = null;
        return null;
    }

    public Path handleDoneState(DTNHostStudent dtnHostStudent, Student student, int simTime) {
        dtnHostStudent.getSimScenario().removeHost(dtnHostStudent);
        return null;
    }

    public Path handleNonlectureState(DTNHostStudent dtnHostStudent, Student student, int simTime) {

        if (!student.hasUpcomingLecture(simTime) && waitTime < simTime) {
            MapNode destinationNode = null;
            prevState = state;
            state = StudentState.MOVING;
            int goHomeDecision = rng.nextInt(10);

            if(goHomeDecision<10){
                Coord nextLocation = fmiGatePoints.get(rng.nextInt(fmiGatePoints.size()));
                destinationNode = getClosestMapNode(nextLocation, this.getMap().getNodes());
                nextState = StudentState.DONE;
                prevState = state;
                state = StudentState.MOVING;
                return startMoving(dtnHostStudent, destinationNode);
            }

            Coord nextLocation = nonLecturePoints.get(rng.nextInt(nonLecturePoints.size()));
            destinationNode = getClosestMapNode(nextLocation, this.getMap().getNodes());
            nextState = StudentState.NON_LECTURE_POINT;
            return startMoving(dtnHostStudent, destinationNode);
        }

        if (student.hasUpcomingLecture(simTime)) {
            MapNode destinationNode = null;
            prevState = state;
            state = StudentState.MOVING;
            Course nextCourse = student.getUpcomingLecture();
            Coord nextLocation = lectureHalls.get(nextCourse.getRoomIndex());
            destinationNode = getClosestMapNode(nextLocation, this.getMap().getNodes());
            nextState = StudentState.CLASS;
            return startMoving(dtnHostStudent, destinationNode);
        }
        return null;
    }


    public Path handleIdleState(DTNHostStudent dtnHostStudent, Student student, int simTime) {
        return null;
    }

    public Path handleMovingState(DTNHostStudent dtnHostStudent, Student student, int simTime) {
        if (dtnHostStudent.getLocation().getX() == destination.getX() && dtnHostStudent.getLocation().getY() == destination.getY()) {
            prevState = state;
            state = nextState;
            nextState = StudentState.NON_LECTURE_POINT;
            if (state == StudentState.NON_LECTURE_POINT) {
                waitTime = simTime + rng.nextInt(1, 3000);
            }
            else if(state == StudentState.DONE){
                return null;
            }
            else if(state == StudentState.CLASS){
                Coord randomSeat = dtnHostStudent.getStudent().getUpcomingLecture().takeRandomSeat();
                dtnHostStudent.setDummyCoord(randomSeat);
            }
        }
        return null;
    }

    public Path decideNewStateAndGetPath() {
        DTNHostStudent dtnHostStudent = (DTNHostStudent) this.getHost();

        Student student = dtnHostStudent.getStudent();
        int simTime = SimClock.getIntTime();
        dtnHostStudent.setDTNHostState(state);
        switch (state) {
            case DONE -> {
                return handleDoneState(dtnHostStudent, student, simTime);
            }
            case IDLE -> {
                return handleIdleState(dtnHostStudent, student, simTime);
            }
            case INITIAL -> {
                return handleInitialState(dtnHostStudent, student, simTime);
            }
            case NON_LECTURE_POINT -> {
                return handleNonlectureState(dtnHostStudent, student, simTime);
            }
            case CLASS -> {
                return handleClassState(dtnHostStudent, student, simTime);
            }
            case MOVING -> {
                return handleMovingState(dtnHostStudent, student, simTime);
            }
        }
        return null;
    }

    public List<Coord> readAndMirror(String settingName) {
        WKTMapReader r = new WKTMapReader(true);
        Settings scheduledMovementSettings = new Settings(SCHULED_STUDENT_MOVEMENT_NS);
        String file = scheduledMovementSettings.getSetting(settingName);
        List<Coord> points = new ArrayList<>();
        try {
            r.addPaths(new File(file), 1);
        } catch (IOException e) {
            throw new SimError(e.toString(), e);
        }

        SimMap simMap = r.getMap();
        //mirrors the map (y' = -y) and moves its upper left corner to origo
        simMap.mirror();
        Coord offset = simMap.getMinBound().clone();
        simMap.translate(-offset.getX(), -offset.getY());

        for (MapNode mapNode : simMap.getNodes()) {
            points.add(new Coord(mapNode.getLocation().getX(), mapNode.getLocation().getY()));
        }

        return points;
    }

    @Override
    public MapBasedMovement replicate() {
        return new ScheduledStudentMovement(this);
    }

    private static MapNode getClosestMapNode(Coord point, List<MapNode> nodes) {
        MapNode closestCoord = null;
        double minDistance = Double.POSITIVE_INFINITY;
        for (MapNode temp : nodes) {
            double distance = temp.getLocation().distance(point);
            if (distance < minDistance) {
                minDistance = distance;
                closestCoord = temp;
            }
        }
        return closestCoord;
    }

    @Override
    public Coord getInitialLocation() {
        Coord randomGatePoint = fmiGatePoints.get(rng.nextInt(fmiGatePoints.size()));
        MapNode closest = getClosestMapNode(randomGatePoint, this.getMap().getNodes());
        lastMapNode = closest;
        destination = closest.getLocation().clone();
        return destination;
    }

    @Override
    public Path getPath() {
        //return null;

        return decideNewStateAndGetPath();
    }

    public ScheduledStudentMovement(final ScheduledStudentMovement ssm) {
        super(ssm);
        this.map = ssm.map;
        this.minPathLength = ssm.minPathLength;
        this.maxPathLength = ssm.maxPathLength;
        this.backAllowed = ssm.backAllowed;
        this.pathFinder = ssm.pathFinder;
        this.fmiGatePoints = ssm.fmiGatePoints;
        this.nonLecturePoints = ssm.nonLecturePoints;
        this.lectureHalls = ssm.lectureHalls;
        this.state = ssm.state;
    }


}
