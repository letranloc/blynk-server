# Blynk server

# Requirements
Java 8 required. (OpenJDK, Oracle)

# Protocol messages

Every message consists of 2 parts.
Header : Protocol command (1 byte), MessageId (2 bytes), Body message length (2 bytes);
Body : string (could be up to 2^15 bytes).
For instance, the value of the length field in this example is 3 bytes (0x0003) which represents the length of body "1 2".

	            	BEFORE DECODE (8 bytes)
	+------------------+------------+---------+------------------------+
	|       1 byte     |  2 bytes   | 2 bytes |    Variable length     |
	+------------------+------------+---------+------------------------+
	| Protocol Command |  MessageId |  Length |  Message body (String) |
	|       0x01       |   0x000A   |  0x0003 |         "1 2"          |
	+------------------+------------+---------+------------------------+
	                                          |        3 bytes         |
    	                                      +------------------------+

So message is always "1 byte + 2 bytes + 2 bytes + messageBody.length".

### Command field
Unsigned byte.
This is 1 byte field responsible for storing [command code](https://bitbucket.org/theblynk/blynk-server/src/a3861b0e9bcb9823bbb6dd2722500c55e197bbe6/common/src/main/java/cc/blynk/common/enums/Command.java?at=master) from client, like login, ping, etc...

### Message Id field
Unsigned short.
Is 2 bytes field for defining unique message identifier. Used in order to distinguish on mobile client how to manage responses from hardware. Message ID field should be generated by client side.
Any read protocol command should always have same messageId for same widget. Let's say, you have widget graph1 that configured to read some analog pin.
After you reconfigured graph1 to read another pin, load command will still look the same and messageID will be an ID of widget to draw results on.

### Length field
Unsigned short.
Is 2 bytes field for defining body length. Could be 0 if body is empty or missing.



#### Protocol command codes

		0 - response; After every message client sends to server it retrieves response message back (exception is for LoadProfile, GetToken, Ping commands).
        1 - register; Must have 2 space-separated params as content field (username and pass) : "a@a.ua a"
        2 - login:
            a) For mobile client must have 2 space-separated params as content field (username and pass) : "a@a.ua a"
            b) For hardware client must have 1 param, user token : "6a7a3151cb044cd893a92033dd65f655"
        3 - save profile; Must have 1 param as content string : "{...}"
        4 - load profile; Don't have any params
        5 - getToken; Must have 1 int param, dash board id : "1". ACCEPTED DASH_ID RANGE IS [1..100];
        6 - ping; Sends request from client ot server, than from server to hardware, than back to server and back to client.
        20 - hardware; Command for hardware. Every widget should form it's own body message for hardware command.


#### Hardware command body forming rules
//todo


