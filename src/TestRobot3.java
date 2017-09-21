import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.*;

public class TestRobot3
{
    private double robotHeading, margin;
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
        this.margin = 0.00000001f;

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

        LocalizationResponse lr = new LocalizationResponse();

        path = SetRobotPath("./input/Path-around-table.json");

        Position nextPosition;

        while(!pathQueue.isEmpty()){
            robotcomm.getResponse(lr);
            double[] coordinates = getPosition(lr);
            robotHeading = getHeadingAngle(lr);
            robotPosition = new Position(coordinates[0], coordinates[2]);

            nextPosition = pathQueue.peek();

            MoveRobotToPosition(nextPosition, robotHeading);

            //System.out.println("Robot: " + robotPosition.getX() + "." +
            //        robotPosition.getY());
            //System.out.println("Next: " + nextPosition.getX() + "." +
            //        nextPosition.getY());
            System.out.println("Heading: "+ robotHeading);

            if(Math.abs(robotPosition.getDistanceTo(nextPosition)) <= margin){
                pathQueue.pollLast();
                System.out.println("Robot moved super good!");
            } else {
                System.out.println("Robot wont move good. ");
            }

            //System.out.println("Position: "+ robotPosition);

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
     * @return coordinates
     */
    double[] getPosition(LocalizationResponse lr)
    {
        return lr.getPosition();
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

        double angle = Math.atan2(Math.cos(distance), Math.sin(distance));

        DifferentialDriveRequest dr = new DifferentialDriveRequest();

        System.out.println("Angle: " + angle);

        dr.setLinearSpeed(0.1);
        dr.setAngularSpeed(Math.toRadians(angle));

        return dr;
    }

}

