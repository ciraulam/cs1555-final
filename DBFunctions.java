import java.sql.*; 
import java.text.*; 
import java.util.Random; 
import java.util.Scanner;

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
        
        public static void main(String[] args)
        { 
        String username, password;
	username = "mac365"; //This is your username in oracle
	password = "3916901"; //This is your password in oracle
	
	try{
	    // Register the oracle driver.  
	    DriverManager.registerDriver (new oracle.jdbc.driver.OracleDriver());
	    
	    //This is the location of the database.  This is the database in oracle
	    //provided to the class
	    String url = "jdbc:oracle:thin:@class3.cs.pitt.edu:1521:dbclass"; 
	    
	    //create a connection to DB on class3.cs.pitt.edu
	    connection = DriverManager.getConnection(url, username, password); 
              DBFunctions dbf1 = new DBFunctions();
              //dbf1.demo();
              dbf1.mattDemo();
	    
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

    private void mattDemo() {
        login("1", "password");
        //confirmFriends("2");
        createGroup("124", "skateboarderz", "skate or die", 100, "1");
        //sendMessageToUser("1", "let's skate", "2");
        displayMessages("1");
    }

    public void login(String username, String pwd) {
        try {
            query = "SELECT userID, password FROM profile WHERE userID = ? AND password = ?";
            prepStatement = connection.prepareStatement(query);
            prepStatement.setString(1, username);
            prepStatement.setString(2, pwd);
            resultSet = prepStatement.executeQuery();

            // need to set some sort of flag to show logged in
            if(!resultSet.isBeforeFirst()) {
                System.out.println("Failed login as " + username);    // login denied
            } else {
                // update last login to be now
                query = "UPDATE profile SET lastLogin = CURRENT_TIMESTAMP WHERE userID = ?";
                prepStatement = connection.prepareStatement(query);
                prepStatement.setString(1, username);
                prepStatement.executeUpdate();
                connection.commit();
                System.out.println("Successfully logged in as " + username);
            }
        } catch(SQLException sqle) {
            System.out.println(sqle.getMessage());
        }
    }

    public void confirmFriends(String userID) {
        try {
            query = "SELECT fromID, message FROM  pendingFriends WHERE toID = ?";
            prepStatement = connection.prepareStatement(query);
            prepStatement.setString(1, userID);
            resultSet = prepStatement.executeQuery();
            
            if(!resultSet.isBeforeFirst()) 
                System.out.println("No friend requests");

            int count = 1;
            while(resultSet.next()) {
                // print out line of dashes
                System.out.println(new String(new char[80]).replace("\0", "-"));
                System.out.println("Friend request #" + count++);
                System.out.println("From: " + resultSet.getString(1) );
                System.out.println("Message: \n\t" + resultSet.getString(2));
            }

            // print line seperating the last message
            System.out.println(new String(new char[80]).replace("\0", "-") + "\n");

            query = "SELECT gID, message FROM pendingGroupMembership WHERE userID = ?";
            prepStatement.setString(1, userID);
            resultSet = prepStatement.executeQuery();

            if(!resultSet.isBeforeFirst())
                System.out.println("No group invites");

            count = 1;
            while(resultSet.next()) {
                System.out.println(new String(new char[80]).replace("\0", "-"));
                System.out.println("Group request #" + count++);
                System.out.println("From group: " + resultSet.getString(1));
                System.out.println("Message: \n\t" + resultSet.getString(2));
            }
        } catch(SQLException sqle) {
            System.out.println(sqle.getMessage());
        }
        /////////////////////////
    }

    public void createGroup(String gID, String name, String description, int limit, String founder) {
        try {
            query = "INSERT INTO groups(gID, name, description, group_limit) VALUES(?, ?, ?, ?)";
            prepStatement = connection.prepareStatement(query);
            prepStatement.setString(1, gID);
            prepStatement.setString(2, name);
            prepStatement.setString(3, description);
            prepStatement.setInt(4, limit);
            prepStatement.executeUpdate();
            connection.commit();
            System.out.println("Successfully created group " + name);
            query = "INSERT INTO groupMembership(gID, userID, role) VALUES(?, ?, 'manager')";
            prepStatement = connection.prepareStatement(query);
            prepStatement.setString(1, gID);
            prepStatement.setString(2, founder);
            connection.commit();
            System.out.println("Added " + founder + " as manager");
        } catch(SQLException sqle) {
            System.out.println(sqle.getMessage());
        }
    }

    public void sendMessageToUser(String fromUsername, String message, String toUsername) {
        try {
            query = "INSERT INTO messages(fromID, message, toUserID, dateSent) VALUES(?, ?, ?, CURRENT_TIMESTAMP)";
            prepStatement = connection.prepareStatement(query);
            prepStatement.setString(1, fromUsername);
            prepStatement.setString(2, message);
            prepStatement.setString(3, toUsername);
            prepStatement.executeUpdate();
            connection.commit();
            System.out.println("Sent message from " + fromUsername + " to " + toUsername);
        } catch(SQLException sqle) {
            System.out.println(sqle.getMessage());
        }
    }

    public void displayMessages(String toUsername) {
        try {
            query = "SELECT fromID, message FROM messages WHERE toUserID = ?";
            prepStatement = connection.prepareStatement(query);
            prepStatement.setString(1, toUsername);
            resultSet = prepStatement.executeQuery();

            if(!resultSet.isBeforeFirst()) {
                System.out.println("No messages");
            } else {
                while(resultSet.next()) {
                    System.out.println(new String(new char[80]).replace("\0", "-"));
                    System.out.println("From: " + resultSet.getString(1));
                    System.out.println("Message: \n\t" + resultSet.getString(2));
                }
                System.out.println(new String(new char[80]).replace("\0", "-"));
            }
        } catch(SQLException sqle) {
            System.out.println(sqle.getMessage());
        }
    }

    public void searchForUser(String input) {
        try {
            query = "SELECT name FROM profile WHERE userID LIKE ?";
            prepStatement = connection.prepareStatement(query);
            prepStatement.setString(1, input);
            resultSet = prepStatement.executeQuery();

            if(!resultSet.isBeforeFirst()) 
                System.out.println("No results found");
            else {
                while(resultSet.next()) {
                    System.out.println("Found user " + resultSet.getString(1));
                }
            }
        } catch(SQLException sqle) {
            System.out.println(sqle.getMessage());
        }
    }

    public void topMessages(int months, int numRows) {
        try {
            query = "SELECT messageRecipient.userID, count(*) FROM (SELECT messageRecipient.userID, count(*) FROM messageRecipient JOIN messages ON messageRecipient.userID = messages.toUserID WHERE messages.date > DATEADD(month, -?, CURRENT_TIMESTAMP) GROUP BY messageRecipient.userID) WHERE rownum < ?";
            prepStatement = connection.prepareStatement(query);
            prepStatement.setInt(1, months);
            prepStatement.setInt(2, numRows);
            resultSet = prepStatement.executeQuery();
            
            if(!resultSet.isBeforeFirst())
                System.out.println("No results");
            else {
                int count = 1;
                while(resultSet.next()) {
                    System.out.println("Rank: " + count + " UserID: " + resultSet.getString(1) + " # of messages: " + resultSet.getInt(2));
                }
            }
        } catch(SQLException sqle) {
            System.out.println(sqle.getMessage());
        }
    }

    public void logout(String userID) {
        try {
            query = "UPDATE profile SET lastLogout CURRENT_TIMESTAMP WHERE userID = ?";
            prepStatement = connection.prepareStatement(query);
            prepStatement.setString(1, userID);
            prepStatement.executeUpdate();
            System.out.println("Logged userID: " + userID + " out");
        } catch(SQLException sqle) {
            System.out.println(sqle.getMessage());
        }
    }
}
