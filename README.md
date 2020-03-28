# Description
The same application can work in both auto scalar mode and video processing worker mode.

## How to build
     mvn clean compile assembly:single 
     
## How to run
If you are running in the local machine you must have AWS credentials 
(AWS access keys or AWS profile) setup in the machine. If you are running in a EC2 instance
AWS authentication chain works (AWS access keys, AWS profile or IAM role)
 
    java -jar target/ec2-launcher-1.0-SNAPSHOT-jar-with-dependencies.jar
    
