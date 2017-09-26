import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.*;

public class TestRobot3
{
    private double lookAheadDistance;
    private int port;
    private String host;
    private RobotCommunication robotcomm;
    private Position[] path;
    Position robotPosition;
    private LinkedList<Position> pathQueue;

    /**
     * Create a robot connected to host "host" at port "port"
     * @param host normally http://127.0.0.1
     * @param port normally 50000
     */
    public TestRobot3(String host, int port)
    {
        this.host = host;
        this.port = port;

        pathQueue = new LinkedList<>();
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

        path = SetRobotPath("./input/Path-around-table-and-back.json");
        //path = SetRobotPath("./input/Path-around-table.json");
        //path = SetRobotPath("./input/Path-to-bed.json");
        //path = SetRobotPath("./input/Path-from-bed.json");

        SetRobotMargins();

        for(int i = 1 ; i < path.length - 1; i++){
            MoveRobotToPosition(i);
            System.out.println("Steps left: " + (path.length - i));
        }

        SendDriveRequest(0,0);

        System.out.println("Robot is done.");

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

        for(int i = 0 ; i < path.length - 1; i++){
            distanceBetweenPoints += path[i].getDistanceTo(path[i+1]);
        }

        this.lookAheadDistance = (distanceBetweenPoints / path.length) * 20;

        System.out.println("Path length: " + path.length);
        System.out.println("Average distance between points: "
                + distanceBetweenPoints);
        System.out.println("Look Ahead Distance: " + this.lookAheadDistance);
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
        return new Position(coordinates[0], coordinates[1]);
    }

    private void MoveRobotToPosition(int i) throws Exception{

        LocalizationResponse lr = new LocalizationResponse();

        do {
            robotcomm.getResponse(lr);
            robotPosition = getPosition(lr);

            MoveRobot(path[i+1], getHeadingAngle(lr));

            robotcomm.getResponse(lr);
        }while(getPosition(lr).getDistanceTo(path[i]) > lookAheadDistance);
    }

    private void MoveRobot(Position nextPosition, double robotHeading){

        double bearing = robotPosition.getBearingTo(nextPosition);

        double angle;
        double speed;

        angle = bearing - robotHeading;
        speed = 1;

        if(angle > Math.PI){
            angle -= 2 * Math.PI;
        } else if (angle < -Math.PI){
            angle += 2 * Math.PI;
        }

        SendDriveRequest(speed, angle);
    }

    private void SendDriveRequest(double linearSpeed, double angularSpeed){
        DifferentialDriveRequest dr = new DifferentialDriveRequest();
        dr.setLinearSpeed(linearSpeed);
        dr.setAngularSpeed(angularSpeed);

        try{
            robotcomm.putRequest(dr);
        } catch(Exception e){
            System.out.println("Sending halt drive request failed.");
        }
    }
}

