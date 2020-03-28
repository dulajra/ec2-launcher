# Description
The same application can work in both auto scalar mode and video processing worker mode.

## How to build
     mvn clean compile assembly:single 
     
## How to run
If you are running in the local machine you must have AWS credentials 
(AWS access keys or AWS profile) setup in the machine. If you are running in a EC2 instance
AWS authentication chain works (AWS access keys, AWS profile or IAM role)

### Run in auto scalar mode
    java -jar target/ec2-launcher-1.0-SNAPSHOT-jar-with-dependencies.jar a
    
### Run in worker mode
    java -jar target/ec2-launcher-1.0-SNAPSHOT-jar-with-dependencies.jar w
    
## Deployment guidelines
1. Login to EC2 instance with name AMI
2. In the /home/ubuntu directory you can see worker.jar file which is this.
3. Start the app in worker mode using `java -jar worker.jar a`
4. Now it will start to launch workers when new events are present in SQS.
5. Workers will start processing as soon as they launched automatically 
    
