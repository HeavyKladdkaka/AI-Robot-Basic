import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.*;

public class TestRobot3
{
    private double robotHeading, linearMargin, angularMargin;
    private int port;
    private String host;
    private RobotCommunication robotcomm;
    private Position[] path;
    private LinkedList<Position> pathQueue;
    private Position robotPosition;

    /**
     * Create a robot connected to host "host" at port "port"
     * @param host normally http://127.0.0.1
     * @param port normally 50000
     */
    public TestRobot3(String host, int port)
    {
        this.host = host;
        this.port = port;

        pathQueue = new LinkedList<Position>();
    }

    /**
     * This simple main program creates a robot, sets up some speed and turning rate and
     * then displays angle and position for 16 seconds.
     * @param args         not used
     * @throws Exception   not caught
     */
    public static void main(String[] args) throws Exception
    {
        System.out.println("Creating Robot3");
        TestRobot3 robot = new TestRobot3("http://127.0.0.1", 50000);
        robot.run();
    }


    public void run() throws Exception
    {
        robotcomm = new RobotCommunication(host, port);

        LocalizationResponse lr = new LocalizationResponse();

        path = SetRobotPath("./input/Path-around-table.json");

        SetRobotMargins();

        for(int i = 0 ; i < path.length ; i++){
            robotcomm.getResponse(lr);
            robotHeading = getHeadingAngle(lr);
            robotPosition = getPosition(lr);

            MoveRobotToPosition(robotHeading, i);

            System.out.println("Steps left: " + (path.length - i));

        }

        MoveRobotToFinalPosition(robotHeading);

        HaltRobotMovement();

        System.out.println("Robot is done. ");

    }

    private Position[] SetRobotPath(String filename){
        File pathFile = new File(filename);
        try{
            BufferedReader in = new BufferedReader(new InputStreamReader
                    (new FileInputStream(pathFile)));

            ObjectMapper mapper = new ObjectMapper();

            // read the path from the file
            Collection<Map<String, Object>> data =
                    (Collection<Map<String, Object>>) mapper.readValue
                            (in, Collection.class);
            int nPoints = data.size();
            path = new Position[nPoints];
            int index = 0;
            for (Map<String, Object> point : data)
            {
                Map<String, Object> pose =
                        (Map<String, Object>)point.get("Pose");
                Map<String, Object> aPosition =
                        (Map<String, Object>)pose.get("Position");
                double x = (Double)aPosition.get("X");
                double y = (Double)aPosition.get("Y");
                path[index] = new Position(x, y);
                pathQueue.add(path[index]);
                index++;
            }
            System.out.println("Found path. First position is: ");
            System.out.println(path[0].getX() + ":" + path[0].getY());
            return path;
        } catch(FileNotFoundException e){
            System.out.println("File not found. ");
        } catch(IOException e){
            System.out.println("IOException. ");
        }
        System.out.println("Null path");
        return null;
    }

    private void SetRobotMargins(){
        double distanceBetweenPoints = 0;
        double angleBetweenPoints = 0;
        for(int i = 0 ; i < path.length - 1 ; i++){
            distanceBetweenPoints += path[i].getDistanceTo(path[i+1]);
            angleBetweenPoints += Math.abs(path[i].getBearingTo(path[i+1]));
        }

        distanceBetweenPoints /= path.length;
        angleBetweenPoints /= path.length;

        this.linearMargin = distanceBetweenPoints;
        this.angularMargin = angleBetweenPoints/4;

        System.out.println("Path length: " + path.length);

        System.out.println("Average distance between points: " + distanceBetweenPoints);
        System.out.println("Linear Margin: " + this.linearMargin);

        System.out.println("Average bearing between points: " +
                angleBetweenPoints);
        System.out.println("Angular Margin: " + this.angularMargin);
    }

    /**
     * Extract the robot heading from the response
     * @param lr
     * @return angle in degrees
     */
    double getHeadingAngle(LocalizationResponse lr)
    {
        double e[] = lr.getOrientation();
        return Math.atan2(e[3], e[0]);
    }

    /**
     * Extract the position
     * @param lr
     * @return Position
     */
    Position getPosition(LocalizationResponse lr)
    {
        double[] coordinates = lr.getPosition();
        return new Position(coordinates[0], coordinates[2]);
    }

    private void MoveRobotToPosition(double robotHeading, int i){

        DifferentialDriveRequest dr;

        while(robotPosition.getDistanceTo(path[i]) > linearMargin) {

            dr = CalculateDrive(path[i+1], robotHeading);

            try {
                robotcomm.putRequest(dr);
            } catch (Exception e) {
                System.out.println("Sending drive request failed.");
            }
        }
    }

    private void MoveRobotToFinalPosition(double robotHeading){

        DifferentialDriveRequest dr;

        while(robotPosition.getDistanceTo(path[path.length - 1]) >
                linearMargin) {

            dr = CalculateDrive(path[path.length - 1], robotHeading);

            try {
                robotcomm.putRequest(dr);
            } catch (Exception e) {
                System.out.println("Sending drive request failed.");
            }
        }
    }

    private DifferentialDriveRequest CalculateDrive(Position nextPosition,
                                                    double robotHeading){

        double bearing = robotPosition.getBearingTo(nextPosition);

        double angle;
        double speed;

        angle = bearing - robotHeading;
        speed = 0.1;

        if(angle > Math.PI){
            angle -= 2 * Math.PI;
        } else if (angle < Math.PI){
            angle += 2 * Math.PI;
        }

        /*
        if(Math.abs(angle) <= this.angularMargin){
            angle = 0;
            speed = 0.3;
        } else if(Math.abs(angle) < Math.PI  + this.angularMargin/2 &&
                Math.abs(angle) > Math.PI  - this.angularMargin/2){

            angle = 0;
            speed = -0.3;
        }
        */

        DifferentialDriveRequest dr = new DifferentialDriveRequest();

        dr.setLinearSpeed(speed);
        dr.setAngularSpeed(angle);

        return dr;
    }

    private void HaltRobotMovement(){

        DifferentialDriveRequest dr = new DifferentialDriveRequest();
        dr.setLinearSpeed(0);
        dr.setAngularSpeed(0);

        try{
            robotcomm.putRequest(dr);
        } catch(Exception e){
            System.out.println("Sending halt drive request failed.");
        }
    }
}

