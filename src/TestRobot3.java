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

        setRobotMargins();

        Position nextPosition;

        while(!pathQueue.isEmpty()){
            robotcomm.getResponse(lr);
            robotHeading = getHeadingAngle(lr);
            robotPosition = getPosition(lr);

            nextPosition = pathQueue.peek();

            MoveRobotToPosition(nextPosition, robotHeading);

            if(robotPosition.getDistanceTo(nextPosition) <= linearMargin){
                pathQueue.pollLast();
                System.out.println("Robot moved super good!");
            } else {
                System.out.println("Robot wont move good. ");
            }
        }
    }

    Position[] SetRobotPath(String filename){
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

    void setRobotMargins(){
        double distanceBetweenPoints = 0;
        double angleBetweenPoints = 0;
        for(int i = 0 ; i < path.length - 1 ; i++){
            distanceBetweenPoints += path[i].getDistanceTo(path[i+1]);
            angleBetweenPoints += Math.abs(path[i].getBearingTo(path[i+1]));
        }

        distanceBetweenPoints /= path.length;
        angleBetweenPoints /= path.length;

        this.linearMargin = distanceBetweenPoints/4;
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

        double angle = 2 * Math.atan2(e[3], e[0]);
        return angle * 180 / Math.PI;
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

    private void MoveRobotToPosition(Position position,
                                     double robotHeading){
        
        DifferentialDriveRequest dr = CalculateDrive(position, robotHeading);

        try{
            robotcomm.putRequest(dr);
        } catch(Exception e){
            System.out.println("Moving robot failed. ");
        }
    }

    private DifferentialDriveRequest CalculateDrive(Position nextPosition,
                                                    double robotHeading){

        double distance = robotPosition.getDistanceTo(nextPosition);
        double bearing = robotPosition.getBearingTo(nextPosition);

        double angle;
        double speed;

        if(Math.abs(robotHeading) > Math.abs(bearing)-this.angularMargin){
            angle = (bearing - robotHeading)/240;
        } else {
            angle = 0;
        }

        speed = 0.3;

        /*
        if(angle > 1.5){
            speed = 0.1;
        } else if (angle > 1){
            speed = 0.3;
        } else if(angle > 0.5){
            speed = 0.5;
        } else if (angle > 0.1){
            speed = 1;
        } else {
            speed = 0.1;
        }
        */

        DifferentialDriveRequest dr = new DifferentialDriveRequest();

        System.out.println("Angle: " + angle);

        dr.setLinearSpeed(speed);
        dr.setAngularSpeed(angle);

        return dr;

    }

}

