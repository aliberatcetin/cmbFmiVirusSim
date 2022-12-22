package core;

import movement.MovementModel;
import movement.ScheduledStudentMovement;
import routing.MessageRouter;


import java.util.List;

public class DTNHostStudent extends DTNHost{

    public void setDTNHostState(ScheduledStudentMovement.StudentState DTNHostState) {
        this.DTNHostState = DTNHostState;
    }

    private ScheduledStudentMovement.StudentState DTNHostState = ScheduledStudentMovement.StudentState.INITIAL;
    private Coord realCoordinate;

    private Student student;

    public boolean infected;

    private Coord dummyCoord;

    private SimScenario simScenario;

    public ScheduledStudentMovement.StudentState getDTNHostState(){
        return  DTNHostState;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public SimScenario getSimScenario() {
        return simScenario;
    }

    /**
     * Creates a new DTNHost.
     *
     * @param msgLs        Message listeners
     * @param movLs        Movement listeners
     * @param groupId      GroupID of this host
     * @param interf       List of NetworkInterfaces for the class
     * @param comBus       Module communication bus object
     * @param mmProto      Prototype of the movement model of this host
     * @param mRouterProto Prototype of the message router of this host
     */
    public DTNHostStudent(List<MessageListener> msgLs, List<MovementListener> movLs, String groupId, List<NetworkInterface> interf, ModuleCommunicationBus comBus, MovementModel mmProto, MessageRouter mRouterProto, SimScenario simScenario) {
        super(msgLs, movLs, groupId, interf, comBus, mmProto, mRouterProto);
        this.simScenario = simScenario;
    }

    public Coord getRealCoordinate() {
        return realCoordinate;
    }

    public void setRealCoordinate(Coord realCoordinate) {
        this.realCoordinate = realCoordinate;
    }

    public Coord getDummyCoord() {
        return dummyCoord;
    }

    public void setDummyCoord(Coord dummyCoord) {
        this.dummyCoord = dummyCoord;
    }

    public void setSimScenario(SimScenario simScenario) {
        this.simScenario = simScenario;
    }

}
