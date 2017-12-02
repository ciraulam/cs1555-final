DROP TABLE profile CASCADE CONSTRAINTS;
DROP TABLE friends CASCADE CONSTRAINTS;
DROP TABLE pendingFriends CASCADE CONSTRAINTS;
DROP TABLE messages CASCADE CONSTRAINTS;
DROP TABLE messageRecipient CASCADE CONSTRAINTS;
DROP TABLE groups CASCADE CONSTRAINTS;
DROP TABLE groupMembership CASCADE CONSTRAINTS;
DROP TABLE pendingGroupMembers CASCADE CONSTRAINTS;

CREATE TABLE profile (
	userID        varchar2(20),
	name          varchar2(50) NOT NULL,
	password      varchar2(50) NOT NULL,
        email	      varchar(15) NOT NULL,
	date_of_birth date,
	lastLogin     timestamp,
	CONSTRAINT pk_profile PRIMARY KEY(userID) DEFERRABLE INITIALLY IMMEDIATE,
        CONSTRAINT email_uq UNIQUE(email) DEFERRABLE INITIALLY IMMEDIATE
);

CREATE TABLE friends (
	userID1 varchar2(20),
	userID2 varchar2(20),
	JDate   date          NOT NULL,
	message varchar2(200) DEFAULT NULL,
	CONSTRAINT pk_friends PRIMARY KEY(userID1, userID2) DEFERRABLE INITIALLY IMMEDIATE,
	CONSTRAINT fk_friends_userID1 FOREIGN KEY(userID1) REFERENCES profile(userID) ON DELETE CASCADE,
	CONSTRAINT fk_friends_userID2 FOREIGN KEY(userID2) REFERENCES profile(userID) ON DELETE CASCADE
);

CREATE TABLE pendingFriends (
	fromID  varchar2(20),
	toID    varchar2(20),
	message varchar2(200) DEFAULT NULL,
	CONSTRAINT pk_pending_friends PRIMARY KEY(fromID, toID) DEFERRABLE INITIALLY IMMEDIATE,
        --I CANT CALL THESE WITHOUT GETTING AN ERROR! HELP.
	CONSTRAINT fk_pendingFriends_fromID FOREIGN KEY(fromID) REFERENCES profile(userID) ON DELETE CASCADE,
	CONSTRAINT fk_pendingFriends_toID FOREIGN KEY(toID) REFERENCES profile(userID) ON DELETE CASCADE   
);

CREATE TABLE groups (
	gID         varchar2(20),
	name        varchar2(20)  NOT NULL,
	description varchar2(200) DEFAULT NULL,
    group_limit number        NOT NULL,
	CONSTRAINT pk_groups PRIMARY KEY(gID) DEFERRABLE INITIALLY IMMEDIATE
);

CREATE TABLE messages (
	msgID     varchar2(20),
	fromID    varchar2(20),
	message   varchar2(200) DEFAULT NULL,
	toGroupID varchar2(20)  DEFAULT NULL,
	dateSent  date          NOT NULL,
	CONSTRAINT pk_messages PRIMARY KEY(msgID) DEFERRABLE INITIALLY IMMEDIATE,
        CONSTRAINT fk_messages_fromID FOREIGN KEY(fromID) REFERENCES profile(userID) ON DELETE SET NULL,
	CONSTRAINT fk_messages_toGroupID FOREIGN KEY(toGroupID) REFERENCES groups(gID)  ON DELETE SET NULL
);

CREATE TABLE messageRecipient (
	msgID  varchar2(20),
	userID varchar2(20),
	CONSTRAINT pk_messageRecipient PRIMARY KEY(msgID, userID) DEFERRABLE INITIALLY IMMEDIATE,
	--NOTE: I used both msgID and userID for the pk in case one message could be sent to multiple people via a group
	CONSTRAINT fk_messageRecipient_userID FOREIGN KEY(userID) REFERENCES profile(userID)  ON DELETE CASCADE
);

CREATE TABLE groupMembership (
	gID   varchar2(20),
	userID varchar2(20),
	role   varchar2(20) DEFAULT 'member', --assuming this is the default vaule for role
	CONSTRAINT pk_groupMembership PRIMARY KEY(gID, userID) DEFERRABLE INITIALLY IMMEDIATE,
	CONSTRAINT fk_groupMembership_gID FOREIGN KEY(gID) REFERENCES groups(gID)  ON DELETE CASCADE,
	CONSTRAINT fk_groupMembership_userID FOREIGN KEY(userID) REFERENCES profile(userID)  ON DELETE CASCADE
);

CREATE TABLE pendingGroupMembers (
	gID      varchar2(20),
	userID  varchar2(20),
	message varchar2(200) DEFAULT NULL,
	CONSTRAINT pk_pendingGroupMembers PRIMARY KEY(gID, userID) DEFERRABLE INITIALLY IMMEDIATE,
	CONSTRAINT fk_pendingGroupMembers_gID FOREIGN KEY(gID) REFERENCES groups(gID)  ON DELETE CASCADE,
	CONSTRAINT fk_pendingGroupMembers_userID FOREIGN KEY(userID) REFERENCES profile(userID) ON DELETE CASCADE
);

