import java.io.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
/**
 * TestRobot interfaces to the (real or virtual) robot over a network connection.
 * It uses Java -> JSON -> HttpRequest -> Network -> DssHost32 -> Lokarria(Robulab) -> Core -> MRDS4
 *
 * @author thomasj
 */
public class TestRobot4
{
    private RobotCommunication robotcomm;  // communication drivers
    private Position[] path;
    private double[] position;
    private double angle;
    private double newPositionAngle;
    private DifferentialDriveRequest dr;

    private String host;
    private int port;
    private LinkedList pathQueue;
    /**
     * Create a robot connected to host "host" at port "port"
     * @param host normally http://127.0.0.1
     * @param port normally 50000
     */
    public TestRobot4(String host, int port)
    {
        this.host = host;
        this.port = port;
        this.pathQueue = new LinkedList<>();
    }

    /**
     * This simple main program creates a robot, sets up some speed and turning rate and
     * then displays angle and position for 16 seconds.
     * @param args         not used
     * @throws Exception   not caught
     */
    public static void main(String[] args) throws Exception
    {
        /*File pathFile = new File
                ("/Users/Aron/Documents/AI-Robot-Basic-master/input/Path" +
                        "-around-table.json");
        BufferedReader in = new BufferedReader(new InputStreamReader(
                new FileInputStream(pathFile)));
        ObjectMapper mapper = new ObjectMapper();
        // read the path from the file
        Collection <Map<String, Object>> data =
                (Collection<Map<String, Object>>) mapper.readValue(in, Collection.class);
        int nPoints = data.size();
        Position[] path = new Position[nPoints];
        int index = 0;
        for (Map<String, Object> point : data)
        {
            Map<String, Object> pose = (Map<String, Object>)point.get("Pose");
            Map<String, Object> aPosition = (Map<String, Object>)pose.get("Position");
            double x = (Double)aPosition.get("X");
            double y = (Double)aPosition.get("Y");
            path[index] = new Position(x, y);
            index++;
        }
        System.out.println("Creating Robot");
        TestRobot4 robot = new TestRobot4("http://127.0.0.1", 50000);*/

        System.out.println("Creating Robot4");
        TestRobot4 robot = new TestRobot4("http://127.0.0.1", 50000);
        robot.run();
    }


    private void run() throws Exception
    {
        robotcomm = new RobotCommunication(host, port);
        Position robotPosition;
        double distance = 0.30;
        RobotCommunication robotcomm = new RobotCommunication(host, port);
        pathQueue = SetRobotPath("./input/Path-around-table.json");
        LocalizationResponse lr = new LocalizationResponse(); // response
        boolean goToPositionSet = false;
        Position goToPosition = (Position)pathQueue.peek();

        do
        {
            dr = new DifferentialDriveRequest(); // request
            robotcomm.getResponse(lr); // ask the robot about its position and angle
            angle = getHeadingAngle(lr);
            System.out.println("heading = " + angle);
            position = getPosition(lr);
            System.out.println("position = " + position[0] + ", " +
                    position[1]);

            robotPosition = new Position(position);

            System.out.println("Distans till position är: " + robotPosition
                    .getDistanceTo((Position)pathQueue.peek()));

            if((robotPosition.getDistanceTo(goToPosition) <
                    distance)){

                goToPosition = (Position) pathQueue.pop();
            }
            else if (robotPosition.getDistanceTo(goToPosition)
                    > distance && !goToPositionSet){

                goToPositionSet = true;
            }
            else if (goToPositionSet){

                newPositionAngle = robotPosition.getBearingTo(goToPosition);
                moveRobot();
            }

        }while(robotPosition != path[path.length-1]);
        // set up request to stop the robot
        dr.setLinearSpeed(0);
        dr.setAngularSpeed(0);

    }

    private void moveRobot(){

        DifferentialDriveRequest dr = new DifferentialDriveRequest();

        if(((angle - newPositionAngle) < 2) && ((angle - newPositionAngle) >
                (-2))){
            dr.setLinearSpeed(0.4);
        }
        else if((angle - newPositionAngle) < 0){
            //dr.setLinearSpeed(0);
            dr.setAngularSpeed(0.5);
        }
        else if((angle - newPositionAngle) > 0){
            //dr.setLinearSpeed(0);
            dr.setAngularSpeed(-0.5);
        }
        try {
            robotcomm.putRequest(dr);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    LinkedList SetRobotPath(String filename){
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
                pathQueue.add(new Position(x, y));
                index++;
            }
            System.out.println("Found path. First position is: ");
            System.out.println(path[0].getX() + ":" + path[0].getY());
            return pathQueue;
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

        double angle = Math.atan2(e[3], e[0]);
        return angle;
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


}

