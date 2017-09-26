import java.io.*;
import java.util.*;

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
public class TestRobot4 {
    private RobotCommunication robotcomm;  // communication drivers
    private Position[] path;
    private double[] position;
    private double angle;
    private double newPositionAngle;
    private DifferentialDriveRequest dr;
    private double newSpeedAngle;
    private LaserEchoesResponse laser;

    private String host;
    private int port;
    private LinkedList<Position> pathQueue;

    /**
     * Create a robot connected to host "host" at port "port"
     *
     * @param host normally http://127.0.0.1
     * @param port normally 50000
     */
    public TestRobot4(String host, int port) {
        this.host = host;
        this.port = port;
        this.pathQueue = new LinkedList<>();
    }

    /**
     * This simple main program creates a robot, sets up some speed and turning rate and
     * then displays angle and position for 16 seconds.
     *
     * @param args not used
     * @throws Exception not caught
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Creating Robot4");
        TestRobot4 robot = new TestRobot4("http://127.0.0.1", 50000);
        robot.run();
    }

    /**
     *
     * @throws Exception
     */
    private void run() throws Exception {

        robotcomm = new RobotCommunication(host, port);
        pathQueue = SetRobotPath("./input/Path-around-table-and-back.json");
        LocalizationResponse lr = new LocalizationResponse();
        dr = new DifferentialDriveRequest(); // request

        Position goToPosition = pathQueue.peekFirst();
        Position robotPosition;
        double lookAheadDistance = 1.2;

        Date startTime = new Date();

        do {

            robotcomm.getResponse(lr); // ask the robot about its position and angle
            angle = lr.getHeadingAngle();
            position = lr.getPosition();

            robotPosition = new Position(position);

            if (robotPosition.getDistanceTo(goToPosition) <
                    lookAheadDistance) {

                goToPosition = pathQueue.poll();

            } else if (robotPosition.getDistanceTo(goToPosition)
                    > lookAheadDistance) {

                newPositionAngle = robotPosition.getBearingTo(goToPosition);
                moveRobot();
            }

        } while (!pathQueue.isEmpty() && pathQueue.peekLast().getDistanceTo
                (robotPosition) > 0.1);

        setWheelSpeed(0, 0);
        Date stopTime = new Date();
        System.out.println("Robot is within 1 meter from the last point in" +
                " the path. Seconds passed: " + ((stopTime.getTime()
                -startTime.getTime())/1000));
    }

    private void setWheelSpeed(double angularSpeed,double linearSpeed) {

        DifferentialDriveRequest dr = new DifferentialDriveRequest();

        dr.setAngularSpeed(angularSpeed);
        dr.setLinearSpeed(linearSpeed);

        try {
            robotcomm.putRequest(dr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void moveRobot() {

        DifferentialDriveRequest dr = new DifferentialDriveRequest();
        newSpeedAngle = newPositionAngle - angle;
        if (newSpeedAngle > Math.PI) {
            setWheelSpeed(newSpeedAngle - Math.PI * 2,1);
        } else if (newSpeedAngle < -Math.PI) {
            setWheelSpeed(newSpeedAngle + Math.PI * 2,1);
        } else if (newSpeedAngle == 0) {
            setWheelSpeed(0,0.5);
        } else {
            setWheelSpeed(newSpeedAngle,0.5);
        }
        /*if((newPositionAngle < Math.PI/4) && (newPositionAngle > (-Math.PI)
                /4)){
            if(newSpeedAngle > Math.PI){
                newSpeedAngle -= 2*Math.PI;
            }
            if(newSpeedAngle < (-Math.PI)){
                newSpeedAngle += 2*Math.PI;
            }
            dr.setAngularSpeed(newSpeedAngle/2);
            dr.setLinearSpeed(1);
        }
        else if(newPositionAngle - angle < 0){

            dr.setAngularSpeed(-0.2);
            dr.setLinearSpeed(0.1);
        }
        else if(newPositionAngle - angle > 0){

            dr.setAngularSpeed(0.2);
            dr.setLinearSpeed(0.1);
        }

        try {
            robotcomm.putRequest(dr);
        }catch(Exception e){
            e.printStackTrace();
       }*/
    }

    LinkedList SetRobotPath(String filename) {
        File pathFile = new File(filename);
        try {
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
            for (Map<String, Object> point : data) {
                Map<String, Object> pose =
                        (Map<String, Object>) point.get("Pose");
                Map<String, Object> aPosition =
                        (Map<String, Object>) pose.get("Position");
                double x = (Double) aPosition.get("X");
                double y = (Double) aPosition.get("Y");
                path[index] = new Position(x, y);
                pathQueue.add(new Position(x, y));
                index++;
            }
            System.out.println("Found path. First position is: ");
            System.out.println(path[0].getX() + ":" + path[0].getY());
            return pathQueue;
        } catch (FileNotFoundException e) {
            System.out.println("File not found. ");
        } catch (IOException e) {
            System.out.println("IOException. ");
        }
        System.out.println("Null path");
        return null;
    }
}

