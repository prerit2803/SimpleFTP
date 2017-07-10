# SimpleFTP
Team: Prerit Bhandari (pbhanda2) Palak Agrawal (pagrawa2) 

In this project, we have implemented the Go-back-N automatic repeat request (ARQ) scheme and Selective Repeat ARQ protocol, and carried out a number of experiments to evaluate its performance.

The FTP protocol provides a sophisticated file transfer service, but uses TCP to ensure reliable data transmission.
Here, we have implemented Simple-FTP that provides a simple service: transferring a file from one host to another.
However, Simple-FTP will use UDP to send packets from one host to another, hence, to implement a reliable
data transfer service, we have implemented the Go-back-N ARQ scheme and Selective Repeat ARQ protocol. Using the unreliable UDP protocol allows us to implement
a “transport layer” service such as reliable data transfer in user space.

There is a server which is running on port 7734 and checks for each sequence number for each chunk of data packet received. If the order of sequence number is not as expected, then the sliding window scheme is applied and accordingly, the error is displayed to user and acknowledged to Client.

Clients are allotted port numbers randomly. They divide the data to be sent into chunks based on the Maximum Segment Size and start sending data packets to the Server.

## How to run our code?

1. Create an Eclipse project with same name as our project and change the location to the place where you have extracted our project files.
2. To start the server, run Server.java and Enter Port Number (Ex:7734), Output filename with complete path and Loss Probability value.
3. To start a client, run Client.java ...Provide server's IP address and Port number, Complete file path to be send, Window Size and MSS :)