## Response Codes
For every command client sends to server it will receive response back.
For commands (register, login, saveProfile, hardware) that doesn't request any data back - 'response' (command field 0x00) message will be returned.
For commands (loadProfile, getToken, ping) that request some data back - message will be returned with same command code. In case you sent 'loadProfile' you will receive 'loadProfile' command back with filled body.
[Here class with all codes](https://bitbucket.org/theblynk/blynk-server/src/a3861b0e9bcb9823bbb6dd2722500c55e197bbe6/common/src/main/java/cc/blynk/common/enums/Response.java?at=master)
Response message structure:

	    	       BEFORE DECODE
	+------------------+------------+----------------+
	|       1 byte     |  2 bytes   |     2 bytes    |
	+------------------+------------+----------------+
	| Protocol Command |  MessageId |  Response code |
	|       0x00       |   0x000A   |      0x0001    |
	+------------------+------------+----------------+
	|               always 5 bytes                   |
	+------------------------------------------------+

    200 - message was successfully processed/passed to server
    
    2 - command is bad formed, check syntax and passed params
    3 - user not registered
    4 - user with such name already registered
    5 - user havn't made login command
    6 - user not allowed to perfrom this operation (most probably not logged or socket was closed)
    7 - arduino board not in network
    8 - command not supported
    9 - token not valid
    10 - server error
    11 - user already logged in. Happens in case same user tries to login more than one time.
    500 - server error. something went wrong on server

## User Profile JSON structure
	{ "dashBoards" : 
		[ 
			{
			 "id":1, "name":"My Dashboard", "isActive":true, "timestamp":333333,
			 "widgets"  : [...], 
			 "settings" : {"boardType":"UNO", ..., "someParam":"someValue"}
			}
		]
	}

## Widgets JSON structure

	Button				: {"id":1, "x":1, "y":1, "dashBoardId":1, "label":"Some Text", "type":"BUTTON",         "pinType":"NONE", "pin":13, "value":"1"   } -- sends HIGH on digital pin 13. Possible values 1|0.
	Toggle Button ON	: {"id":1, "x":1, "y":1, "dashBoardId":1, "label":"Some Text", "type":"TOGGLE_BUTTON",  "pinType":"DIGITAL", "pin":18, "value":"1", "state":"ON"} -- sends 1 on digital pin 18
	Toggle Button OFF	: {"id":1, "x":1, "y":1, "dashBoardId":1, "label":"Some Text", "type":"TOGGLE_BUTTON",  "pinType":"VIRTUAL", "pin":18, "value":"0", "state":"OFF"} -- sends 0 on digital pin 18
	Slider				: {"id":1, "x":1, "y":1, "dashBoardId":1, "label":"Some Text", "type":"SLIDER",         "pinType":"ANALOG",  "pin":18, "value":"244" } -- sends 244 on analog pin 18. Possible values -9999 to 9999
	Timer				: {"id":1, "x":1, "y":1, "dashBoardId":1, "label":"Some Text", "type":"TIMER",          "pinType":"DIGITAL", "pin":13, "startTime" : 1111111111, "stopInterval" : 111111111} -- startTime is time in UTC when to start timer (milliseconds are ignored), stopInterval interval in milliseconds after which stop timer.

	//pin reading widgets
	LED					: {"id":1, "x":1, "y":1, "dashBoardId":1, "label":"Some Text", "type":"LED",            "pinType":"DIGITAL", "pin":10} - sends READ pin to server
	Digit Display		: {"id":1, "x":1, "y":1, "dashBoardId":1, "label":"Some Text", "type":"DIGIT4_DISPLAY", "pinType":"DIGITAL", "pin":10} - sends READ pin to server
	Graph				: {"id":1, "x":1, "y":1, "dashBoardId":1, "label":"Some Text", "type":"GRAPH",          "pinType":"DIGITAL", "pin":10, "readingFrequency":1000} - sends READ pin to server. Frequency in microseconds

## Commands order processing
Server guarantees that commands will be processed in same order in which they were send.

## GETTING STARTED

+ Run the server

        java -jar server.jar -port 8080
+ Run the client (simulates smartphone client)

        java -jar client.jar -host localhost -port 8080

+ In this client: register new user and/or login with the same credentials

        register your@email.com yourPassword
        login your@email.com yourPassword

+ Get the token for Arduino

        getToken 1

   	You will get server response similar to this one:

    	00:05:18.086 INFO  - Sending : Message{messageId=30825, command=5, body='1'}
    	00:05:18.100 INFO  - Getting : Message{messageId=30825, command=5, body='33bcbe756b994a6768494d55d1543c74'}
Where `33bcbe756b994a6768494d55d1543c74` is your token.

+ Start another client (simulates Arduino) and use received token to login

    	java -jar client.jar -host localhost -port 8080
    	login 33bcbe756b994a6768494d55d1543c74
   

You can run as many clients as you want.

Clients with same credentials and token are grouped within one chat room/group and can send messages to each other.
All client commands are human-flriendly, so you don't have to remember codes. Examples:

    	digitalWrite 1 1
    	digitalRead 1
    	analogWrite 1 1
    	analogRead 1
    	virtualWrite 1 1
    	virtualRead 1

Registered users are stored locally in TMP dir of your system in file "user.db". So after restart you don't have to register again.


## Local server setup

### Behind wifi router
In case you start Blynk server behind wifi-router and want it to be accessible from internet you have to add port-forwarding rule
on your router. This is required in order all request that come to router were forwarded to Blynk server within local network of your router.
Im my router it look like this: {image here}

## Licensing
[MIT license] (https://bitbucket.org/theblynk/blynk-server/src/c1b06bca3183aba9ea9ed1fad37b856d25cd8a10/license.txt?at=master)