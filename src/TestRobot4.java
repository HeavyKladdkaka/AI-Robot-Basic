import java.io.*;
import java.util.Collection;
import java.util.Iterator;
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
    /**
     * Create a robot connected to host "host" at port "port"
     * @param host normally http://127.0.0.1
     * @param port normally 50000
     */
    public TestRobot4(String host, int port)
    {
        this.host = host;
        this.port = port;
    }

    /**
     * This simple main program creates a robot, sets up some speed and turning rate and
     * then displays angle and position for 16 seconds.
     * @param args         not used
     * @throws Exception   not caught
     */
    public static void main(String[] args) throws Exception
    {
        File pathFile = new File
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
        TestRobot4 robot = new TestRobot4("http://127.0.0.1", 50000);

        robot.run(path);
    }


    private void run(Position[] path) throws Exception
    {
        Position robotPosition;
        double distance = 2;
        Position goToPosition;
        RobotCommunication robotcomm = new RobotCommunication(host, port);

        Thread responseThread = new Thread(){
            @Override
            public void run(){

                try {
                    LocalizationResponse lr = new LocalizationResponse(); // response
                    dr = new DifferentialDriveRequest(); // request
                    robotcomm.getResponse(lr); // ask the robot about its position and angle
                    angle = lr.getHeadingAngle();
                    System.out.println("heading = " + angle);
                    position = getPosition(lr);
                    System.out.println("position = " + position[0] + ", " +
                            position[1]);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        };

        responseThread.run();

        // THIS IS THE PLACE FOR THE ALGORITHM!
        //dr.setAngularSpeed(Math.PI * 0.25); // set up the request to move in
        // a circle
        //dr.setLinearSpeed(1.0);
        //int rc = robotcomm.putRequest(dr); // move
        int i = 0;
        do
        {

            robotPosition = new Position(position);

            if((robotPosition.getDistanceTo(path[i]) > distance) && (i != 0)){

                goToPosition = path[i-1];

                newPositionAngle = robotPosition.getBearingTo(goToPosition);

                moveRobot();

            }
            else if((robotPosition.getDistanceTo(path[i]) > distance) && (i
                    == 0)){

                throw new IllegalStateException("Bad look ahead distance, the" +
                        " look ahead distance is too short...");
            }

            i++;


        }while(robotPosition != path[path.length-1]);
        // set up request to stop the robot
        dr.setLinearSpeed(0);
        dr.setAngularSpeed(0);
        //rc = robotcomm.putRequest(dr);

    }

    void moveRobot(){

        if((angle - newPositionAngle) == 0){
            dr.setLinearSpeed(1.0);
        }
        else if((angle - newPositionAngle) < 0){
            dr.setAngularSpeed(-2);
        }
        else if((angle - newPositionAngle) > 0){
            dr.setAngularSpeed(2);
        }
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


}

