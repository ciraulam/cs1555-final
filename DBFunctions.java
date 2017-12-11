import java.sql.*; 
import java.text.*; 
import java.util.Random; 
import java.util.Scanner;

/*
* DBFunctions contains the driver code as well as all the functions that needed to be implemented.
* Some of the instructions were unclear, so I had to make some assuptions. Theese assumptions are commented out 
* in the main function
* 
* MATT: just edit the switch statements to fit your functions in. 
*/

public class DBFunctions{

    private static Connection connection; //used to hold the jdbc connection to the DB
    private Statement statement; //used to create an instance of the connection
    private PreparedStatement prepStatement; //used to create a prepared statement, that will be later reused
    private ResultSet resultSet; //used to hold the result of your query (if one
    // exists)
    private String query;  //this will hold the query we are using
    private SimpleDateFormat date_format = new SimpleDateFormat("yyyy-MM-dd");
    private static String loggedInUser; //once the user is logged in, this is the string that keeps track of their ID
    private static boolean loggedIn = false; //used to keep track of which menu to use
    
    //returns a 1 on success, 0 on failute 
        public int createUser(String name, String email, java.util.Date dob)
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
                java.sql.Date sqlDate = new java.sql.Date(dob.getTime());
                prepStatement.setDate(4, sqlDate); 

                
                prepStatement.executeUpdate(); 
                connection.commit();
                System.out.println("User '" + name + "' created.");
                System.out.println("with userID " + this.getUserID(email)); 
                System.out.println("and password " + pw);
                return 1; 
            }
            catch(SQLException sqle)
            {
                System.out.println("There was an error adding the profile. ");
                
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
             return 0;
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
                    System.out.println("friendship request sent. "); 
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
                
                System.out.println("message successfully sent. ");
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
                if (!resultSet.isBeforeFirst()) 
                {
                    System.out.println("No friends found for user " + user_ID1);
                    return "";
                }
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
                
                System.out.println("user " + userID + " successfully deleted.");
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

                    System.out.println("User " + fromID);
                    System.out.println("sent: " + msg); 
                }
            }
            catch(SQLException sqle)
            {
                System.out.println("error displaying new messages: " + sqle.getMessage());
            }
        }
        
        //helper function to obtain a user's ID given their email
        public String getUserID(String email)
        {
            try {
            query = "SELECT userID FROM profile WHERE email = ?"; 
            prepStatement = connection.prepareStatement(query);   
            prepStatement.setString(1, email); 
            resultSet = prepStatement.executeQuery(); 
            
            resultSet.next(); 
            String userID = resultSet.getString("userID"); 
            
            return userID;
            }
            catch (SQLException sqle)
            {
                System.out.println("error obtaining userID: " + sqle.getMessage());
            }
            return null;
        }
        
        public String getName(String userID)
        {
            try {
            query = "SELECT name FROM profile WHERE userID = ?"; 
            prepStatement = connection.prepareStatement(query);   
            prepStatement.setString(1, userID); 
            resultSet = prepStatement.executeQuery(); 
            
            resultSet.next(); 
            String name = resultSet.getString("name"); 
            
            return name;
            }
            catch (SQLException sqle)
            {
                System.out.println("error obtaining userID: " + sqle.getMessage());
            }
            return null;
        }
        
        /*
        * a helper function that checks to see if the given user is a member of a given group
        * @param userID the userID in question 
        * @param groupID the groupID in question
        */
        public boolean isAGroupMember(String userID, String groupID)
        {
            try {
                query = "SELECT userID FROM groupMembership WHERE userID = ? AND gID = ?";
                prepStatement = connection.prepareStatement(query);
                prepStatement.setString(1, userID);
                prepStatement.setString(2, groupID);
                
                resultSet = prepStatement.executeQuery(); 
                if (resultSet.isBeforeFirst()) return true; 
                else return false; 
            }
            catch (SQLException sqle)
            {
                System.out.print("error checking for group member: " + sqle.getMessage());
            }
            return false; 
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
            Scanner kb = new Scanner(System.in);
       int choice = 0; 
       DBFunctions database = new DBFunctions();    //this is the object that will communicate with the database through the functions we wrote 
       
       System.out.println("Welcome to Social@Panther!"); 
       do
       {
        System.out.println("1. Create user");
        System.out.println("2. Log In"); 
        System.out.println("0.exit");
        System.out.print("choice number: ");

        choice = kb.nextInt(); 
        kb.nextLine();
           switch (choice) {
               case 0:
                   System.exit(0);
               case 1: //create user
                   int success = 0; 
                   while (success != 1)
                   {
                    System.out.print("Enter a name: ");
                    String name = kb.nextLine(); 
                    System.out.print("Enter an email: "); 
                    String email = kb.next(); 
                    kb.nextLine();
                    java.util.Date date2= null; 
                    SimpleDateFormat date_format = new SimpleDateFormat("yyyy-MM-dd");

                    while(true)
                    {
                     try
                     {   System.out.print("Enter a date of birth (yyyy-mm-dd format): "); 
                         String date = kb.next();
                         kb.nextLine();
                         date2 = date_format.parse(date);
                         break;
                     }
                     catch(ParseException e)
                     {
                         System.out.println("Invalid date. try again.");
                         System.out.println("\n");
                     }
                    }
                    success = database.createUser(name, email, date2);
                   }
                   break;
               case 2: //log in 
                   System.out.print("enter user ID: "); 
                   String userID = kb.next();
                   kb.nextLine();
                   System.out.print("enter password: "); 
                   String pw = kb.next(); 
                   kb.nextLine(); 
                   success = database.login(userID, pw);
                   if (success == 1)
                   {
                       loggedInUser = userID; 
                       loggedIn= true; 
                   }
                   break;
               default:
                   System.out.println("Invalid choice, try again");
                   break;
           }
           if (loggedIn)
           {
               int choice2 = -1; 
               do {
                System.out.println("1. Initiate a friendship");
                System.out.println("2. Confirm a friendship request");
                System.out.println("3. Display friends"); 
                System.out.println("4. Create a group");
                System.out.println("5. Add users to group"); 
                System.out.println("6. Send a message to a user");
                System.out.println("7. Send a message to a group"); 
                System.out.println("8. Display messages"); 
                System.out.println("9. Display new messages");
                System.out.println("10. Search for a user");
                System.out.println("11. Three degrees of seperation: find a path between two users");
                System.out.println("12. Display top messages");
                System.out.println("13. Delete this user profile");  //assuming that the only profile you can delete is your own 
                System.out.println("14. Log out"); 
                System.out.println("15. quit program");
                System.out.print("Select a menu option: ");
                choice2 = kb.nextInt(); 
                kb.nextLine(); 

                switch(choice2)
                {
                    case 1:  //initiate a friendship 
                        System.out.print("user ID of the other user: ");
                        String userID2 = kb.next(); 
                        kb.nextLine(); 
                        String name = database.getName(userID2); 
                        if (name == null) {
                            System.out.println("error obtaining user. "); 
                          }
                        else 
                        {
                            System.out.print("send friend request to " + name + "? (yes/no): ");
                            String ans = kb.next(); 
                            kb.nextLine();
                            if (ans.equals("yes"))
                            {

                                System.out.println("enter message to send with the request:");
                                String message = kb.nextLine(); 
                                if (message.length() > 200) message = message.substring(0, 200); 
                                database.initiateFriendship(loggedInUser, userID2, message);
                            }
                        }
                        break;
                    case 2: //confirm friendship 
                        break; 
                    case 3: //display friendships
                        ResultSet rs = database.displayFriends(loggedInUser); 

                        //display the friends of friends of the logged in user 
                        while(rs.next())
                        {
                            String friend = rs.getString("userID"); 
                            database.displayFriends(friend);
                        }
                        break;
                    case 4: //create a group 
                        break; 
                        
                        
                     /*
                      * the guidelines for adding a user to a group on the project description aren't very clear. 
                      * I'm going to assume that the "initiateAddingGroup" feature should ask the logged in user 
                      * for a group number to be added to, and send a request to the corresponding group along with 
                      * a message.
                      *  
                      * also, the project doesn't have any requirement to actually approve the group member request. 
                      *  
                      */
                    case 5: //initiate adding to a group 
                        System.out.print("Enter the group ID you would like to send a request to: ");
                        String groupID = kb.next(); 
                        kb.nextLine(); 

                        System.out.println("Enter the message you would like to send along with the request: "); 
                        String message = kb.nextLine(); 
                        if (message.length() > 200) message = message.substring(0, 200);
                        database.initiateAddingGroup(loggedInUser, groupID, message);

                        System.out.println("Request successfully sent."); 
                        break;
                    case 6: //send message to user 
                        break;
                    case 7: //send message to group
                        //first I have to make sure that the user is a part of the group they're trying to send a message to
                        System.out.println("Enter the group ID: "); 
                        String groupID2 = kb.next();
                        kb.nextLine(); 

                        boolean insert = database.isAGroupMember(loggedInUser, groupID2); 
                        if (insert)
                        {
                            System.out.println("Enter the message you would like to send to group " + groupID2 + ":");
                            String mess = kb.nextLine();
                            database.sendMessageToGroup(groupID2, loggedInUser, mess);
                        }
                        else 
                        {
                            System.out.println("error: you have to be a member of the group to send the group a message.");
                        }
                        break; 
                    case 8: //display messages 
                        break;
                    case 9:  //display new messages
                        database.displayNewMessages(loggedInUser);
                        break;
                    case 10: //search for user 
                        break;

                        /*
                        * the description for this function was unclear on if we're finding three degrees between any two users, 
                        * or three degrees between the logged in user and a given user. I'm going to assume its the latter
                        */
                    case 11: //three degrees 
                        System.out.println("this feature finds a path between you and a user of your choice, if a path exiss, within three degrees of separation.");
                        System.out.print("user ID: ");
                        String ID = kb.next(); 

                        database.threeDegrees(loggedInUser, ID); 

                        break; 
                    case 12: // top messages
                        break;
                        /*
                        * The assumption here is that you can only delete your own profile 
                        */
                    case 13: //drop user
                        System.out.print("Delete this user? (Yes/no): ");
                        String c = kb.next();

                        if (c.equals("yes"))
                        {
                            database.dropUser(loggedInUser);
                            //System.exit(0);
                        }
                        break; 
                    case 14: //log out
                        break;
                    case 15: //exit
                        System.exit(0);
                }
               }
            while (choice2 != 15);
           }
         }
       while (!((choice <= 0) && (choice > 2)));
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

/*****************************************************************************************************************************************************
**                                     ALL FUNCTIONS ABOVE THIS LINE WRITTEN BY IYANNA BUFFALOE                                                     **
**                                                                                                                                                  **
**                                     ALL FUNCTIONS BELOW THIS LINE WRITTEN BY MATTHEW CIRAULA                                                     **
*****************************************************************************************************************************************************/

    /* Demonstrates functions written by Matthew Ciraula
    */
    private void mattDemo() {
        login("1", "password");
        confirmFriends("2");
        createGroup("124", "new group", "this is new", 100, "1");
        sendMessageToUser("1", "hello there", "2");
        displayMessages("1");
        searchForUser("1");
        topMessages(2, 2);
        logout("1");
    }

    /* checks db for a matching userID and password combination
    ** if a match is found the last login value on their profile is updated
    ** @param userID: String representing userID
    ** @param pwd: String representing password
    */
    public int login(String userID, String pwd) {
        try {
            query = "SELECT userID, password FROM profile WHERE userID = ? AND password = ?";
            prepStatement = connection.prepareStatement(query);
            prepStatement.setString(1, userID);
            prepStatement.setString(2, pwd);
            resultSet = prepStatement.executeQuery();

            // need to set some sort of flag to show logged in
            if(!resultSet.isBeforeFirst()) {
                System.out.println("Failed login as " + userID);    // login denied
                return 0; 
            } else {
                // update last login to be now
                query = "UPDATE profile SET lastLogin = CURRENT_TIMESTAMP WHERE userID = ?";
                prepStatement = connection.prepareStatement(query);
                prepStatement.setString(1, userID);
                prepStatement.executeUpdate();
                connection.commit();
                System.out.println("Successfully logged in as " + userID);
                return 1;
            }
        } catch(SQLException sqle) {
            System.out.println(sqle.getMessage());
        }
        return 0;
    }

    /* prints out all pending friend and group requests
    ** @param userID: the userID of user to whom requests were sent
    */
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
    }

    /* creates a new group
    ** @param gID: group ID
    ** @param name: name of group
    ** @param description: brief description
    ** @param limit: maximum number of group members
    ** @param manager: userID of manager
    */
    public void createGroup(String gID, String name, String description, int limit, String manager) {
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
            // now make groupMembership entry for manager
            query = "INSERT INTO groupMembership(gID, userID, role) VALUES(?, ?, 'manager')";
            prepStatement = connection.prepareStatement(query);
            prepStatement.setString(1, gID);
            prepStatement.setString(2, manager);
            connection.commit();
            System.out.println("Added " + manager + " as manager");
        } catch(SQLException sqle) {
            System.out.println(sqle.getMessage());
        }
    }

    /* Sends a message
    ** @param fromUserID: sender's ID
    ** @param message: message text
    ** @param toUserID: recipient's ID
    */
    public void sendMessageToUser(String fromUserID, String message, String toUserID) {
        try {
            query = "INSERT INTO messages(fromID, message, toUserID, dateSent) VALUES(?, ?, ?, CURRENT_TIMESTAMP)";
            prepStatement = connection.prepareStatement(query);
            prepStatement.setString(1, fromUserID);
            prepStatement.setString(2, message);
            prepStatement.setString(3, toUserID);
            prepStatement.executeUpdate();
            connection.commit();
            System.out.println("Sent message from " + fromUserID + " to " + toUserID);
        } catch(SQLException sqle) {
            System.out.println(sqle.getMessage());
        }
    }

    /* displays all messages a user has recieved
    ** @param toUserID: userID of recipient
    */
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
                    System.out.println(new String(new char[80]).replace("\0", "-"));    // these are just lines
                    System.out.println("From: " + resultSet.getString(1));
                    System.out.println("Message: \n\t" + resultSet.getString(2));
                }
                System.out.println(new String(new char[80]).replace("\0", "-"));    // these are just lines
            }
        } catch(SQLException sqle) {
            System.out.println(sqle.getMessage());
        }
    }

    /* searches for user with a containing a specific string 
    ** @param input: string we are searching for
    */
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

    /* displays the users who have recieved the most messages
    ** @param months: how many months back are we starting from
    ** @param numRows: the number of users to include in output
    */
    public void topMessages(int months, int numRows) {
        try {
            query = "SELECT messageRecipient.userID, count(*) FROM (SELECT messageRecipient.userID, count(*) FROM messageRecipient JOIN messages ON messageRecipient.userID = messages.toUserID WHERE messages.date > DATEADD(month, -?, CURRENT_TIMESTAMP) GROUP BY messageRecipient.userID ORDER BY count(*) DESC) WHERE rownum < ?";
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

    /* sets last logout to now for given userID
    ** @param userID: user to log out
    */
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
