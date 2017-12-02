import java.sql.*; 
import java.text.*; 
import java.util.Random; 

public class DBFunctions{

    private static Connection connection; //used to hold the jdbc connection to the DB
    private Statement statement; //used to create an instance of the connection
    private PreparedStatement prepStatement; //used to create a prepared statement, that will be later reused
    private ResultSet resultSet; //used to hold the result of your query (if one
    // exists)
    private String query;  //this will hold the query we are using
    private SimpleDateFormat date_format = new SimpleDateFormat("yyyy-MM-dd");
    
        public void createUser(String name, String email, Date dob)
        {
            try {
                //generate a password 
                String chars = "abcdefghijklmnopqrstuvwxyz"; 
                String numbers = "0123456789";
                String symbols = "!@#$%^&*_=+-/.?<>)";
                String all_values = chars + numbers + symbols; 
                
                Random rand = new Random();
                char[] password = new char[10]; //9 characters will be the length of the password 
                for (int i = 0; i <password.length; i++)
                {
                    password[i] = all_values.charAt(rand.nextInt(all_values.length()));
                }
                
                String pw = new String(password);
                
                query = "INSERT INTO profile (name, password, email, date_of_birth, lastLogin) " + "VALUES (?,?,?,?, CURRENT_TIMESTAMP)";
                prepStatement = connection.prepareStatement(query); 
                prepStatement.setString(1, name); 
                prepStatement.setString(2, pw);
                prepStatement.setString(3, email);               
                prepStatement.setDate(4, dob); 

                
                prepStatement.executeUpdate(); 
                connection.commit();
                System.out.println("User '" + name + "' created.");
                System.out.println("with password " + pw);
            }
            catch(SQLException sqle)
            {
                System.out.println("error adding profile: " + sqle.getMessage());
            }
            finally
            {
                try{
                    if (prepStatement != null) prepStatement.close();
                }
                catch(SQLException sqle)
                {
                    System.out.println("createUser(): Can't close the statement. " + sqle.toString());
                }
            }
        }
        /*
        * initiates a friendship between two users from their 
        * userIDs. 
        * @param fromID the ID of the user sending the request
        * @param toID the ID of the user receiving the request
        * @param message the message to be sent along with the user request
        */
        public void initiateFriendship(String fromID, String toID, String message){
          
            try 
            { 
                    query = "INSERT INTO pendingFriends(fromID, toID, message) " + "VALUES (?,?, ?)";
                    
                    prepStatement = connection.prepareStatement(query); 
                    prepStatement.setString(1, fromID);
                    prepStatement.setString(2, toID);
                    prepStatement.setString(3, message);
                    prepStatement.executeUpdate(); 
               
                    connection.commit();        
            }
            catch(SQLException sqle)
            {
                System.out.println("error initiating friendship: " + sqle.getMessage());
            }
  
        }
        
  
        public void initiateAddingGroup(String userID, String gID, String message)
        {
           try 
            { 
                query = "INSERT INTO pendingGroupMembers(gID, userID, message) " + "VALUES (?,?,?)";
                    
                    prepStatement = connection.prepareStatement(query); 
                    prepStatement.setString(1, gID);
                    prepStatement.setString(2, userID);
                    prepStatement.setString(3, message);
                    prepStatement.executeUpdate(); 
               
                    connection.commit();        
            }
            catch(SQLException sqle)
            {
                System.out.println("error initiating group membership: " + sqle.getMessage());
            }
        }
        
        public void sendMessageToGroup(String gID, String userID, String message)
        {
            try
            {
                query = "INSERT INTO messages(fromID, message, toGroupID, datesent) " + "VALUES (?,?,?, CURRENT_TIMESTAMP)"; 
                prepStatement = connection.prepareStatement(query);
                prepStatement.setString(1, userID);
                prepStatement.setString(2, message);
                prepStatement.setString(3, gID);
                
                prepStatement.executeUpdate(); 
                connection.commit(); 
            }
            catch(SQLException sqle)
            {
                System.out.println("error sending group message: " + sqle.getMessage());
            }
        }
        
        /*
        * I'm going to return the resultSet of the query so that I can use it in a different function 
        */
        public ResultSet displayFriends(String user_ID)
        {
            try {
                query = "SELECT name, userID FROM friends, profile "+
                        "WHERE (userID1 = ? OR userID2 = ?) AND ((userID1 = ? AND userID2 = userID) OR (userID2 = ? AND userID1 = userID))"; 
                prepStatement = connection.prepareStatement(query); 
                prepStatement.setString(1, user_ID);
                prepStatement.setString(2, user_ID);
                prepStatement.setString(3, user_ID); 
                prepStatement.setString(4, user_ID); 
                
                resultSet = prepStatement.executeQuery(); 
                
                if (!resultSet.isBeforeFirst()) //user doesnt have any friends
                {
                    System.out.println("User #" + user_ID + " does not have any friends"); 
                }
                else //user has friends
                {
                    System.out.println("Friends of user # " + user_ID+ ": ");
                    while(resultSet.next())
                    {
                       System.out.print("Name: '" + resultSet.getString("name")+ "', "); 
                       System.out.print("User #: " + resultSet.getString("userID")); 
                    }
                }
             return resultSet;
            }
            catch (SQLException sqle)
            {
                System.out.println("error displaying friends: " + sqle.getMessage());
            }
            return null; 
        }
        
