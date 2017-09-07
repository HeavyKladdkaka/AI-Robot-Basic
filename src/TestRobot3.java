import javafx.geometry.Pos;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.util.Iterator;

public class TestRobot3
{
    private RobotCommunication robotcomm;  // communication drivers
    private Position[] path;

    /**
     * Create a robot connected to host "host" at port "port"
     * @param host normally http://127.0.0.1
     * @param port normally 50000
     */
    public TestRobot3(String host, int port)
    {
        robotcomm = new RobotCommunication(host, port);
    }

    /**
     * This simple main program creates a robot, sets up some speed and turning rate and
     * then displays angle and position for 16 seconds.
     * @param args         not used
     * @throws Exception   not caught
     */
    public static void main(String[] args) throws Exception
    {
        System.out.println("Creating Robot");
        TestRobot3 robot = new TestRobot3("http://127.0.0.1", 50000);
        robot.run();
    }


    public void run() throws Exception
    {


    }

    public void SetRobotPath(){
        JSONParser parser = new JSONParser();

        try {

            Object obj = parser.parse(new FileReader(
                    "./input/Path-around-table.json"));

            JSONObject jsonObject = (JSONObject) obj;

            JSONValue pose = (JSONValue) jsonObject.get("Pose");
            String author = (String) jsonObject.get("Author");
            JSONArray companyList = (JSONArray) jsonObject.get("Company List");
            JSONArray positions = (JSONArray) jsonObject.get("Position");

            System.out.println("Pose: " + pose);
            System.out.println("Author: " + author);
            System.out.println("\nCompany List:");

            Iterator<String> positionIterator = positions.iterator();
            while(positionIterator.hasNext()) {
                //Position p = new Position(positionIterator.next());
                System.out.println(positionIterator.next());
            }

            //path = positions.toArray();

            Iterator<String> iterator = companyList.iterator();
            while (iterator.hasNext()) {
                System.out.println(iterator.next());
            }

        } catch (Exception e) {
            e.printStackTrace();
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