-- a trigger to increment the userID and insert it whenever a new user is added to the profile table 
DROP SEQUENCE user_sequence;
CREATE SEQUENCE user_sequence
   start with 1; 
CREATE OR REPLACE TRIGGER increment_userID 
BEFORE INSERT ON profile 
FOR EACH ROW
BEGIN 
   SELECT user_sequence.nextval INTO :new.userID FROM dual; 
END; 
/

--make sure that each friendship is unique, and that noone is friends with themselves
CREATE OR REPLACE TRIGGER friendship_unique
BEFORE INSERT ON friends
FOR EACH ROW
DECLARE
   it_exists NUMBER; 
BEGIN 
   SELECT COUNT(*)
   INTO it_exists 
   FROM (
      SELECT userID1, userID2
	  FROM friends
	  WHERE userID1 = :new.userID2     --basically flip flop the userID's to see if its already in the table
	     AND userID2 = :new.userID1
	); 
	
	IF it_exists > 0 THEN
	    raise_application_error(-20001, 'the two users are already friends'); 
    ELSIF :new.userID1 = :new.userID2 THEN
	    raise_application_error(-20002, 'users cannot be friends with themselves'); 
    END IF; 
END; 
/

--trigger to automatically increment the messageID for each tuple
DROP SEQUENCE message_sequence;
CREATE SEQUENCE message_sequence;
CREATE OR REPLACE TRIGGER message_increment
BEFORE INSERT ON messages
FOR EACH ROW
BEGIN
   SELECT message_sequence.nextval INTO :new.msgID FROM dual; 
END;
/

--make sure groups do not go over their enroll limit 
CREATE OR REPLACE TRIGGER group_limit
BEFORE INSERT ON groupMembership
FOR EACH ROW
DECLARE 
   g_count NUMBER;
   g_limit NUMBER; 
BEGIN 
   SELECT COUNT(*)
   INTO g_count
   FROM (
      SELECT gID
	  FROM groupMembership
	  WHERE groupMembership.gID = :new.gID
	 );
	 
	 SELECT group_limit
	 INTO g_limit 
	 FROM groups 
	 WHERE groups.gID = :new.gID; 
	 
	 IF g_count = g_limit THEN
	    raise_application_error(-20003, 'the enrollment limit has been reached for this group'); 
	END IF;
END; 
/

--delete from pendingFriends table once someone is accepted as a friend
CREATE OR REPLACE TRIGGER accept_friend
AFTER INSERT ON friends
FOR EACH ROW
BEGIN
   DELETE FROM pendingFriends
   WHERE toID = :new.userID1 AND fromID = :new.userID2;
   
   DELETE FROM pendingFriends
   WHERE toID = :new.userID2 AND fromID = :new.userID1;
END;
/	 

--delete from pendingGroupMembers when someone is accepted as a group member
CREATE OR REPLACE TRIGGER accept_group_member
AFTER INSERT ON groupMembership
FOR EACH ROW
BEGIN
   DELETE FROM pendingGroupMembers
   WHERE pendingGroupMembers.gID = :new.gID AND pendingGroupMembers.userID = :new.userID;
END; 
/

--insert into messageRecipient when a message is sent to a group 
CREATE OR REPLACE TRIGGER group_message
BEFORE INSERT ON messages
FOR EACH ROW
DECLARE 
   ID varchar(20); 
BEGIN
   IF :new.toGroupID IS NOT NULL THEN
     SELECT userID INTO ID 
     FROM groupMembership
     WHERE groupMembership.gID = :new.toGroupID; 
     
     INSERT INTO messageRecipient(msgID, userID) VALUES (:new.msgID, ID);
   END IF;
END;
/

--delete user from groups once they are deleted from the system
CREATE OR REPLACE TRIGGER delete_group_member
BEFORE DELETE ON profile
FOR EACH ROW
BEGIN
   DELETE FROM groupMembership 
   WHERE userID = :old.userID; 
END; 
/

--delete a message only when both users have been deleted from the system
CREATE OR REPLACE TRIGGER delete_messages
BEFORE DELETE ON profile
FOR EACH ROW 
DECLARE 
   toID varchar2(20); 
   from_ID varchar2(20); 

BEGIN
   UPDATE messages SET fromID = NULL WHERE fromID = :old.userID; 
   UPDATE messageRecipient SET userID = NULL WHERE userID = :old.userID; 

  DELETE FROM messages WHERE EXISTS (SELECT 'x' FROM messages NATURAL JOIN messageRecipient WHERE messages.fromID = NULL AND messageRecipient.userID = NULL ); 
  
END;
/

   