        public String threeDegrees(String user_ID1, String user_ID2)
        {
            String path = user_ID1 + "-> ";
            String person2 = "";
            String person3 = "";
            String person4 = "";
            try{
                resultSet = this.displayFriends(user_ID1); 
                while(resultSet.next())
                {
                    person2 = resultSet.getString("userID"); 
                    if (person2.equals(user_ID2))   //directly friends with the person (1 friendship)
                    {
                        path += person2; 
                        return path;  
                    }
                   ResultSet rs2 = this.displayFriends(person2); 
                   while(rs2.next())
                   {
                       person3 = resultSet.getString("userID"); 
                       if (person3.equals(user_ID2))    //two friendships
                       {
                           path += person2 + " -> " + person3;  
                           return path; 
                       }
                       ResultSet rs3 = this.displayFriends(person3); 
                       while (rs3.next())
                       {
                           person4 = rs3.getString("userID"); 
                           if (person4.equals(user_ID2))     //third and final friendship 
                           {
                               path+= person2 + "-> " + person3 + "-> " + person4; 
                               return path; 
                           }
                       }
                   }
                }
               
            }
            catch (SQLException sqle)
            {
                System.out.println("error finding three degrees: " + sqle.getMessage()); 
            }
            path = "No path exists between the two users within three degrees"; 
            return path; 
        }
        public void dropUser(String userID)
        {
            try {
                query = "DELETE profile WHERE userID = ?"; 
                prepStatement = connection.prepareStatement(query); 
                prepStatement.setString(1, userID);
                
                prepStatement.executeUpdate();
                connection.commit(); 
            }
            catch (SQLException sqle)
            {
                System.out.println("error deleting user: " + sqle.getMessage());
            }
        }
        public void displayNewMessages(String userID)
        {
            try
            {
                query = "SELECT fromID, message FROM messages NATURAL JOIN messageRecipient NATURAL JOIN profile WHERE messageRecipient.userID = ? AND messages.datesent > profile.lastLogin";
                prepStatement = connection.prepareStatement(query);
                prepStatement.setString(1, userID);
                
                resultSet = prepStatement.executeQuery(); 
                while(resultSet.next())
                {
                    String fromID = resultSet.getString("fromID");
                    String msg = resultSet.getString("message"); 
                    
                    System.out.println("User " + fromID + "sent: " + msg); 
                }
            }
            catch(SQLException sqle)
            {
                System.out.println("error displaying new messages: " + sqle.getMessage());
            }
        }
        public void demo()
        {
            //just a demo function to help me test out these functions
            Date dob = null;
            try{
                  dob = new Date(date_format.parse("1996-09-28").getTime()); 
            }
            catch(ParseException e)
            {
                System.out.println(e.getMessage());
            }
            
            this.createUser("Iyanna Buffaloe", "iyb7@pitt.edu", dob);
            this.createUser("Jimi Hendrix", "iljimi@pitt.edu", dob); 
            this.createUser("third person", "tp3@pitt.edu",dob); 
            this.createUser("person four", "prf@pitt.edu", dob);
            this.initiateFriendship("1", "2","hi");                 
            this.initiateFriendship("2", "3", "hey");
            this.initiateFriendship("1", "3", "wassup"); 
            this.initiateFriendship("2", "4", "hi");
            this.initiateFriendship("1", "4", "yoooo"); 
            this.initiateAddingGroup("1", "1", "hey");
        }
        
        public static void main(String[] args)
        { 
        String username, password;
	username = "iyb7"; //This is your username in oracle
	password = "3970115"; //This is your password in oracle
	
	try{
	    // Register the oracle driver.  
	    DriverManager.registerDriver (new oracle.jdbc.driver.OracleDriver());
	    
	    //This is the location of the database.  This is the database in oracle
	    //provided to the class
	    String url = "jdbc:oracle:thin:@class3.cs.pitt.edu:1521:dbclass"; 
	    
	    //create a connection to DB on class3.cs.pitt.edu
	    connection = DriverManager.getConnection(url, username, password); 
              DBFunctions dbf1 = new DBFunctions();
              dbf1.demo();
	    
	}
	catch(Exception Ex)  {
	    System.out.println("Error connecting to database.  Machine Error: " +
			       Ex.toString());
        }
	finally
	{
		/*
		 * NOTE: the connection should be created once and used through out the whole project;
		 * Is very expensive to open a connection therefore you should not close it after every operation on database
		 */
               try
               {
                   connection.close();
               }
	       catch(SQLException sqle)
               {
                   System.out.println(sqle.getMessage());
               }
	}
    }  
}
